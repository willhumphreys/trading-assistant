package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Builder
@Value
public class SetupGroupsDto {

    Integer id;
    String name;
    Path scriptsDirectory;

}
