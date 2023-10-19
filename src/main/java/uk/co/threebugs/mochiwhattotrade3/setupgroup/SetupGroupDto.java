package uk.co.threebugs.mochiwhattotrade3.setupgroup;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsDto;

import java.nio.file.Path;


@Builder
@Value
@Jacksonized
public class SetupGroupDto {

    Integer id;
    SetupGroupsDto setupGroups;
    Path path;
    String symbol;
    Boolean enabled;
}

