package uk.co.threebugs.mochiwhattotrade3.metatrader;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Jacksonized
@Builder
public class TradeInfo {
        int magic;
        BigDecimal lots;
        String symbol;
        BigDecimal swap;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
        @JsonProperty("open_time")
        LocalDateTime openTime;

        @JsonProperty("SL")
        BigDecimal stopLoss;

        String comment;
        String type;

        @JsonProperty("open_price")
        BigDecimal openPrice;

        @JsonProperty("TP")
        BigDecimal takeProfit;

        @JsonProperty("pnl")
        BigDecimal profitAndLoss;

        Object mapType;

        Boolean empty;
}
