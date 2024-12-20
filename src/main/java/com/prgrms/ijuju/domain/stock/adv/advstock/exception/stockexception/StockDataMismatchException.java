package com.prgrms.ijuju.domain.stock.adv.advstock.exception.stockexception;

import com.prgrms.ijuju.domain.stock.adv.advstock.exception.AdvStockErrorCode;
import com.prgrms.ijuju.global.exception.BusinessException;

public class StockDataMismatchException extends BusinessException {
    public StockDataMismatchException() {
        super(AdvStockErrorCode.STOCK_DATA_MISMATCH);
    }
}