package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
@AllArgsConstructor
public class AccountSetupGroupsFileRepository {

    private final ObjectMapper objectMapper;

    public List<AccountSetupGroupsFileDto> load(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });

        } catch (Exception e) {
            throw new RuntimeException("Failed to load setup groups from file: " + path, e);
        }
    }
}

