package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.account.*;
import com.ghostHoliday.graduationExhibitions.repository.*;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.ghostHoliday.graduationExhibitions.utility.S3Uploader;
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;

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
    private final HomeRepository homeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final S3Uploader s3Uploader;
    private final ProfessorRepository professorRepository;

    @Transactional
    public ResponseEntity<Object> login(LoginRequestDTO request ) {
        Account account = accountRepository.findAccountByUserEmail(request.getUserName())
                .orElseThrow(() -> new RuntimeException("아이디가 잘못되었습니다."));

        if (!passwordEncoder.matches(request.getPassword(), account.getPwd())) {
            throw new RuntimeException("비밀번호가 잘못되었습니다.");
        }

        account.setRecent(LocalDateTime.now());

        String accessToken = jwtUtility.generateToken(request.getUserName(), account.getRole());
        String refreshToken = jwtUtility.generateRefreshToken(request.getUserName());

        refreshTokenRepository.deleteByAccount(account);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setAccount(account);
        newRefreshToken.setRefreshToken(refreshToken);
        newRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(newRefreshToken);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("Strict")
                .build();

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setRole(account.getRole());
        loginDTO.setRecent(account.getRecent());

        if (account.getRole().equals(Role.ADMIN)) {
            loginDTO.setTeamName("ADMIN");
            loginDTO.setUuid(null);
        } else {
            loginDTO.setTeamName(account.getTeam().getName());
            loginDTO.setUuid(account.getTeam().getPost().getUuid());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(loginDTO);
    }

    @Transactional
    public int logout(HttpServletRequest request ) {
        String token = jwtUtility.extractAccessTokenFromCookie(request);
        String email = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(email).get();
        return refreshTokenRepository.deleteByAccount(account);
    }

    @Transactional
    public void createAccount(RegisterAccountRequest request) throws IOException, CsvException {
        ArrayList<Account> accounts = new ArrayList<>();
        int year = request.getYear();

        if (!homeRepository.existsByExhibitionYear(String.valueOf(year))){
            Home home = new Home();
            home.setExhibitionYear(String.valueOf(year));
            home.setExhibitionDate("");
            home.setExhibitionHour("");
            homeRepository.save(home);
        }

        for (RegisterAccountDTO accountInfo : request.getRegisterAccounts()) {
            Post post = createNewPost();
            postRepository.save(post);
            Professor professor = professorRepository.findByName(accountInfo.getProfessorName()).orElseThrow(() -> new IllegalArgumentException("교수 정보가 없습니다."));
            Team team = createTeam(accountInfo.getTeamName(), accountInfo.getCategory(),professor, year, post);
            teamRepository.save(team);

            boolean isLeader = true;
            for (StudentInfoByAccountDTO studentInfo : accountInfo.getStudentInfos()) {
                Student student = studentRepository.findByStudentNumber(studentInfo.getStudentNumber());
                student.setExhibitionYear(String.valueOf(year));
                if (isLeader) {
                    student.setRole(Role.LEADER);
                    Account account = createAccount(studentInfo.getStudentEmail(), passwordEncoder.encode(studentInfo.getStudentNumber()), team);
                    accounts.add(account);
                    isLeader = false;
                } else {
                    student.setRole(Role.MEMBER);
                }
                student.setTeam(team);
                student.getStudentProfile().setStudentEmail(studentInfo.getStudentEmail());
            }
        }

        accountRepository.saveAll(accounts);
    }

    @Transactional
    public List<FindAccountByYearResponseDTO> findAllAccountByYear(int year) throws Exception {
        List<Account> accounts = accountRepository.findNonAdminAccountsByExhibitionYear(year);
        ArrayList<FindAccountByYearResponseDTO> result = new ArrayList<>();

        for (Account account : accounts) {
            String encryptedAccountId = encryptionService.encryptPrimaryKey(account.getId());
            Team team = account.getTeam();

            if (year == team.getExhibitionYear()){
                FindAccountByYearResponseDTO dto = new FindAccountByYearResponseDTO();
                dto.setEncryptedAccountId(encryptedAccountId);
                dto.setTeamName(team.getName());
                dto.setUserEmail(account.getUserEmail());
                dto.setRecent(account.getRecent());
                result.add(dto);
            }
        }

        return result;
    }

    @Transactional
    public void deleteAccount(ArrayList<Long> accountIds){
        for (Long id : accountIds) {
            Optional<Account> optionalAccount = accountRepository.findById(id);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                account.setTeam(null);
                accountRepository.save(account);
                accountRepository.delete(account);
            }
        }
    }

    @Transactional
    public void resetAccount(ArrayList<Long> AccountsId){
        accountRepository.resetPasswordsToDefault(AccountsId);
    }

    public FindStudentInfoByCreateAccountResponse findInfosByCreateAccount(String token) throws AccessDeniedException {
        String userEmail = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!account.getRole().equals(Role.ADMIN)){
            throw new AccessDeniedException("권한이 없습니다.");
        }

        //교수 검색
        List<ProfessorInfosByCreateAccount> professorInfos = new ArrayList<>();
        for (Professor professor : professorRepository.findAll()) {
            ProfessorInfosByCreateAccount professorInfo = new ProfessorInfosByCreateAccount();
            professorInfo.setProfessorName(professor.getName());
            professorInfos.add(professorInfo);
        }

        //학생 검색
        List<StudentInfosByCreateAccount> studentInfos = new ArrayList<>();
        for (Student student : studentRepository.findByTeamIsNull()) {
            StudentInfosByCreateAccount studentInfo = new StudentInfosByCreateAccount();
            studentInfo.setStudentName(student.getName());
            studentInfo.setStudentNumber(student.getStudentNumber());
            studentInfo.setStudentEmail(student.getStudentProfile().getStudentEmail());
            studentInfos.add(studentInfo);
        }
        return new FindStudentInfoByCreateAccountResponse(professorInfos, studentInfos);


    }

    public Post createNewPost() {
        Post post = new Post();

        // URL은 초기값 null로 설정 (업로드 시점에 반영)
        post.setSlideUrl(null);
        post.setPosterUrl(null);
        post.setDemoUrl(null);
        post.setTeamProfileUrl(null);

        return post;
    }

    public static Team createTeam(String teamName, Category category,Professor professor, int year, Post post){
        Team team = new Team();
        team.setName(teamName);
        team.setCategory(category);
        team.setProfessor(professor);
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
