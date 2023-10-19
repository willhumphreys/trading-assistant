package uk.co.threebugs.mochiwhattotrade3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.co.threebugs.mochiwhattotrade3.setup.SetupDto;
import uk.co.threebugs.mochiwhattotrade3.trade.TradeDto;
import uk.co.threebugs.mochiwhattotrade3.trade.TradeService;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SetupServiceIntegrationTestLocation {

    @Autowired
    private TradeService tradeService;

    @Test
    public void testCreateRecord() {

        SetupDto setupDto = SetupDto.builder()
                                    .rank(1)
                                    .dayOfWeek(1)
                                    .hourOfDay(1)
                                    .stop(1)
                                    .limit(1)
                                    .tickOffset(1)
                                    .tradeDuration(1)
                                    .outOfTime(1)
                                    .build();

        var recordDto = TradeDto.builder()
                                .setup(setupDto)
                                .placedDateTime(ZonedDateTime.now())
                                .placedPrice(new BigDecimal("1.00"))
                                .filledDateTime(ZonedDateTime.now())
                                .filledPrice(new BigDecimal("1.00"))
                                .closedDateTime(ZonedDateTime.now())
                                .closedPrice(new BigDecimal("1.00"))
                                .closeType("Type")
                                .message("Message")
                                .build();

        var createdRecord = tradeService.save(recordDto);

        assertThat(createdRecord).isNotNull();

        assertThat(createdRecord).isNotNull();
        assertThat(createdRecord.getId()).isEqualTo(1);
        SetupDto setup = createdRecord.getSetup();
        assertThat(setup.getRank()).isEqualTo(1);
        assertThat(setup.getDayOfWeek()).isEqualTo(1);
        assertThat(setup.getHourOfDay()).isEqualTo(1);
        assertThat(setup.getStop()).isEqualTo(1);
        assertThat(setup.getLimit()).isEqualTo(1);
        assertThat(setup.getTickOffset()).isEqualTo(1);
        assertThat(setup.getTradeDuration()).isEqualTo(1);
        assertThat(setup.getOutOfTime()).isEqualTo(1);
        assertThat(createdRecord.getPlacedDateTime()).isNotNull();
        assertThat(createdRecord.getPlacedPrice()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(createdRecord.getFilledDateTime()).isNotNull();
        assertThat(createdRecord.getFilledPrice()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(createdRecord.getClosedDateTime()).isNotNull();
        assertThat(createdRecord.getClosedPrice()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(createdRecord.getCloseType()).isEqualTo("Type");
        assertThat(createdRecord.getMessage()).isEqualTo("Message");

        var byId = tradeService.findById(createdRecord.getId());

        assertThat(byId).isPresent();

        assertThat(byId.get()).isNotNull();

        assertThat(byId.get()
                       .getId()).isEqualTo(1);
        assertThat(byId.get()
                       .getSetup()
                       .getRank()).isEqualTo(1);

        tradeService.deleteById(createdRecord.getId());

    }


}
