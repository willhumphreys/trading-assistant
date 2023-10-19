package uk.co.threebugs.mochiwhattotrade3.account;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Builder
@Value
public class AccountDto {
    Integer id;
    String name;
    Path metatraderAdvisorPath;
}
