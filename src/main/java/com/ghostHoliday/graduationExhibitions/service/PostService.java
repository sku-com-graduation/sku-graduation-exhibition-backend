package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.post.*;
import com.ghostHoliday.graduationExhibitions.repository.*;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.ghostHoliday.graduationExhibitions.utility.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.AccessDeniedException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final EncryptionService encryptionService;
    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final AccountRepository accountRepository;
    private final JwtUtility jwtUtility;
    private final S3Uploader s3Uploader;


    public Long save(Post post) {
        return postRepository.save(post).getId();
    }


    @Transactional
    public UpdatePostInfoV2Response UpdatePostInfoV2(UpdatePostInfoV2Request request) {
        UpdatePostInfoV2Response response = new UpdatePostInfoV2Response();
        List<S3UrlDTO> images = new ArrayList<>();
        String baseDir = "teamPost/" + request.getTeamUuid() + "/";

        for (FileInfoDTO fileInfo : request.getImageInfos()) {
            if (fileInfo.getFileType().equals(FileType.TEAM_PROFILE)){
                String fileName = "teamProfile";
                String s3Path = baseDir + fileName;
                UploadUrlDTO uploadUrlDTO = s3Uploader.generatePreSignedUploadUrl(s3Path, fileInfo.getContentType(), fileInfo.getExtension());
                images.add(new S3UrlDTO(FileType.TEAM_PROFILE, uploadUrlDTO.getCloudFrontUrl(), uploadUrlDTO.getS3Url()));
            }
            else if (fileInfo.getFileType().equals(FileType.POSTER)){
                String fileName = "poster";
                String s3Path = baseDir + fileName;
                UploadUrlDTO uploadUrlDTO = s3Uploader.generatePreSignedUploadUrl(s3Path, fileInfo.getContentType(), fileInfo.getExtension());
                images.add(new S3UrlDTO(FileType.POSTER, uploadUrlDTO.getCloudFrontUrl(), uploadUrlDTO.getS3Url()));
            }
        }
        response.setImages(images);
        UpdatePostInfoV2Request.VideoInfo videoInfo = request.getVideoInfo();

        if (videoInfo.getFileType().equals(FileType.DEMO)){
            String fileName = "demo";
            String s3Path = baseDir + fileName;
            response.setVideo(s3Uploader.initiateMultipartUpload(
                    s3Path, videoInfo.getContentType(), videoInfo.getExtension(), videoInfo.getFileSize(), videoInfo.getPartSize()));
        }
        else{
            response.setVideo(null);
        }


        return response;
    }


    @Transactional
    public EditPostInfoResponseDTO searchEditPostInfo(String token, String uuid) throws Exception {
        String userEmail = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalStateException("포스트를 찾을 수 없습니다."));
        Team team = teamRepository.findByPostId(post.getId())
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (account.getRole().equals(Role.USER) &&
                !Objects.equals(account.getTeam().getId(), team.getId())) {
            throw new AccessDeniedException("해당 팀 수정 권한이 없습니다.");
        }

        EditPostInfoResponseDTO response = new EditPostInfoResponseDTO();
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setCategory(team.getCategory());

        // S3 URL 그대로 반환
        response.setTeamProfileImage(
                post.getTeamProfileUrl() != null ? s3Uploader.rebuildCdnUrl(post.getTeamProfileUrl()) : null
        );

        response.setPosterImage(
                post.getPosterUrl() != null ? s3Uploader.rebuildCdnUrl(post.getPosterUrl()) : null
        );

        response.setDemoVideo(
                post.getDemoUrl() != null ? s3Uploader.rebuildCdnUrl(post.getDemoUrl()) : null
        );

        List<EditStudentResponse> editStudentResponses = new ArrayList<>();
        for (Student student : studentRepository.findAllByTeamId(team.getId())) {
            StudentProfile profile = student.getStudentProfile();
            EditStudentResponse studentResponse = new EditStudentResponse();

            studentResponse.setEncryptedStudentProfileId(encryptionService.encryptPrimaryKey(student.getId()));
            studentResponse.setName(Optional.ofNullable(student.getName()).orElse(""));
            studentResponse.setInfo(Optional.ofNullable(profile.getInfo()).orElse(""));
            studentResponse.setRole(student.getRole());
            studentResponse.setGithubUrl(Optional.ofNullable(s3Uploader.rebuildCdnUrl(profile.getGithubUrl())).orElse(""));
            studentResponse.setStudentEmail(Optional.ofNullable(profile.getStudentEmail()).orElse(""));
            studentResponse.setStudentBlog(Optional.ofNullable(profile.getStudentBlog()).orElse(""));

            studentResponse.setProfileImage(
                    profile.getStudentProfileUrl() != null ? s3Uploader.rebuildCdnUrl(profile.getStudentProfileUrl()) : ""
            );

            editStudentResponses.add(studentResponse);
        }

        response.setStudents(editStudentResponses);
        return response;
    }


    @Transactional
    public SearchPostInfoDTO searchPostInfo(String uuid) throws Exception {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalStateException("해당 포스트를 찾을 수 없습니다."));
        Team team = teamRepository.findByPostId(post.getId())
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        Long requestTeamId = team.getId();

        List<StudentInfoDTO> studentInfoDTOS = new ArrayList<>();
        List<Student> students = studentRepository.findAllByTeamId(requestTeamId);

        for (Student student : students) {
            StudentInfoDTO studentInfoDTO = new StudentInfoDTO();
            StudentProfile studentProfile = student.getStudentProfile();

            studentInfoDTO.setName(Optional.ofNullable(student.getName()).orElse(""));
            studentInfoDTO.setInfo(Optional.ofNullable(studentProfile.getInfo()).orElse(""));
            studentInfoDTO.setRole(student.getRole());
            studentInfoDTO.setGithubUrl(Optional.ofNullable(studentProfile.getGithubUrl()).orElse(""));
            studentInfoDTO.setStudentEmail(Optional.ofNullable(studentProfile.getStudentEmail()).orElse(""));
            studentInfoDTO.setStudentBlog(Optional.ofNullable(studentProfile.getStudentBlog()).orElse(""));

            String profileImageUrl = s3Uploader.rebuildCdnUrl(studentProfile.getStudentProfileUrl());
            studentInfoDTO.setProfileImage(profileImageUrl != null ? profileImageUrl : "");

            studentInfoDTOS.add(studentInfoDTO);
        }

        PostTeamInfoDTO postTeamInfoDTO = new PostTeamInfoDTO();
        postTeamInfoDTO.setTeamUuid(post.getUuid());
        postTeamInfoDTO.setTitle(post.getTitle());
        postTeamInfoDTO.setContent(post.getContent());
        postTeamInfoDTO.setCategory(team.getCategory());

        postTeamInfoDTO.setTeamProfileImage(s3Uploader.rebuildCdnUrl(post.getTeamProfileUrl() ));
        postTeamInfoDTO.setPosterImage(s3Uploader.rebuildCdnUrl(post.getPosterUrl()));
        postTeamInfoDTO.setDemoVideo(s3Uploader.rebuildCdnUrl(post.getDemoUrl()));

        // 슬라이드 이미지 목록 S3에서 동적으로 조회
        List<String> slideImageUrls = new ArrayList<>();
        String slideFolder = post.getSlideUrl();
        if (slideFolder != null && !slideFolder.isEmpty()) {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(s3Uploader.getBucket())
                    .prefix(slideFolder)
                    .build();

            ListObjectsV2Response listResponse = s3Uploader.getS3Client().listObjectsV2(listRequest);
            for (S3Object object : listResponse.contents()) {
                String url = s3Uploader.getCloudFrontUrl() + "/" + object.key();
                slideImageUrls.add(url);
            }
        }
        postTeamInfoDTO.setSlideImages(!slideImageUrls.isEmpty() ? slideImageUrls : null);

        Professor professor = team.getProfessor();
        ProfessorInfoDTO professorInfoDTO = new ProfessorInfoDTO();
        if (professor != null) {
            professorInfoDTO.setProfessorImage(
                    professor.getImageUrl() != null ? s3Uploader.rebuildCdnUrl(professor.getImageUrl()) : ""
            );
            professorInfoDTO.setProfessorName(Optional.ofNullable(professor.getName()).orElse(""));
            professorInfoDTO.setProfessorEmail(Optional.ofNullable(professor.getEmail()).orElse(""));
            professorInfoDTO.setProfessorTenure(professor.isTenure());
        } else {
            professorInfoDTO.setProfessorName("");
            professorInfoDTO.setProfessorEmail("");
            professorInfoDTO.setProfessorTenure(false);
        }

        return new SearchPostInfoDTO(studentInfoDTOS, postTeamInfoDTO, professorInfoDTO);
    }

    @Transactional
    public void updatePostInfo(UpdateTeamPostDTO dto, String userEmail) throws Exception {

        String requestedTeamUuId = dto.getTeamUuid();
        Post post = postRepository.findByUuid(requestedTeamUuId)
                .orElseThrow(() -> new IllegalStateException("해당 UUID의 게시글을 찾을 수 없습니다."));

        Team requestedTeam = teamRepository.findByPostId(post.getId())
                .orElseThrow(() -> new IllegalStateException("해당 팀을 찾을 수 없습니다."));

        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Team userTeam = account.getTeam();
        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeam.getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        requestedTeam.setCategory(dto.getCategory());

        String path =  "teamPost/" + dto.getTeamUuid();

        if (dto.getTeamProfileImageOperation().equals(Operation.UPLOAD)) {
            post.setTeamProfileUrl(s3Uploader.rebuildCdnUrl(dto.getTeamProfileImage()));
            s3Uploader.cleanupOldVersions(path + "/teamProfile_");

        } else if (dto.getTeamProfileImageOperation().equals(Operation.DELETE)) {
            s3Uploader.delete(dto.getTeamProfileImage());
            post.setTeamProfileUrl(null);
        }

        if (dto.getDemoVideoOperation().equals(Operation.UPLOAD)) {
            post.setDemoUrl(s3Uploader.rebuildCdnUrl(dto.getDemoVideo()));
            s3Uploader.cleanupOldVersions(path + "/demo_");
        } else if (dto.getDemoVideoOperation().equals(Operation.DELETE)) {
            s3Uploader.delete(dto.getDemoVideo());
            post.setDemoUrl(null);
        }

        if (dto.getPosterImageOperation().equals(Operation.UPLOAD)) {
            post.setPosterUrl(s3Uploader.rebuildCdnUrl(dto.getPosterImage()));
            s3Uploader.cleanupOldVersions(path + "/poster_");
        } else if (dto.getPosterImageOperation().equals(Operation.DELETE)) {
            s3Uploader.delete(dto.getPosterImage());
            post.setPosterUrl(null);
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

        if (!account.getRole().equals(Role.ADMIN) && (userTeam == null || !userTeam.getId().equals(requestedTeam.getId()))) {
            throw new IllegalStateException("해당 팀의 포스트를 수정할 권한이 없습니다.");
        }

        StudentProfile studentProfile = student.getStudentProfile();
        studentProfile.setGithubUrl(dto.getGithubUrl());
        studentProfile.setStudentEmail(dto.getStudentEmail());
        studentProfile.setStudentBlog(dto.getStudentBlog());
        studentProfile.setInfo(dto.getInfo());

        String path = "studentProfileImage/" + student.getStudentNumber() + "_";
        if(dto.getProfileImageOperation().equals(Operation.UPLOAD)) {
            s3Uploader.cleanupOldVersions(path);
            studentProfile.setStudentProfileUrl(dto.getProfileImage());
        } else if (dto.getProfileImageOperation().equals(Operation.DELETE)) {
            s3Uploader.delete(dto.getProfileImage());
            studentProfile.setStudentProfileUrl(null);
        }

    }





}