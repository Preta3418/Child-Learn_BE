package com.prgrms.ijuju.domain.stock.adv.advancedinvest.service;

import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.gameexception.GameAlreadyPlayedException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.gameexception.GameAlreadyStartedException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.gameexception.GameNotFoundException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.gameexception.InvalidGameTimeException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.otherexception.MemberNotFoundException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.stockexception.DataNotFoundException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.stockexception.InvalidQuantityException;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.exception.stockexception.StockNotFoundException;
import com.prgrms.ijuju.global.util.WebSocketUtil;
import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.domain.member.repository.MemberRepository;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.dto.request.StockTransactionRequestDto;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.dto.response.AdvStockResponseDto;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.entity.AdvancedInvest;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.repository.AdvancedInvestRepository;
import com.prgrms.ijuju.domain.stock.adv.advstock.constant.DataType;
import com.prgrms.ijuju.domain.stock.adv.advstock.entity.AdvStock;
import com.prgrms.ijuju.domain.stock.adv.advstock.repository.AdvStockRepository;
import com.prgrms.ijuju.domain.stock.adv.stockrecord.constant.TradeType;
import com.prgrms.ijuju.domain.stock.adv.stockrecord.dto.request.StockRecordRequestDto;
import com.prgrms.ijuju.domain.stock.adv.stockrecord.service.StockRecordService;
import com.prgrms.ijuju.domain.wallet.service.WalletService;
import com.prgrms.ijuju.domain.wallet.dto.request.StockPointRequestDTO;
import com.prgrms.ijuju.domain.wallet.entity.StockType;
import com.prgrms.ijuju.domain.wallet.entity.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;


@Service
@RequiredArgsConstructor
public class AdvancedInvestServiceImpl implements AdvancedInvestService {

