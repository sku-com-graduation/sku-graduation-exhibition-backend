package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.FindAccountByYearDTO;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final PostRepository postRepository;
    private final JwtUtility jwtUtility;

    @Transactional
    public String login(String userEmail, String password) {
        Account account = accountRepository.findAccountByuserEmailAndPwd(userEmail, password)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 잘못되었습니다."));
        return jwtUtility.generateToken(userEmail);
    }

    @Transactional
    public void registAccount(MultipartFile file) throws IOException, CsvException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), "EUC-KR"));
        List<String []> rows = csvReader.readAll();
        ArrayList<Team> teams = new ArrayList<>();
        ArrayList<Account> accounts = new ArrayList<>();
        int year = LocalDateTime.now().getYear();
        for (String[] row : rows) {
            String teamName = row[0];
            Category category = Category.valueOf(row[1]);
            String professorName = row[2];
            String studentNumber = row[3];
            String userEmail = row[4];

            // 해당 팀의 post 생성
            Post post = createNewPost();
            postRepository.save(post);

            // 팀 생성
            Team team = createTeam(teamName, category, year, post);
            teams.add(team);

            // 계정 생성
            Account account = createAccount(userEmail, studentNumber, team);
            accounts.add(account);

        }
        
        accountRepository.saveAll(accounts);
        teamRepository.saveAll(teams);
    }
    @Transactional
    public ArrayList<FindAccountByYearDTO> findAllAccountByYear(int year) {
        List<Account> accounts = accountRepository.findAll();
        ArrayList<FindAccountByYearDTO> findAccountByYearDTOS = new ArrayList<>();
        for (Account account : accounts) {
            FindAccountByYearDTO findAccountByYearDTO = new FindAccountByYearDTO();
            Team team = account.getTeam();
            if (year == team.getExhibitionYear()){
                findAccountByYearDTO.setTeamName(team.getName());
                findAccountByYearDTO.setUserEmail(account.getUserEmail());
                findAccountByYearDTO.setRecent(account.getRecent());
                findAccountByYearDTOS.add(findAccountByYearDTO);
            }
        }
        return findAccountByYearDTOS;

    }

    public Account tokenToAccount(String token) {
        String userEmail = jwtUtility.validateToken(token).getSubject();
        return accountRepository.findAccountByUserEmail(userEmail).get();

    }

    @Transactional
    public void deleteAccount(Long accountId){
        accountRepository.deleteById(accountId);
    }

    @Transactional
    public void resetAccount(ArrayList<Long> AccountsId){
         accountRepository.resetPasswordsToDefault(AccountsId);
    }




    public static Post createNewPost(){
        Post post = new Post();
        post.setTitle(null);
        post.setContent(null);
        post.setSlideUrl(null);
        post.setPosterUrl(null);
        post.setDemoUrl(null);
        post.setTeamProfileUrl(null);

        return post;
    }

    public static Team createTeam(String teamName, Category category, int year, Post post){
        Team team = new Team();
        team.setName(teamName);
        team.setCategory(category);
        team.setProfessor(null); // 추 후 교수 정보로 변경
        team.setExhibitionYear(year);
        team.setPost(post);
        return team;
    }

    public static Account createAccount(String userEmail, String studentNumber, Team team){
        Account account = new Account();
        account.setUserEmail(userEmail);
        account.setDefaultPwd(studentNumber);
        account.setPwd(studentNumber);
        account.setRecent(null);
        account.setRole(Role.USER);
        account.setTeam(team);
        return account;
    }
}

