package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Value
public class AccountSetupGroupsFileDto {
    String name;
    @JsonProperty("setup-group-name")
    String setupGroupName;
    @JsonProperty("metatrader-account")
    String metatraderAccount;
}
