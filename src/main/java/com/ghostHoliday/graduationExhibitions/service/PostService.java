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
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
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
    private final JwtUtility jwtUtility;


    public Long save(Post post) {
        return postRepository.save(post).getId();
    }

//    @Transactional
//    public void updateSlideImage(UpdateSlideImageDTO dto, List<MultipartFile> files) throws Exception {
//        String token = dto.getToken();
//        Long teamId = accountRepository.findAccountByUserEmail(jwtUtility.validateToken(token)
//                .getSubject())
//                .get()
//                .getTeam()
//                .getId();
//
//        if (!Objects.equals(teamId, encryptionService.decryptPrimaryKey(dto.getEncryptionTeamId()))){
//            throw new IllegalArgumentException("잘못된 처리입니다.");
//        }
//
//        String UploadDir = "";
//        File directory = new File()
//
//
//    }


}
