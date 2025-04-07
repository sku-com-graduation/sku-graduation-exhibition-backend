package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.post.*;
import com.ghostHoliday.graduationExhibitions.repository.*;
import com.ghostHoliday.graduationExhibitions.utility.Base64Utility;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    private final JwtUtility jwtUtility;
    private final ProfessorRepository professorRepository;
    private final TeamService teamService;


    public Long save(Post post) {
        return postRepository.save(post).getId();
    }


    @Transactional
    public EditPostInfoResponseDTO searchEditPostInfo(String token, String uuid) throws Exception {
        String userEmail = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team team;
        Post post = postRepository.findByUuid(uuid).get();
        team = teamRepository.findByPostId(post.getId()).get();
        if (account.getRole().equals(Role.USER)){
            if (!Objects.equals(account.getTeam().getId(), team.getId()))
                throw new AccessDeniedException("해당 팀 수정 권한이 없습니다.");
        }

        EditPostInfoResponseDTO response = new EditPostInfoResponseDTO();


        response.setTitle(post != null ? post.getTitle() : null);
        response.setContent(post != null ? post.getContent() : null);
        response.setCategory(team.getCategory());

        response.setTeamProfileImage(
                post != null && post.getTeamProfileUrl() != null && !post.getTeamProfileUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getTeamProfileUrl()) : null
        );

        response.setSlideImages(
                post != null && post.getSlideUrl() != null && !post.getSlideUrl().isEmpty() && !Files.list(Paths.get(post.getSlideUrl())).findAny().isEmpty()
                        ? base64Utility.ImagesToBase64(post.getSlideUrl())
                        : null
        );

        response.setPosterImage(
                post != null && post.getPosterUrl() != null && !post.getPosterUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getPosterUrl()) : null
        );

        response.setDemoVideo(
                post != null && post.getDemoUrl() != null && !post.getDemoUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getDemoUrl()) : null
        );


        List<EditStudentResponse> editStudentResponses = new ArrayList<>();
        for (Student student : studentRepository.findAllByTeamId(team.getId())) {
            EditStudentResponse editStudentResponse = new EditStudentResponse();
            StudentProfile studentProfile = student.getStudentProfile();

            editStudentResponse.setEncryptedStudentProfileId(encryptionService.encryptPrimaryKey(student.getId()));
            // 이름 (null 체크)
            editStudentResponse.setName(student.getName() != null ? student.getName() : "");

            // 정보 (null 체크)
            editStudentResponse.setInfo(studentProfile.getInfo() != null ? studentProfile.getInfo() : "");

            // 역할 (null 체크)
            editStudentResponse.setRole(student.getRole() != null ? student.getRole() : null);

            // Github URL (null 체크)
            editStudentResponse.setGithubUrl(studentProfile.getGithubUrl() != null ? studentProfile.getGithubUrl() : "");

            // 이메일 (null 체크)
            editStudentResponse.setStudentEmail(studentProfile.getStudentEmail() != null ? studentProfile.getStudentEmail() : "");

            // 블로그 (null 체크)
            editStudentResponse.setStudentBlog(studentProfile.getStudentBlog() != null ? studentProfile.getStudentBlog() : "");

            // 프로필 이미지 URL (null 체크)
            String profileImageUrl = studentProfile.getStudentProfileUrl();
            editStudentResponse.setProfileImage(profileImageUrl != null && !profileImageUrl.isEmpty()
                    ? base64Utility.encodeFileToBase64(profileImageUrl)
                    : ""); // 기본값은 빈 문자열로 설정 (혹은 기본 이미지를 설정할 수 있음)

            editStudentResponses.add(editStudentResponse);
        }
        response.setStudents(editStudentResponses);
        return response;

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
            studentInfoDTO.setName(student.getName() != null ? student.getName() : "");

            // 정보 (null 체크)
            studentInfoDTO.setInfo(studentProfile.getInfo() != null ? studentProfile.getInfo() : "");

            // 역할 (null 체크)
            studentInfoDTO.setRole(student.getRole() != null ? student.getRole() : null);

            // Github URL (null 체크)
            studentInfoDTO.setGithubUrl(studentProfile.getGithubUrl() != null ? studentProfile.getGithubUrl() : "");

            // 이메일 (null 체크)
            studentInfoDTO.setStudentEmail(studentProfile.getStudentEmail() != null ? studentProfile.getStudentEmail() : "");

            // 블로그 (null 체크)
            studentInfoDTO.setStudentBlog(studentProfile.getStudentBlog() != null ? studentProfile.getStudentBlog() : "");

            // 프로필 이미지 URL (null 체크)
            String profileImageUrl = studentProfile.getStudentProfileUrl();
            studentInfoDTO.setProfileImage(profileImageUrl != null && !profileImageUrl.isEmpty()
                    ? base64Utility.encodeFileToBase64(profileImageUrl)
                    : ""); // 기본값은 빈 문자열로 설정 (혹은 기본 이미지를 설정할 수 있음)

            studentInfoDTOS.add(studentInfoDTO);
        }

        PostTeamInfoDTO postTeamInfoDTO = new PostTeamInfoDTO();
        postTeamInfoDTO.setTeamUuid(post.getUuid());
        postTeamInfoDTO.setTitle(post != null ? post.getTitle() : null);
        postTeamInfoDTO.setContent(post != null ? post.getContent() : null);
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


        Professor professor = team.getProfessor();
        ProfessorInfoDTO professorInfoDTO = new ProfessorInfoDTO();

        if (professor != null) {
            professorInfoDTO.setProfessorImage(
                    professor.getImageUrl() != null && !professor.getImageUrl().isEmpty()
                            ? base64Utility.encodeFileToBase64(professor.getImageUrl())
                            : ""
            );
            professorInfoDTO.setProfessorName(professor.getName() != null ? professor.getName() : "");
            professorInfoDTO.setProfessorEmail(professor.getEmail() != null ? professor.getEmail() : "");
            professorInfoDTO.setProfessorTenure(professor.isTenure());
        } else {
            professorInfoDTO.setProfessorName("");
            professorInfoDTO.setProfessorEmail("");
            professorInfoDTO.setProfessorTenure(false); // 교수 정보가 없으면 tenure 기본값 false
        }
        return new SearchPostInfoDTO(studentInfoDTOS, postTeamInfoDTO, professorInfoDTO);
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
        if (dto.getTeamProfileImage() != null && !dto.getTeamProfileImage().isEmpty()) {  // 파일이 null이 아니고 비어있지 않으면 업로드
            String teamProfileExtension = fileUtility.getImageFileExtension(dto.getTeamProfileImage().getOriginalFilename());
            if (teamProfileExtension == null || (!teamProfileExtension.equalsIgnoreCase("jpg") && !teamProfileExtension.equalsIgnoreCase("png"))) {
                throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + dto.getTeamProfileImage().getOriginalFilename());
            }
            String teamProfilefileName = "teamProfile." + teamProfileExtension;
            Path teamProfileFilePath = Paths.get("teamPost", post.getUuid(), teamProfilefileName);
            post.setTeamProfileUrl(teamProfileFilePath.toString());
            // 기존 파일 삭제
            if (Files.exists(teamProfileFilePath)) {
                Files.delete(teamProfileFilePath);
            }

            Files.write(teamProfileFilePath, dto.getTeamProfileImage().getBytes());
        } else {
            // 팀 프로필 이미지가 null이거나 비어 있으면 기존 파일 삭제
            if (post.getTeamProfileUrl() != null) {
                Path existingProfileImagePath = Paths.get(post.getTeamProfileUrl());
                if (Files.exists(existingProfileImagePath)) {
                    Files.delete(existingProfileImagePath);
                }
                post.setTeamProfileUrl(null);  // 기존 이미지 URL을 null로 설정
            }
        }

        // 데모 영상 업로드
        if (dto.getDemoVideo() != null && !dto.getDemoVideo().isEmpty()) {  // 영상이 null이 아니고 비어있지 않으면 업로드
            String demoExtension = fileUtility.getVideoFileExtension(dto.getDemoVideo().getOriginalFilename());
            if (demoExtension == null || (!demoExtension.equalsIgnoreCase("avi") && !demoExtension.equalsIgnoreCase("mp4") && !demoExtension.equalsIgnoreCase("mkv"))) {
                throw new IllegalStateException("avi, mp4, mkv 파일만 업로드 가능합니다. " + dto.getDemoVideo().getOriginalFilename());
            }
            String demoFileName = "demo." + demoExtension;
            Path demoFilePath = Paths.get("teamPost", post.getUuid(), demoFileName);
            post.setDemoUrl(demoFilePath.toString());
            // 기존 파일 삭제
            if (Files.exists(demoFilePath)) {
                Files.delete(demoFilePath);
            }

            Files.write(demoFilePath, dto.getDemoVideo().getBytes());
        } else {
            // 데모 영상이 null이거나 비어 있으면 기존 파일 삭제
            if (post.getDemoUrl() != null) {
                Path existingDemoVideoPath = Paths.get(post.getDemoUrl());
                if (Files.exists(existingDemoVideoPath)) {
                    Files.delete(existingDemoVideoPath);
                }
                post.setDemoUrl(null);  // 기존 데모 영상 URL을 null로 설정
            }
        }

        // 포스터 이미지 업로드
        if (dto.getPosterImage() != null && !dto.getPosterImage().isEmpty()) {  // 이미지가 null이 아니고 비어있지 않으면 업로드
            String posterExtension = fileUtility.getImageFileExtension(dto.getPosterImage().getOriginalFilename());
            if (posterExtension == null || (!posterExtension.equalsIgnoreCase("jpg") && !posterExtension.equalsIgnoreCase("png"))) {
                throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + dto.getPosterImage().getOriginalFilename());
            }
            String posterFileName = "poster." + posterExtension;
            Path posterFilePath = Paths.get("teamPost", post.getUuid(), posterFileName);
            post.setPosterUrl(posterFilePath.toString());
            // 기존 파일 삭제
            if (Files.exists(posterFilePath)) {
                Files.delete(posterFilePath);
            }

            Files.write(posterFilePath, dto.getPosterImage().getBytes());
        } else {
            // 포스터 이미지가 null이거나 비어 있으면 기존 파일 삭제
            if (post.getPosterUrl() != null) {
                Path existingPosterImagePath = Paths.get(post.getPosterUrl());
                if (Files.exists(existingPosterImagePath)) {
                    Files.delete(existingPosterImagePath);
                }
                post.setPosterUrl(null);  // 기존 포스터 이미지 URL을 null로 설정
            }
        }
    }




    @Transactional
    public void updateSlideImage(UpdateSlideImageDTO dto, String userEmail) throws Exception {
        try {
            String requestedTeamUuId  = dto.getTeamUuid();

            Account account = accountRepository.findAccountByUserEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            Post post = postRepository.findByUuid(requestedTeamUuId).get();

            Team requestedTeam = teamRepository.findByPostId(post.getId())
                    .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));

            Team userTeam = account.getTeam();
            if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeam.getId()))) {
                throw new IllegalStateException("해당 팀의 슬라이드를 수정할 권한이 없습니다.");
            }

            List<MultipartFile> files = dto.getFiles();

            // 슬라이드 디렉토리 초기화
            String uploadDir = post.getSlideUrl();
            File directory = new File(uploadDir);
            cleanDirectory(directory);

            long currentFileCount = Files.list(Paths.get(uploadDir))
                    .filter(path -> !Files.isDirectory(path))
                    .count();

            // 업로드된 파일 개수와 기존 파일 개수를 합쳤을 때 최대 개수를 초과하는지 체크
            if (currentFileCount + files.size() > MAX_IMAGES) {
                throw new IllegalStateException("최대 파일 업로드 제한(" + MAX_IMAGES + "개)을 초과합니다.");
            }

            int fileIndex = 1;
            for (MultipartFile file : files) {
                // 파일이 null이거나 비어있으면 건너뛰기
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String extension = fileUtility.getImageFileExtension(file.getOriginalFilename());
                // 이미지 파일 여부 확인
                if (extension == null) {
                    throw new IllegalStateException("jpg, png 파일만 업로드 가능합니다. " + file.getOriginalFilename());
                }

                // 파일 저장
                String fileName = "slide" + fileIndex + "." + extension;
                Path filePath = Paths.get(uploadDir, fileName);
                Files.write(filePath, file.getBytes());
                fileIndex++;
            }

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void updateStudentProfileByPost(UpdateStudentProfileByPostDTO dto, String userEmail) throws Exception {
        String requestedTeamUuId = dto.getTeamUuid();

        Post post = postRepository.findByUuid(requestedTeamUuId)
                .orElseThrow(() -> new IllegalStateException("해당 포스트를 찾을 수 없습니다."));

        Team requestedTeam = teamRepository.findByPostId(post.getId())
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));

        Long requestedStudentId = encryptionService.decryptPrimaryKey(dto.getEncryptedStudentId());

        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        Student student = studentRepository.findById(requestedStudentId)
                .orElseThrow(() -> new IllegalStateException("학생을 찾을 수 없습니다."));

        // 권한 체크
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeam.getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(student.getTeam().getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        // 학생 프로필 업데이트
        StudentProfile studentProfile = student.getStudentProfile();
        studentProfile.setGithubUrl(dto.getGithubUrl());
        studentProfile.setStudentEmail(dto.getStudentEmail());
        studentProfile.setStudentBlog(dto.getStudentBlog());
        studentProfile.setInfo(dto.getInfo());

        // 프로필 이미지 처리
        if (dto.getProfileImage() == null || dto.getProfileImage().isEmpty()) {
            // 이미지가 null 이면 기존 이미지를 삭제
            if (studentProfile.getStudentProfileUrl() != null) {
                Path existingProfileImagePath = Paths.get(studentProfile.getStudentProfileUrl());
                if (Files.exists(existingProfileImagePath)) {
                    Files.delete(existingProfileImagePath);  // 기존 이미지 삭제
                }
            }
            studentProfile.setStudentProfileUrl(null);  // 기존 이미지 URL을 null로 설정
        } else {
            // 새로운 이미지가 있다면 저장
            String url = saveStudentProfileImage(dto.getProfileImage(), student.getStudentNumber());
            studentProfile.setStudentProfileUrl(url);
        }
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
        String uploadDir = Paths.get("studentProfileImage").toString();
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