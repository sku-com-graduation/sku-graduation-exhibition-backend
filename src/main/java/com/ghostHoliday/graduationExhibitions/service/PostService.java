package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.*;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.Base64Utility;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.lang.model.element.NestingKind;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final EncryptionService encryptionService;
    private final Base64Utility base64Utility;
    private final StudentRepository studentRepository;
    private final FileUtility fileUtility;
    private final int MAX_IMAGES = 10;
    private final TeamRepository teamRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;


    public Long save(Post post) {
        return postRepository.save(post).getId();
    }


    @Transactional
    public SearchPostInfoDTO searchPostInfo(String uuid) throws Exception {
        Post post = postRepository.findByUuid(uuid).get();
        Team team = teamRepository.findByPostId(post.getId()).get();
        Long requestTeamId = team.getId();

        List<StudentInfoDTO> studentInfoDTOS = new ArrayList<>();
        List<Student> students = studentRepository.findAllByTeamId(requestTeamId);

        for (Student student : students) {
            StudentInfoDTO studentInfoDTO = new StudentInfoDTO();
            StudentProfile studentProfile = student.getStudentProfile();

            // 이름 (null 체크)
            studentInfoDTO.setName(student.getName() != null ? student.getName() : "이름 없음");

            // 정보 (null 체크)
            studentInfoDTO.setInfo(studentProfile.getInfo() != null ? studentProfile.getInfo() : "정보 없음");

            // 역할 (null 체크)
            studentInfoDTO.setRole(student.getRole() != null ? student.getRole() : null);

            // Github URL (null 체크)
            studentInfoDTO.setGithubUrl(studentProfile.getGithubUrl() != null ? studentProfile.getGithubUrl() : "Github URL 없음");

            // 이메일 (null 체크)
            studentInfoDTO.setStudentEmail(studentProfile.getStudentEmail() != null ? studentProfile.getStudentEmail() : "이메일 없음");

            // 블로그 (null 체크)
            studentInfoDTO.setStudentBlog(studentProfile.getStudentBlog() != null ? studentProfile.getStudentBlog() : "블로그 없음");

            // 프로필 이미지 URL (null 체크)
            String profileImageUrl = studentProfile.getStudentProfileUrl();
            studentInfoDTO.setProfileImage(profileImageUrl != null && !profileImageUrl.isEmpty()
                    ? base64Utility.encodeFileToBase64(profileImageUrl)
                    : ""); // 기본값은 빈 문자열로 설정 (혹은 기본 이미지를 설정할 수 있음)

            studentInfoDTOS.add(studentInfoDTO);
        }

        PostTeamInfoDTO postTeamInfoDTO = new PostTeamInfoDTO();
        postTeamInfoDTO.setUuid(post.getUuid());
        postTeamInfoDTO.setProjectName(post != null ? post.getTitle() : null);
        postTeamInfoDTO.setExplanation(post != null ? post.getContent() : null);
        postTeamInfoDTO.setCategory(team.getCategory());

        postTeamInfoDTO.setTeamProfileImage(
                post != null && post.getTeamProfileUrl() != null && !post.getTeamProfileUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getTeamProfileUrl()) : null
        );

        postTeamInfoDTO.setSlideImages(
                post != null && post.getSlideUrl() != null && !post.getSlideUrl().isEmpty() && !Files.list(Paths.get(post.getSlideUrl())).findAny().isEmpty()
                        ? base64Utility.ImagesToBase64(post.getSlideUrl())
                        : null
        );

        postTeamInfoDTO.setPosterImage(
                post != null && post.getPosterUrl() != null && !post.getPosterUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getPosterUrl()) : null
        );

        postTeamInfoDTO.setDemoVideo(
                post != null && post.getDemoUrl() != null && !post.getDemoUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getDemoUrl()) : null
        );

        return new SearchPostInfoDTO(studentInfoDTOS, postTeamInfoDTO);
    }

    @Transactional
    public void updatePostInfo(UpdateTeamPostDTO dto, String userEmail) throws Exception {
        String requestedTeamUuId  = dto.getTeamUuid();

        Post post = postRepository.findByUuid(requestedTeamUuId).get();

        Team reqestedTeam = teamRepository.findByPostId(post.getId())
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));



        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(reqestedTeam.getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        reqestedTeam.setCategory(dto.getCategory());

        // 팀 프로필 이미지 업로드
        String teamProfileExtension = fileUtility.getImageFileExtension(dto.getTeamProfileImage().getOriginalFilename());
        if (teamProfileExtension == null) {
            throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + dto.getTeamProfileImage().getOriginalFilename());
        }
        String teamProfilefileName = "teamProfile." + teamProfileExtension;
        Path teamProfileFilePath = Paths.get("teamPost",post.getUuid(),teamProfilefileName);
        post.setTeamProfileUrl(teamProfileFilePath.toString());
        // 기존 파일 삭제
        if (Files.exists(teamProfileFilePath)) {
            Files.delete(teamProfileFilePath);
        }

        Files.write(teamProfileFilePath, dto.getTeamProfileImage().getBytes());

        // 데모 영상 업로드
        String demoExtension = fileUtility.getVideoFileExtension(dto.getDemo().getOriginalFilename());
        if (demoExtension == null) {
            throw new IllegalStateException("avi, mp4, mkv 파일만 업로드 가능합니다. " + dto.getDemo().getOriginalFilename());
        }
        String demoFileName = "demo."+demoExtension;
        Path demoFilePath = Paths.get("teamPost",post.getUuid(),demoFileName);
        post.setDemoUrl(demoFilePath.toString());
        // 기존 파일 삭제
        if (Files.exists(demoFilePath)) {
            Files.delete(demoFilePath);
        }

        Files.write(demoFilePath, dto.getDemo().getBytes());

        // 포스터 이미지 업로드
        String posterExtension = fileUtility.getImageFileExtension(dto.getPosterImg().getOriginalFilename());
        if (posterExtension == null) {
            throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + dto.getPosterImg().getOriginalFilename());
        }
        String posterFileName = "poster." + posterExtension;
        Path posterFilePath = Paths.get("teamPost",post.getUuid(),posterFileName);
        post.setPosterUrl(posterFilePath.toString());
        // 기존 파일 삭제
        if (Files.exists(posterFilePath)) {
            Files.delete(posterFilePath);
        }

        Files.write(posterFilePath, dto.getPosterImg().getBytes());
    }



    @Transactional
    public void updateSlideImage(UpdateSlideImageDTO dto, String userEmail) throws Exception {
        try{
            String requestedTeamUuId  = dto.getTeamUuid();

            Account account = accountRepository.findAccountByUserEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            Post post = postRepository.findByUuid(requestedTeamUuId).get();

            Team reqestedTeam = teamRepository.findByPostId(post.getId())
                    .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));


            Team userTeam = account.getTeam();
            if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(reqestedTeam.getId()))) {
                throw new IllegalStateException("해당 팀의 슬라이드를 수정할 권한이 없습니다.");
            }



            List<MultipartFile> files = dto.getFiles();

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

    @Transactional
    public void updateStudentProfileByPost(UpdateStudentProfileByPostDTO dto, String userEmail) throws Exception {
        String requestedTeamUuId  = dto.getTeamUuid();

        Post post = postRepository.findByUuid(requestedTeamUuId).get();

        Team reqestedTeam = teamRepository.findByPostId(post.getId())
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));


        Long requestedStudentId  = encryptionService.decryptPrimaryKey(dto.getEncryptedStudentId());


        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        Student student = studentRepository.findById(requestedStudentId).get();
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(reqestedTeam.getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(student.getTeam().getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        StudentProfile studentProfile = studentRepository.findById(requestedStudentId).get().getStudentProfile();
        studentProfile.setGithubUrl(dto.getGithubUrl());
        studentProfile.setStudentEmail(dto.getStudentEmail());
        studentProfile.setStudentBlog(dto.getStudentBlog());
        studentProfile.setInfo(dto.getInfo());
        String url = saveStudentProfileImage(dto.getProfileImage(), student.getStudentNumber());
        studentProfile.setStudentProfileUrl(url);
    }

    public boolean verifyEditPermission(String token, String requestedUuid){
        Account account = accountService.tokenToAccount(token);
        if (account.getRole().equals(Role.ADMIN))
            return true;

        String uuid = account.getTeam().getPost().getUuid();
        if (requestedUuid.equals(uuid))
            return true;
        else
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
    }


    static void cleanDirectory(File directory) {
        File[] files = directory.listFiles();
        Arrays.stream(files).forEach(File::delete);
    }


    private String saveStudentProfileImage(MultipartFile studentImage, String number) throws IOException {
        // 저장할 디렉토리 경로
        String uploadDir = "studentProfileImage";
        File dir = new File(uploadDir);

        // 디렉토리가 없으면 생성
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 파일 확장자 추출
        String extension = fileUtility.getImageFileExtension(studentImage.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("png"))) {
            throw new RuntimeException("지원되지 않는 파일 형식입니다.");
        }

        String fileName = number + "." + extension;

        // 파일을 실제 경로에 저장
        Path path = Paths.get(uploadDir, fileName);
        Files.write(path, studentImage.getBytes());

        // 저장된 파일 경로 반환
        return path.toString();
    }





}