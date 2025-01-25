package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.dto.UpdatePostByTeamDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateSlideImageDTO;
import com.ghostHoliday.graduationExhibitions.repository.PostRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final TeamRepository teamRepository;

    public Long save(Post post) {
        return postRepository.save(post).getId();
    }

//    public void updatePostByTeam(UpdatePostByTeamDTO dto) {
//        String token = dto.getToken();
//
//        Long teamId = 1l; // 토큰 암호화 해독 코드 필요
//        Post post = findPostByTeam(teamId);
//        Team team = teamRepository.findById(teamId).get();
//        post.setTitle(dto.getTitle());
//        post.setContent(dto.getContent());
//        post.setTeamProfileUrl(dto.getTeamProfileUrl());
//        post.setDemoUrl(dto.getDemoUrl());
//
//        team.setCategory(dto.getCategory());
//
//        postRepository.save(post);
//        teamRepository.save(team);
//    }
}
