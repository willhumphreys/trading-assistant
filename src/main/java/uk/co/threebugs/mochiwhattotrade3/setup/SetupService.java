package uk.co.threebugs.mochiwhattotrade3.setup;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final SetupRepository setupRepository;
    private final SetupMapper setupMapper;


}
