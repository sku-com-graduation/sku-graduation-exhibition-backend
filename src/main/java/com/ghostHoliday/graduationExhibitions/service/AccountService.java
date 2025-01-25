package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Long save(Account account ) {
        return accountRepository.save(account).getId();
    }

}

