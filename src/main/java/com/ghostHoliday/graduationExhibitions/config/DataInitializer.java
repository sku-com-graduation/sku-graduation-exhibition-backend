package com.ghostHoliday.graduationExhibitions.config;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> initializeData();
    }

    @Transactional
    public void initializeData() {

        if (!accountRepository.existsAccountByUserEmail("computeraee@sunkyul.ac.kr")){
            Account adminAccount = new Account();
            adminAccount.setTeam(null);
            adminAccount.setUserEmail("computeraee@sunkyul.ac.kr");
            adminAccount.setDefaultPwd(passwordEncoder.encode("computeraee@sunkyul.ac.kr"));
            adminAccount.setPwd(passwordEncoder.encode("computeraee@sunkyul.ac.kr"));
            adminAccount.setRole(Role.ADMIN);
            accountRepository.save(adminAccount);
        }
    }
}

