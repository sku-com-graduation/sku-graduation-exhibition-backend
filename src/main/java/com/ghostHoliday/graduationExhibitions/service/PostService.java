package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.*;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final AccountService accountService;
    private final StudentRepository studentRepository;
    private final FileUtility fileUtility;
    private final int MAX_IMAGES = 10;
    private final TeamRepository teamRepository;
    private final AccountRepository accountRepository;


    public Long save(Post post) {
        return postRepository.save(post).getId();
    }


    @Transactional
    public SearchPostInfoDTO searchPostInfo(String encryptedTeamId) throws Exception {
        Long requestTeamId = encryptionService.decryptPrimaryKey(encryptedTeamId);


        List<StudentInfoDTO> studentInfoDTOS = new ArrayList<>();
        List<Student> students = studentRepository.findAllByTeamId(requestTeamId);
        for (Student student : students) {
            StudentInfoDTO studentInfoDTO = new StudentInfoDTO();
            StudentProfile studentProfile = student.getStudentProfile();

            studentInfoDTO.setName(student.getName());
            studentInfoDTO.setInfo(studentProfile.getInfo());
            studentInfoDTO.setRole(student.getRole());
            studentInfoDTO.setGithubUrl(studentProfile.getGithubUrl());
            studentInfoDTO.setStudentEmail(studentProfile.getStudentEmail());
            studentInfoDTO.setStudentBlog(studentProfile.getStudentBlog());
            studentInfoDTO.setProfileImage(encodeFileToBase64(studentProfile.getStudentProfileUrl()));

            studentInfoDTOS.add(studentInfoDTO);
        }

        Team team = teamRepository.findById(requestTeamId).get();
        Post post = team.getPost();
        PostTeamInfoDTO postTeamInfoDTO = new PostTeamInfoDTO();
        postTeamInfoDTO.setProjectName(post.getTitle());
        postTeamInfoDTO.setExplanation(post.getContent());
        postTeamInfoDTO.setTeamProfileImage(encodeFileToBase64(post.getTeamProfileUrl()));
        postTeamInfoDTO.setSlideImages(slideImagesToBase64(post.getSlideUrl()));
        postTeamInfoDTO.setPosterImage(encodeFileToBase64(post.getPosterUrl()));
        postTeamInfoDTO.setDemoVideo(encodeFileToBase64(post.getDemoUrl()));

        return new SearchPostInfoDTO(studentInfoDTOS, postTeamInfoDTO);
    }

    @Transactional
    public void updatePostInfo(UpdateTeamPostDTO dto, String userEmail) throws Exception {
        Long requestedTeamId  = encryptionService.decryptPrimaryKey(dto.getEncryptedTeamId());

        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeamId))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        Team team = teamRepository.findById(requestedTeamId)
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));

        Post post = team.getPost();

        // 팀 프로필 이미지 업로드
        String teamProfileExtension = fileUtility.getImageFileExtension(dto.getTeamProfileImg().getOriginalFilename());
        if (teamProfileExtension == null) {
            throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + dto.getTeamProfileImg().getOriginalFilename());
        }
        String teamProfilefileName = "teamProfile." + teamProfileExtension;
        Path teamProfileFilePath = Paths.get("teamPost",post.getUuid(),teamProfilefileName);
        post.setTeamProfileUrl(teamProfileFilePath.toString());
        // 기존 파일 삭제
        if (Files.exists(teamProfileFilePath)) {
            Files.delete(teamProfileFilePath);
        }

        Files.write(teamProfileFilePath, dto.getTeamProfileImg().getBytes());

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
        String posterExtension = fileUtility.getImageFileExtension(dto.getPoster().getOriginalFilename());
        if (posterExtension == null) {
            throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + dto.getPoster().getOriginalFilename());
        }
        String posterFileName = "poster." + posterExtension;
        Path posterFilePath = Paths.get("teamPost",post.getUuid(),posterFileName);
        post.setPosterUrl(posterFilePath.toString());
        // 기존 파일 삭제
        if (Files.exists(posterFilePath)) {
            Files.delete(posterFilePath);
        }

        Files.write(posterFilePath, dto.getPoster().getBytes());
    }



    @Transactional
    public void updateSlideImage(UpdateSlideImageDTO dto, String userEmail) throws Exception {
        try{
        Long requestedTeamId  = encryptionService.decryptPrimaryKey(dto.getEncryptedTeamId());

        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeamId))) {
            throw new IllegalStateException("해당 팀의 슬라이드를 수정할 권한이 없습니다.");
        }

        Team team = teamRepository.findById(requestedTeamId)
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));

        List<MultipartFile> files = dto.getFiles();

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

    @Transactional
    public void updateStudentProfileByPost(UpdateStudentProfileByPostDTO dto, String userEmail) throws Exception {
        Long requestedTeamId  = encryptionService.decryptPrimaryKey(dto.getEncryptedTeamId());
        Long requestedStudentId  = encryptionService.decryptPrimaryKey(dto.getEncryptedStudentId());


        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        Student student = studentRepository.findById(requestedStudentId).get();
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeamId))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(student.getTeam().getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        Team team = teamRepository.findById(requestedTeamId)
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));

        StudentProfile studentProfile = studentRepository.findById(requestedStudentId).get().getStudentProfile();
        studentProfile.setGithubUrl(dto.getGithubUrl());
        studentProfile.setStudentEmail(dto.getStudentEmail());
        studentProfile.setStudentBlog(dto.getStudentBlog());
        studentProfile.setInfo(dto.getInfo());
        String url = saveStudentProfileImage(dto.getProfileImage(), student.getStudentNumber());
        studentProfile.setStudentProfileUrl(url);


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

    public static String encodeFileToBase64(String filePath) throws IOException {
        File imageFile = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(imageFile);

        byte[] imageBytes = fileInputStream.readAllBytes();
        fileInputStream.close();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static List<String> slideImagesToBase64(String folderPath) throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(folderPath))) {
            List<String> slideImages = new ArrayList<>();
            paths.filter(Files::isRegularFile) // 파일만 선택 (디렉토리 제외)
                    .forEach(path -> {
                        try {
                            String base64 = encodeFileToBase64(String.valueOf(path));
                            slideImages.add(base64);
                        } catch (IOException e) {
                            System.err.println("파일 변환 실패: " + path);
                            e.printStackTrace();
                        }
                    });
            return slideImages;
        }
    }

}



