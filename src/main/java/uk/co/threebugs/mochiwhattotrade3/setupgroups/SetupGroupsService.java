package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SetupGroupsService {

    private final SetupGroupsRepository setupGroupsRepository;
    private final SetupGroupsMapper setupGroupsMapper;

    public List<SetupGroupsDto> findAll() {
        return setupGroupsRepository.findAll()
                                    .stream()
                                    .map(setupGroupsMapper::toDto)
                                    .toList();
    }

}
