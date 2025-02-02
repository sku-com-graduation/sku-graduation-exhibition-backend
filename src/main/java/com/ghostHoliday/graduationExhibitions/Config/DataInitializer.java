package com.ghostHoliday.graduationExhibitions.config;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Category;
import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
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
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> initializeData();
    }

    @Transactional
    public void initializeData() {
        Team team = new Team();
        team.setName("admin");
        team.setExhibitionYear(2025);
        team.setCategory(Category.WEB);

        team = teamRepository.save(team);

        Account adminAccount = new Account();
        adminAccount.setTeam(team);
        adminAccount.setUserEmail("admin");
        adminAccount.setDefaultPwd(passwordEncoder.encode("admin"));
        adminAccount.setPwd(passwordEncoder.encode("admin"));
        adminAccount.setRole(Role.ADMIN);

        accountRepository.save(adminAccount);
    }
}

