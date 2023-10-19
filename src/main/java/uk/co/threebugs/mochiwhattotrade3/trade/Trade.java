package uk.co.threebugs.mochiwhattotrade3.trade;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.co.threebugs.mochiwhattotrade3.Type;
import uk.co.threebugs.mochiwhattotrade3.account.Account;
import uk.co.threebugs.mochiwhattotrade3.setup.Setup;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    //@Column(name = "type", columnDefinition = "ENUM('BUY', 'SELL', 'HOLD')")
    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_id")
    private Setup setup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private Account account;

    private Integer metatraderId;

    private ZonedDateTime placedDateTime;

    private BigDecimal placedPrice;

    private ZonedDateTime filledDateTime;

    private BigDecimal filledPrice;

    private ZonedDateTime closedDateTime;

    private BigDecimal closedPrice;

    private String closeType;

    private BigDecimal profit;

    @Column(name = "message_column")
    private String message;


    public String getNewTradeMessage() {
        return "New Trade: %d %s %s in account: %s".formatted(setup.getRank(), setup.getSymbol(), setup.getDirection(), account.getName());
    }
}
