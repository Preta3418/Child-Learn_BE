package com.prgrms.ijuju.domain.article.scheduler;

import com.prgrms.ijuju.domain.article.contant.DataType;
import com.prgrms.ijuju.domain.article.data.Trend;
import com.prgrms.ijuju.domain.article.entity.Article;
import com.prgrms.ijuju.domain.article.repository.ArticleRepository;
import com.prgrms.ijuju.domain.article.service.ArticleGenerationService;
import com.prgrms.ijuju.domain.article.service.StockTrendService;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleScheduler {

    private static final int MAX_ARTICLES = 5;
    private final ReentrantLock schedulerLock = new ReentrantLock();

    private final ArticleGenerationService articleGenerationService;
    private final ArticleRepository articleRepository;
    private final StockTrendService stockTrendService;
    private final MidStockRepository midStockRepository;

    @Scheduled(cron = "0 30 9 * * ?")
    @Transactional
    public void manageArticles() {
        if (!schedulerLock.tryLock()) {
            log.warn("Article management already running, skipping this execution");
            return;
        }

        try {
            log.info("Starting article management");
            decrementDurationAndDeleteExpiredArticles();
            manageArticlesByType(DataType.ADVANCED);
            manageArticlesByType(DataType.MID);
            log.info("Article management completed");
        } catch (Exception e) {
            log.error("Error during article management", e);
        } finally {
            schedulerLock.unlock();
        }
    }

    private void decrementDurationAndDeleteExpiredArticles() {
        articleRepository.decrementAllDurations();
        articleRepository.deleteByDuration(0);
    }


    private void manageArticlesByType(DataType type) {
        try {
            long currentCount = articleRepository.countByType(type);
            log.debug("Current article count for {}: {}", type, currentCount);

            if (currentCount >= MAX_ARTICLES) {
                log.debug("Article limit reached for {}, skipping generation", type);
                return;
            }

            List<Trend> trends = fetchTrendsByType(type);
            if (trends.isEmpty()) {
                log.warn("No trends found for type: {}", type);
                return;
            }

            log.info("Generating articles for {} trends of type {}", trends.size(), type);
            articleGenerationService.generateArticles(trends, type);
            removeExcessArticles(type);
        } catch (Exception e) {
            log.error("Failed to manage articles for type: {}", type, e);
        }
    }

    private List<Trend> fetchTrendsByType(DataType type) {
        try {
            if (type == DataType.ADVANCED) {
                return stockTrendService.analyzeTrendsForAdvStock();
            } else if (type == DataType.MID) {
                List<Long> midStockIds = midStockRepository.findAllIds();
                if (midStockIds.isEmpty()) {
                    log.warn("No mid stock IDs found");
                    return Collections.emptyList();
                }

                return midStockIds.stream()
                        .flatMap(id -> stockTrendService.analyzeTrendsForMidStock(id).stream())
                        .collect(Collectors.toList());
            } else {
                throw new IllegalArgumentException("Unknown DataType: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to fetch trends for type: {}", type, e);
            return Collections.emptyList();
        }
    }

    private void removeExcessArticles(DataType type) {
        List<Article> articles = articleRepository.findByType(type);

        if (articles.size() > MAX_ARTICLES) {
            Collections.shuffle(articles);
            List<Article> articlesToRemove = articles.subList(MAX_ARTICLES, articles.size());
            articleRepository.deleteAll(articlesToRemove);
        }
    }


}