package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.dto.UpdatePostByTeamDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateSlideImageDTO;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.AESUtil;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final TeamRepository teamRepository;
    private final AccountRepository accountRepository;
    private final EncryptionService encryptionService;
    private final AccountService accountService;
    private final JwtUtility jwtUtility;
    private final FileUtility fileUtility;
    private final int MAX_IMAGES = 10;



    public Long save(Post post) {
        return postRepository.save(post).getId();
    }

    @Transactional
    public void updateSlideImage(String token,String encryptionTeamId, List<MultipartFile> files) throws Exception {
        try{
        Account account = accountService.tokenToAccount(token);
        Team team = account.getTeam();
        Long decryptedTeamId = encryptionService.decryptPrimaryKey(encryptionTeamId);

        if (!Objects.equals(team.getId(), decryptedTeamId)){
            throw new IllegalArgumentException("잘못된 처리입니다.");
        }

        Post post = team.getPost();
        String uploadDir = "teamPost" + "\\" + post.getUuid() + "\\" + "slideImage";
        File directory = new File(uploadDir);
        if(!directory.exists()){
            directory.mkdirs();
        }

        long currentFileCount = Files.list(Paths.get(uploadDir))
                .filter(path -> !Files.isDirectory(path))
                .count();

        if (currentFileCount + files.size() > MAX_IMAGES) {
                    throw new IllegalStateException("최대 파일 업로드 제한(" + MAX_IMAGES + "개)을 초과합니다.");
        }



        List<String> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String extention = fileUtility.getFileExtension(file.getOriginalFilename());
            // 이미지 파일 여부 확인
            if (extention == null) {
                throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + file.getOriginalFilename());
            }

            // 파일 저장
            String fileName = findNextNumber(uploadDir) + "." + extention;
            Path filePath = Paths.get(uploadDir +"\\"+ fileName);
            Files.write(filePath, file.getBytes());
            savedFiles.add(fileName);
        }

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage(), e);
        }
    }
    static int findNextNumber(String uploadDir) throws IOException {
        // 기존 파일 이름 중 숫자를 추출하여 오름차순 정렬
        List<Integer> existingNumbers = new ArrayList<>();
        Files.list(Paths.get(uploadDir))
                .filter(path -> !Files.isDirectory(path))
                .map(path -> path.getFileName().toString())
                .filter(fileName -> fileName.matches("\\d+\\.jpg")) // "숫자.jpg" 형식인 파일만 필터링
                .forEach(fileName -> {
                    int number = Integer.parseInt(fileName.replace(".jpg", ""));
                    existingNumbers.add(number);
                });
        existingNumbers.sort(Integer::compareTo);

        // 다음 파일 번호 결정
        int nextFileNumber = existingNumbers.isEmpty() ? 1 : existingNumbers.get(existingNumbers.size() - 1) + 1;
        return nextFileNumber;
    }
}



