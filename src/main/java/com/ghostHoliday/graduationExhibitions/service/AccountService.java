package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.account.*;
import com.ghostHoliday.graduationExhibitions.repository.*;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final HttpOnlyService httpOnlyService;


    @Transactional
    public ResponseEntity<Object> login(LoginRequestDTO request ) {
        Account account = accountRepository.findAccountByUserEmail(request.getUserName())
                .orElseThrow(() -> new RuntimeException("아이디가 잘못되었습니다."));

        if (!passwordEncoder.matches(request.getPassword(), account.getPwd())) {
            throw new RuntimeException("비밀번호가 잘못되었습니다.");
        }

        account.setRecent(LocalDateTime.now());

        // JWT 토큰 생성
        String accessToken = jwtUtility.generateToken(request.getUserName(), account.getRole());
        String refreshToken = jwtUtility.generateRefreshToken(request.getUserName());


        // 기존 리프레시 토큰 삭제 (같은 유저의 이전 토큰 제거)
        refreshTokenRepository.deleteByAccount(account);

        // 새로운 리프레시 토큰 저장
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setAccount(account);
        newRefreshToken.setRefreshToken(refreshToken);
        newRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(30));  // 30일 유효
        refreshTokenRepository.save(newRefreshToken);


        // Refresh Token을 HTTP-Only 쿠키에 저장
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)  // HTTPS 환경에서만 전송 (테스트 시 false 가능)
                .path("/")
                .maxAge(60 * 60 * 24)  // 24시간 유지
                .sameSite("Strict")
                .build();


        // LoginDTO 생성
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



    /**
     * csv로 받은 파일을 전부 저장
     */
    @Transactional
    public void registAccount(List<RegistAccountRequest> accountInfos) throws IOException, CsvException {

        ArrayList<Account> accounts = new ArrayList<>();
        int year = LocalDateTime.now().getYear();
        if (!homeRepository.existsByExhibitionYear(String.valueOf(year))){
            Home home = new Home();
            home.setExhibitionYear(String.valueOf(year));
            home.setExhibitionDate("");
            home.setExhibitionHour("");
            homeRepository.save(home);
        }
        for (RegistAccountRequest accountInfo : accountInfos) {

            // 해당 팀의 post 생성
            Post post = createNewPost();
            //포스트 저장폴더 생성
            String uploadDir = Paths.get("teamPost", post.getUuid()).toString();
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            postRepository.save(post);

            // 팀 생성
            Team team = createTeam(accountInfo.getTeamName(), accountInfo.getCategory(), year, post);
            teamRepository.save(team);
            boolean isLeader = true;
            for (StudentInfoByAccountDTO studentInfo : accountInfo.getStudentInfos()) {
                Student student = studentRepository.findByStudentNumber(studentInfo.getStudentNumber());
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
            accountRepository.saveAll(accounts);

        }

    }

    @Transactional
    public List<FindAccountByYearResponseDTO> findAllAccountByYear(int year) throws Exception {
        List<Account> accounts = accountRepository.findNonAdminAccountsByExhibitionYear(year);
        ArrayList<FindAccountByYearResponseDTO> FindAccountByYearResponseDTOS = new ArrayList<>();

        for (Account account : accounts) {
            String encryptedAccountId = encryptionService.encryptPrimaryKey(account.getId());
            FindAccountByYearResponseDTO findAccountByYearResponseDTO = new FindAccountByYearResponseDTO();
            Team team = account.getTeam();

            if (year == team.getExhibitionYear()){
                findAccountByYearResponseDTO.setEncryptedAccountId(encryptedAccountId);
                findAccountByYearResponseDTO.setTeamName(team.getName());
                findAccountByYearResponseDTO.setUserEmail(account.getUserEmail());
                findAccountByYearResponseDTO.setRecent(account.getRecent());
                FindAccountByYearResponseDTOS.add(findAccountByYearResponseDTO);
            }
        }

        return FindAccountByYearResponseDTOS;

    }

    public Account tokenToAccount(String token) {
        String userEmail = jwtUtility.validateToken(token).getSubject();
        return accountRepository.findAccountByUserEmail(userEmail).get();

    }

    @Transactional
    public void deleteAccount(ArrayList<Long> accountIds){

        for (Long id : accountIds) {
            // 각 계정 조회
            Optional<Account> optionalAccount = accountRepository.findById(id);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                // team 연결 해제
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




    public static Post createNewPost(){
        Post post = new Post();
        post.setTitle(null);
        post.setContent(null);
        // 슬라이드 저장 폴더 생성
        String uploadSlideDir = Paths.get("teamPost",post.getUuid(),"slideImage").toString();
        File slideDir = new File(uploadSlideDir);
        if(!slideDir.exists()){
            slideDir.mkdirs();
        }
        post.setSlideUrl(uploadSlideDir);

        //포스트 저장폴더 생성
        String uploadPostDir = Paths.get("teamPost",post.getUuid()).toString();
        File postDir = new File(uploadPostDir);
        if(!postDir.exists()){
            postDir.mkdirs();
        }
        post.setPosterUrl(Paths.get(uploadPostDir,"poster").toString());
        post.setDemoUrl(Paths.get(uploadPostDir,"demo").toString());
        post.setTeamProfileUrl(Paths.get(uploadPostDir,"teamProfile").toString());

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

