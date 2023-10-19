package uk.co.threebugs.mochiwhattotrade3.account;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public Optional<AccountDto> findByName(String name) {
        return accountRepository.findByName(name)
                                .map(accountMapper::toDto);
    }

    public AccountDto save(AccountDto accountDto) {
        return accountMapper.toDto(accountRepository.save(accountMapper.toEntity(accountDto)));
    }

    public List<AccountDto> findAll() {
        return accountRepository.findAll()
                                .stream()
                                .map(accountMapper::toDto)
                                .toList();
    }
}
