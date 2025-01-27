package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
    public void updatePostInfo(String token, String encryptionTeamId, MultipartFile teamProfileImg, MultipartFile demo, MultipartFile poster) throws Exception {
        Account account = accountService.tokenToAccount(token);
        Team team = account.getTeam();
        Long decryptedTeamId = encryptionService.decryptPrimaryKey(encryptionTeamId);

        if (!Objects.equals(team.getId(), decryptedTeamId)) {
            throw new IllegalArgumentException("잘못된 처리입니다.");
        }
        Post post = team.getPost();

        // 팀 프로필 이미지 업로드
        String teamProfileExtension = fileUtility.getImageFileExtension(teamProfileImg.getOriginalFilename());
        if (teamProfileExtension == null) {
            throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + teamProfileImg.getOriginalFilename());
        }

        String teamProfileFileName = "." + teamProfileExtension;
        Path teamProfileFilePath = Paths.get(post.getTeamProfileUrl() + teamProfileFileName);

        // 기존 파일 삭제
        if (Files.exists(teamProfileFilePath)) {
            Files.delete(teamProfileFilePath);
        }

        Files.write(teamProfileFilePath, teamProfileImg.getBytes());

        // 데모 영상 업로드
        String demoExtension = fileUtility.getVideoFileExtension(demo.getOriginalFilename());
        if (demoExtension == null) {
            throw new IllegalStateException("avi, mp4, mkv 파일만 업로드 가능합니다. " + demo.getOriginalFilename());
        }

        String demoFileName = "." + demoExtension;
        Path demoFilePath = Paths.get(post.getDemoUrl() + demoFileName);

        // 기존 파일 삭제
        if (Files.exists(demoFilePath)) {
            Files.delete(demoFilePath);
        }

        Files.write(demoFilePath, demo.getBytes());

        // 포스터 이미지 업로드
        String posterExtension = fileUtility.getImageFileExtension(poster.getOriginalFilename());
        if (posterExtension == null) {
            throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + poster.getOriginalFilename());
        }

        String posterFileName = "." + posterExtension;
        Path posterFilePath = Paths.get(post.getPosterUrl() + posterFileName);

        // 기존 파일 삭제
        if (Files.exists(posterFilePath)) {
            Files.delete(posterFilePath);
        }

        Files.write(posterFilePath, poster.getBytes());
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
        String uploadDir = post.getSlideUrl();
        File directory = new File(uploadDir);
        cleanDirectory(directory);

        long currentFileCount = Files.list(Paths.get(uploadDir))
                .filter(path -> !Files.isDirectory(path))
                .count();

        if (currentFileCount + files.size() > MAX_IMAGES) {
                    throw new IllegalStateException("최대 파일 업로드 제한(" + MAX_IMAGES + "개)을 초과합니다.");
        }


        int fileIndex = 1;
        for (MultipartFile file : files) {
            String extention = fileUtility.getImageFileExtension(file.getOriginalFilename());
            // 이미지 파일 여부 확인
            if (extention == null) {
                throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + file.getOriginalFilename());
            }

            // 파일 저장
            String fileName = "slide" + fileIndex + "." + extention;
            Path filePath = Paths.get(uploadDir +"\\"+ fileName);
            Files.write(filePath, file.getBytes());
            fileIndex++;
        }

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    static void cleanDirectory(File directory) {
        File[] files = directory.listFiles();
        Arrays.stream(files).forEach(File::delete);
    }


}



