package com.prgrms.ijuju.global.error;

import com.prgrms.ijuju.domain.minigame.flipcard.dto.response.ErrorFlipCardResponse;
import com.prgrms.ijuju.domain.minigame.flipcard.exception.FlipCardErrorCode;
import com.prgrms.ijuju.domain.minigame.flipcard.exception.FlipCardException;
import com.prgrms.ijuju.domain.stock.mid.dto.response.ErrorMidResponse;
import com.prgrms.ijuju.domain.stock.mid.exception.MidStockErrorCode;
import com.prgrms.ijuju.domain.stock.mid.exception.MidStockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MidStockException.class)
    protected ResponseEntity<ErrorMidResponse> handleMidStockException(final MidStockException e) {
        log.error("MidStockException: {}", e.getMessage());
        final MidStockErrorCode errorCode = e.getErrorCode();
        final ErrorMidResponse response = ErrorMidResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(FlipCardException.class)
    protected ResponseEntity<ErrorFlipCardResponse> handleFlipCardException(final FlipCardException e) {
        log.error("FlipCardException: {}", e.getMessage());
        final FlipCardErrorCode errorCode = e.getErrorCode();
        final ErrorFlipCardResponse response = ErrorFlipCardResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

}
