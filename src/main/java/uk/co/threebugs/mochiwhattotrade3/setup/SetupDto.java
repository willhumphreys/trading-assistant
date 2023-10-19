package uk.co.threebugs.mochiwhattotrade3.setup;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroupDto;

@Builder
@Value
@Jacksonized
public class SetupDto {
    Integer id;
    SetupGroupDto setupGroup;
    String symbol;
    Integer rank;
    Integer dayOfWeek;
    Integer hourOfDay;
    Integer stop;
    Integer limit;
    Integer tickOffset;
    Integer tradeDuration;
    Integer outOfTime;
}
