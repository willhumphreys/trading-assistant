package uk.co.threebugs.mochiwhattotrade3.setupgroup;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsFile;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsMapper;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SetupGroupService {

    private static final Logger logger = LogManager.getLogger(SetupGroupService.class);

    private final SetupGroupMapper setupGroupMapper;
    private final SetupGroupRepository setupGroupRepository;
    private final SetupGroupsRepository setupGroupsRepository;
    private final SetupGroupsMapper setupGroupsMapper;
    private final ObjectMapper objectMapper;

    public List<SetupGroup> loadSetupsFromFile(Path setupsPath) {
        try {
            var setupGroupsFile = objectMapper.readValue(setupsPath.toFile(), SetupGroupsFile.class);
            var scriptsDirectory = Paths.get(setupGroupsFile.getScriptsDirectory());

            var setupGroups = setupGroupsRepository.findByName(setupGroupsFile.getName())
                                                   .orElseGet(() -> setupGroupsRepository.save(setupGroupsMapper.toEntity(setupGroupsFile)));

            return setupGroupsFile.getSetupGroups()
                                  .stream()
                                  .map(setupGroup -> SetupGroupDto.builder()
                                                                  .path(scriptsDirectory.resolve(setupGroup.getPath()))
                                                                  .symbol(setupGroup.getSymbol())
                                                                  .enabled(setupGroup.getEnabled())
                                                                  .build())
                                  .map(setupGroupDto -> {

                                      var optionalSetupGroup = setupGroupRepository.findByPath(setupGroupDto.getPath()
                                                                                                            .toString());

                                      return optionalSetupGroup.orElseGet(() -> {
                                          var setupGroup = setupGroupMapper.toEntity(setupGroupDto, setupGroups);
                                          return setupGroupRepository.save(setupGroup);
                                      });
                                  })
                                  .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error loading setups: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
