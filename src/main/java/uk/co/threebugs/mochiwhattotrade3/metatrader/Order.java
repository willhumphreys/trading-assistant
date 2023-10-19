package uk.co.threebugs.mochiwhattotrade3.metatrader;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Order {
    String symbol;
    String orderType;
    double lots;
    BigDecimal price;
    BigDecimal stopLoss;
    BigDecimal takeProfit;
    int magic;
    String comment;
    long expiration;
}