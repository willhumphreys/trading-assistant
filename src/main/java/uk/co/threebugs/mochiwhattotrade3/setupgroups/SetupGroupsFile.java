package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
@Value
@Builder
public class SetupGroupsFile {
    String name;
    String scriptsDirectory;
    List<SetupGroupFile> setupGroups;
}