    private final AdvancedInvestRepository advancedInvestRepository;
    private final AdvStockRepository advStockRepository;
    private final MemberRepository memberRepository;
    private final StockRecordService stockRecordService;
    private final WalletService walletService;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);




    
    private final Map<Long, GameState> gameStates = new ConcurrentHashMap<>();
    private final Map<Long, Long> memberIdToGameId = new ConcurrentHashMap<>();


    private static class GameState {
        private final ScheduledFuture<?> timer;
        private final WebSocketSession session;
        private volatile int currentSecond;
        private volatile int liveSentCounter;

        public GameState(ScheduledFuture<?> timer, WebSocketSession session) {
            this.timer = timer;
            this.session = session;
            this.currentSecond = 0;
            this.liveSentCounter = 0;
        }

        public synchronized void updateSecond(int second) {
            this.currentSecond = second;
        }

        public synchronized void incrementLiveCounter() {
            this.liveSentCounter++;
        }

        
        public ScheduledFuture<?> getTimer() { return timer; }
        public WebSocketSession getSession() { return session; }
        public synchronized int getCurrentSecond() { return currentSecond; }
        public synchronized int getLiveSentCounter() { return liveSentCounter; }
    }


    //게임 타이머. 게임은 총 7분 진행되며, 1분은 장전 거래 시간, 5분은 거래 시간, 마지막 1분은 장후 거래 시간
    //SechduledExecutorService 를 사용하였습니다.   >>>  https://lslagi.tistory.com/entry/JAVA-ScheduledExecutorService-Timer-%EC%A0%81%EC%9A%A9%EC%9E%90%EB%8F%99-%EB%A6%AC%ED%94%8C%EB%A0%88%EC%89%AC
    @Override
    public void startGameTimer(WebSocketSession session, Long gameId, int startSecond) {


        Runnable gameTask = new Runnable() {
            int second = startSecond;

            public void run() {
                try {
                    GameState gameState = gameStates.get(gameId);
                    if (gameState == null) {
                        return; // Game state not found, stop execution
                    }

                    gameState.updateSecond(second);

                    if (second == 0) { // 장전 거래 시간 1분 > ReferenceData
                        sendReferenceData(session);

                    } else if (second >= 60 && second <= 310 && (second - 60) % 50 == 0) { // 거래 시간 5분 > LiveData >> 총 6개의 데이터가 전송
                        int livePhase = (second - 60) / 50;
                        if (livePhase < 6 && gameState.getLiveSentCounter() < 6) { // 최대 6개 데이터만 전송
                            sendLiveData(session, livePhase);
                            gameState.incrementLiveCounter();
                        }

                    } else if (second >= 420) {
                        endGame(gameId); // 게임 종료
                        sendEndSignal(session);
                        return; // Stop execution after game end
                    }

                    second++; // 다음 초로 진행

                } catch (DataNotFoundException | StockNotFoundException e) {
                    // 데이터 관련 예외는 게임을 종료
                    endGame(gameId);
                } catch (Exception e) {
                    // 기타 예외는 로깅 후 게임 일시정지
                    e.printStackTrace();
                    pauseGame(gameId);
                }
            }
        };

        //activeTimer 는 카운터 작업을 관리하는 객체
        ScheduledFuture<?> activeTimer = executorService.scheduleAtFixedRate(gameTask, 0, 1, TimeUnit.SECONDS);
        GameState gameState = new GameState(activeTimer, session);
        gameState.updateSecond(startSecond);
        gameStates.put(gameId, gameState);
    }

    // Reference Data
    //LazyInitializationException 이 생겨서 문제 해결하려고 발악했었음.
    @Transactional
    public void sendReferenceData(WebSocketSession session) {
        List<AdvStock> referenceData = advStockRepository.findByDataType(DataType.REFERENCE);

        if (referenceData.isEmpty()) {
            throw new DataNotFoundException();
        }

        List<AdvStockResponseDto> responseDto = referenceData.stream()
                .flatMap(stock -> AdvStockResponseDto.fromEntityForReference(stock).stream())
                .toList();

        WebSocketUtil.send(session, responseDto);
    }

    // Live Data
    @Transactional
    public void sendLiveData(WebSocketSession session, int livePhase) {
        List<AdvStock> liveData = advStockRepository.findByDataType(DataType.LIVE);

        if (liveData.isEmpty() || livePhase >= liveData.size()) {
            throw new DataNotFoundException();
        }

        List<AdvStockResponseDto> responseDto = liveData.stream()
                .map(stock -> AdvStockResponseDto.fromEntity(stock, livePhase))  // 특정 시간 데이터를 전송
                .toList();
        WebSocketUtil.send(session, responseDto);
    }

    private void sendEndSignal(WebSocketSession session) {
        WebSocketUtil.send(session, "게임 종료");
    }

    private void safeCloseWebSocketSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                // 로그만 남기고 예외는 무시 (게임 종료 과정에서 실패해도 계속 진행)
                e.printStackTrace();
            }
        }
    }

    // Volumes 조회
    @Transactional
    @Override
    public void getRecentVolumes(WebSocketSession session, String stockSymbol, Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new GameNotFoundException();
        }

        int liveSentCounterValue = gameState.getLiveSentCounter(); // LiveData 전송 횟수

        // 2. ReferenceData 가져오기
        List<Long> referenceVolumes = advStockRepository.findBySymbolAndDataType(stockSymbol, DataType.REFERENCE)
                .map(AdvStock::getVolumes)
                .orElseThrow(() -> new IllegalArgumentException("Reference Data를 찾을 수 없습니다."));

        // 3. LiveData 가져오기
        List<Long> liveVolumes = advStockRepository.findBySymbolAndDataType(stockSymbol, DataType.LIVE)
                .map(AdvStock::getVolumes)
                .orElseThrow(() -> new IllegalArgumentException("Live Data를 찾을 수 없습니다."));

        // 4. ReferenceData와 LiveData 조합
        int referenceCount = Math.max(0, 8 - liveSentCounterValue); // ReferenceData에서 가져올 개수
        int liveCount = Math.min(liveSentCounterValue, 8);         // LiveData에서 가져올 개수

        List<Long> combinedVolumes = new ArrayList<>();

        // ReferenceData에서 최신 데이터 추가
        if (referenceCount > 0) {
            List<Long> latestReferenceVolumes = referenceVolumes.subList(
                    Math.max(referenceVolumes.size() - referenceCount, 0), referenceVolumes.size()
            );
            combinedVolumes.addAll(latestReferenceVolumes);
        }

        // LiveData에서 가장 오래된 데이터 추가
        if (liveCount > 0) {
            List<Long> earliestLiveVolumes = liveVolumes.subList(0, Math.min(liveCount, liveVolumes.size()));
            combinedVolumes.addAll(earliestLiveVolumes);
        }

        // 5. WebSocket으로 전송
        WebSocketUtil.send(session, Map.of("volumes", combinedVolumes));
    }


    //게임 실행 메소드로, 새벽 6시부터 8시 사이에는 게임이 제한되며, 오늘 하루동안 게임을 이미 했다면 또한 제한된다
    @Transactional
    @Override
    public void startGame(WebSocketSession session, Long memberId) {
        LocalTime now = LocalTime.now();
        LocalTime startRestrictedTime = LocalTime.of(6, 0); // 오전 6시
        LocalTime endRestrictedTime = LocalTime.of(8, 0);   // 오전 8시

        if (now.isAfter(startRestrictedTime) && now.isBefore(endRestrictedTime)) {
            throw new InvalidGameTimeException();
        }

        if (memberIdToGameId.containsKey(memberId)) {
            throw new GameAlreadyStartedException();
        }


        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 오늘 이미 게임을 시작했는지 확인
        Optional<AdvancedInvest> existingInvest = advancedInvestRepository.findByMemberIdAndPlayedTodayTrue(memberId);
        if (existingInvest.isPresent()) {
            throw new GameAlreadyPlayedException();
        }

        AdvancedInvest advancedInvest = advancedInvestRepository.save(
                AdvancedInvest.builder()
                        .member(member)
                        .startTime(System.currentTimeMillis())
                        .paused(false)
                        .build()
        );

        memberIdToGameId.put(memberId, advancedInvest.getId());

        // 게임 타이머 시작
        startGameTimer(session, advancedInvest.getId(), 0);
    }


    // 게임 일시정지
    @Override
    @Transactional
    public void pauseGame(Long gameId) {
        GameState gameState = gameStates.remove(gameId);
        if (gameState == null) {
            throw new GameNotFoundException();
        }

        ScheduledFuture<?> activeTimer = gameState.getTimer();
        if (activeTimer != null) {
            activeTimer.cancel(false);
        }

        AdvancedInvest advancedInvest = advancedInvestRepository.findById(gameId)
                .orElseThrow(GameNotFoundException::new);

        advancedInvest.setPaused(true); // 게임은 일시정지 상태로 표시
        advancedInvest.setCurrentSecond(gameState.getCurrentSecond());
        advancedInvestRepository.save(advancedInvest);
    }

    // 게임 재개
    @Override
    @Transactional
    public void resumeGame(WebSocketSession session, Long gameId) {
        AdvancedInvest advancedInvest = advancedInvestRepository.findById(gameId)
                .orElseThrow(GameNotFoundException::new);
        if (!advancedInvest.isPaused()) {
            throw new GameAlreadyPlayedException();
        }

        int currentSecond = advancedInvest.getCurrentSecond(); // 저장된 초 가져오기
        advancedInvest.setPaused(false); // 게임 상태를 진행 중으로 변경
        advancedInvestRepository.save(advancedInvest);

        startGameTimer(session, gameId, currentSecond); // 타이머 재개
    }

    // 게임 종료
    @Override
    @Transactional
    public void endGame(Long gameId) {
        AdvancedInvest advancedInvest = advancedInvestRepository.findById(gameId)
                .orElseThrow(GameNotFoundException::new);

        GameState gameState = gameStates.remove(gameId);
        memberIdToGameId.values().remove(gameId);

        if (gameState != null) {
            ScheduledFuture<?> timer = gameState.getTimer();
            if (timer != null) {
                timer.cancel(false);
            }

            //웹소켓 종료
            WebSocketSession session = gameState.getSession();
            safeCloseWebSocketSession(session);
        }

        advancedInvest.setPlayedToday(true);
        advancedInvestRepository.save(advancedInvest);
    }

    // 남은 시간 조회 메소드
    @Override
    public int getRemainingTime(Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new GameNotFoundException();
        }
        return Math.max(0, 420 - gameState.getCurrentSecond()); // 전체 시간에서 현재 초수 뺀 값 반환
    }


    //아침 7시 리셋
    @Override
    @Scheduled(cron = "0 0 7 * * ?") //
    @Transactional
    public void resetPlayedTodayStatus() {

        //7시에 모든 유저 PlayedToday = false;
        advancedInvestRepository.resetPlayedToday();

        // 진행 중인 게임 강제 종료
        List<Long> gameIds = new ArrayList<>(gameStates.keySet());
        for (Long gameId : gameIds) {
            endGame(gameId);
        }

        // 정지 상태인 게임 강제 종료
        List<AdvancedInvest> pausedGames = advancedInvestRepository.findAllByPausedTrue();
        for (AdvancedInvest game : pausedGames) {
            endGame(game.getId());
        }
    }


    @Transactional
    @Override
    public void buyStock(Long gameId, StockTransactionRequestDto request) {
        AdvancedInvest advancedInvest = advancedInvestRepository.findById(gameId)
                .orElseThrow(GameNotFoundException::new);

        List<Double> closePrices = advStockRepository.findBySymbol(request.getStockSymbol())
                .map(AdvStock::getClosePrices)
                .orElseThrow(StockNotFoundException::new);

        if (closePrices.isEmpty()) {
            throw new DataNotFoundException();
        }

        Double latestClosePrice = closePrices.get(closePrices.size() - 1); // 가장 최신 종가

        if (request.getQuantity() <= 0) {
            throw new InvalidQuantityException();
        }

        // 주식 구매에 필요한 포인트 계산
        BigDecimal pointsRequired = BigDecimal.valueOf(latestClosePrice)
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        // 포인트 차감 및 거래 기록
        StockPointRequestDTO stockPointRequest = StockPointRequestDTO.builder()
                .memberId(request.getMemberId())
                .points(pointsRequired.longValue())
                .stockType(StockType.ADVANCED)
                .transactionType(TransactionType.USED)
                .stockName(request.getStockSymbol())
                .build();
        walletService.simulateStockInvestment(stockPointRequest);
        

        StockRecordRequestDto recordRequest = StockRecordRequestDto.builder()
                .memberId(request.getMemberId())
                .stockSymbol(request.getStockSymbol())
                .tradeType(TradeType.BUY)
                .quantity(request.getQuantity())
                .price(pointsRequired)
                .advId(gameId)
                .build();

        // 거래 내역 저장
        stockRecordService.saveRecord(recordRequest, advancedInvest.getMember());
    }

    @Transactional
    @Override
    public void sellStock(Long gameId, StockTransactionRequestDto request) {
        AdvancedInvest advancedInvest = advancedInvestRepository.findById(gameId)
                .orElseThrow(GameNotFoundException::new);

        List<Double> closePrices = advStockRepository.findBySymbol(request.getStockSymbol())
                .map(AdvStock::getClosePrices)
                .orElseThrow(StockNotFoundException::new);

        if (closePrices.isEmpty()) {
            throw new DataNotFoundException();
        }

        Double latestClosePrice = closePrices.get(closePrices.size() - 1); // 가장 최신 종가

        if (request.getQuantity() <= 0) {
            throw new InvalidQuantityException();
        }

        // 보유 주식 수량 확인
        double ownedQuantity = stockRecordService.calculateOwnedStock(gameId, request.getStockSymbol());
        if (ownedQuantity < request.getQuantity()) {
            throw new IllegalArgumentException("보유 수량보다 많은 주식을 판매할 수 없습니다.");
        }

        // 판매로 얻는 포인트 계산
        BigDecimal pointsEarned = BigDecimal.valueOf(latestClosePrice)
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // 포인트 환급 및 거래 기록
        StockPointRequestDTO stockPointRequest = StockPointRequestDTO.builder()
                .memberId(request.getMemberId())        
                .points(pointsEarned.longValue())
                .stockType(StockType.ADVANCED)
                .transactionType(TransactionType.EARNED)
                .stockName(request.getStockSymbol())
                .build();

        walletService.simulateStockInvestment(stockPointRequest);

        StockRecordRequestDto recordRequest = StockRecordRequestDto.builder()
                .memberId(request.getMemberId())
                .stockSymbol(request.getStockSymbol())
                .tradeType(TradeType.SELL)
                .quantity(request.getQuantity())
                .price(pointsEarned)
                .advId(gameId)
                .build();

        // 거래 내역 저장
        stockRecordService.saveRecord(recordRequest, advancedInvest.getMember());
    }
}
