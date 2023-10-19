package uk.co.threebugs.mochiwhattotrade3.trade;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.co.threebugs.mochiwhattotrade3.account.AccountDto;
import uk.co.threebugs.mochiwhattotrade3.setup.SetupDto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Builder
@Value
@Jacksonized
public class TradeDto {
    Integer id;
    String type;
    SetupDto setup;
    AccountDto account;
    Integer metatraderId;
    ZonedDateTime placedDateTime;
    BigDecimal placedPrice;
    ZonedDateTime filledDateTime;
    BigDecimal filledPrice;
    ZonedDateTime closedDateTime;
    BigDecimal closedPrice;
    BigDecimal profit;
    String closeType;
    String message;
}
