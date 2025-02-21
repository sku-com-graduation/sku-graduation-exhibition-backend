package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.FindAccountByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.LoginDTO;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final PostRepository postRepository;
    private final JwtUtility jwtUtility;
    private final StudentRepository studentRepository;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginDTO login(String userEmail, String password) {
        System.out.println(userEmail);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("아이디가 잘못되었습니다."));
        if (!passwordEncoder.matches(password,account.getPwd())){
            throw new RuntimeException("비밀번호가 잘못되었습니다.");
        }
        account.setRecent(LocalDateTime.now());
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccessToken(jwtUtility.generateToken(userEmail,account.getRole()));
        loginDTO.setRole(account.getRole());
        if (account.getRole().equals(Role.ADMIN))
            loginDTO.setTeamName("ADMIN");
        else
            loginDTO.setTeamName(account.getTeam().getName());
        return loginDTO;
    }

    @Transactional
    public void registAccount(MultipartFile file) throws IOException, CsvException {

        CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        List<String []> rows = csvReader.readAll();
        ArrayList<Account> accounts = new ArrayList<>();
        int year = LocalDateTime.now().getYear();
        for (String[] row : rows) {
            String teamName = row[0];
            Category category = Category.valueOf(row[1]);
            String professorName = row[2];

            // 해당 팀의 post 생성
            Post post = createNewPost();
            //포스트 저장폴더 생성
            String uploadDir = "teamPost" + "\\" + post.getUuid();
            File directory = new File(uploadDir);
            if(!directory.exists()){
                directory.mkdirs();
            }

            postRepository.save(post);
            // 팀 생성
            Team team = createTeam(teamName, category, year, post);
            teamRepository.save(team);
            int total = (row.length / 3);
            for(int i = 1; i < total; i++) {
                String name = row[3*i];
                String number = row[3*i+1];
                String email = row[3*i+2];
                Student student = studentRepository.findByStudentNumber(number);
                if (i == 1){
                    // 팀장이라면 계정 생성
                    student.setRole(Role.LEADER);
                    Account account = createAccount(email, passwordEncoder.encode(number), team);
                    accounts.add(account);
                }
                else {
                    student.setRole(Role.MEMBER);
                }
                student.setTeam(team);
                student.getStudentProfile().setStudentEmail(email);
            }
        }
        accountRepository.saveAll(accounts);

    }

    @Transactional
    public ArrayList<FindAccountByYearDTO> findAllAccountByYear(int year) throws Exception {
        List<Account> accounts = accountRepository.findNonAdminAccountsByExhibitionYear(year);
        ArrayList<FindAccountByYearDTO> findAccountByYearDTOS = new ArrayList<>();

        for (Account account : accounts) {
            String encryptedAccountId = encryptionService.encryptPrimaryKey(account.getId());
            FindAccountByYearDTO findAccountByYearDTO = new FindAccountByYearDTO();
            Team team = account.getTeam();

            if (year == team.getExhibitionYear()){
                findAccountByYearDTO.setEncryptedAccountId(encryptedAccountId);
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
    public void deleteAccount(ArrayList<Long> accountIds){
        accountRepository.deleteAllById(accountIds);
    }

    @Transactional
    public void resetAccount(ArrayList<Long> AccountsId){
         accountRepository.resetPasswordsToDefault(AccountsId);
    }




    public static Post createNewPost(){
        Post post = new Post();
        post.setTitle(null);
        post.setContent(null);
        // 슬라이드 저장 폴더 생성
        String uploadSlideDir = "teamPost" + "\\" + post.getUuid() + "\\" + "slideImage";
        File slideDir = new File(uploadSlideDir);
        if(!slideDir.exists()){
            slideDir.mkdirs();
        }
        post.setSlideUrl(uploadSlideDir);

        //포스트 저장폴더 생성
        String uploadPostDir = "teamPost" + "\\" + post.getUuid();
        File postDir = new File(uploadPostDir);
        if(!postDir.exists()){
            postDir.mkdirs();
        }
        post.setPosterUrl(uploadPostDir + "\\" + "poster");
        post.setDemoUrl(uploadPostDir + "\\" + "demo");
        post.setTeamProfileUrl(uploadPostDir + "\\" + "teamProfile");

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

