package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SetupGroupsController {

    private final SetupGroupsService setupGroupsService;

    @GetMapping("/setupGroups")
    public List<SetupGroupsDto> findAll() {
        return setupGroupsService.findAll();
    }

}
