package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.co.threebugs.mochiwhattotrade3.account.AccountRepository;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AccountSetupGroupsService {

    private final AccountSetupGroupsFileRepository accountSetupGroupsFileRepository;

    private final AccountSetupGroupsRepository accountSetupGroupsRepository;
    private final SetupGroupsRepository setupGroupsRepository;
    private final AccountRepository accountRepository;
    private final AccountSetupGroupsMapper accountSetupGroupsMapper;

    public List<AccountSetupGroups> loadAccountSetupGroups(Path path) {

        return accountSetupGroupsFileRepository.load(path)
                                               .stream()
                                               .map(accountSetupGroupName -> {

                                                   var setupGroups = setupGroupsRepository.findByName(accountSetupGroupName.getSetupGroupName())
                                                                                          .orElseThrow(() -> new RuntimeException("Failed to find setup group: " + accountSetupGroupName.getSetupGroupName()));

                                                   var account = accountRepository.findByName(accountSetupGroupName.getMetatraderAccount())
                                                                                  .orElseThrow(() -> new RuntimeException("Failed to find account: " + accountSetupGroupName.getMetatraderAccount()));

                                                   var accountSetupGroups = new AccountSetupGroups();

                                                   accountSetupGroups.setName(accountSetupGroupName.getName());
                                                   accountSetupGroups.setSetupGroups(setupGroups);
                                                   accountSetupGroups.setAccount(account);

                                                   return accountSetupGroupsRepository.findByName(accountSetupGroupName.getName())
                                                                                      .map(existingAccountSetupGroups -> {
                                                                                          existingAccountSetupGroups.setSetupGroups(setupGroups);
                                                                                          existingAccountSetupGroups.setAccount(account);
                                                                                          return accountSetupGroupsRepository.save(existingAccountSetupGroups);
                                                                                      })
                                                                                      .orElseGet(() -> accountSetupGroupsRepository.save(accountSetupGroups));

                                               })
                                               .toList();
    }

    public Optional<AccountSetupGroupsDto> findByName(String accountSetupGroupsName) {
        var optionalAccountSetupGroups = this.accountSetupGroupsRepository.findByName(accountSetupGroupsName);

        return optionalAccountSetupGroups.map(accountSetupGroupsMapper::toDto);
    }

    public List<AccountSetupGroupsDto> findAll() {
        return accountSetupGroupsRepository.findAll()
                                           .stream()
                                           .map(accountSetupGroupsMapper::toDto)
                                           .toList();
    }
}
