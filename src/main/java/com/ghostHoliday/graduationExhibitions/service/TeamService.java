package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.dto.FindTeamPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    private final TeamRepository teamRepository;
    private final EncryptionService encryptionService;
    private final AccountRepository accountRepository;

    public Long save(Team team){
        return teamRepository.save(team).getId();
    }

    public Team findById(Long teamId) {
        return teamRepository.findById(teamId).get();
    }
    @Transactional
    public List<FindTeamPostInfoByYearDTO> findPostsInfoByYear(int year) throws Exception {
        ArrayList<FindTeamPostInfoByYearDTO> findTeamPostInfoByYearDTOS = new ArrayList<>();

        List<Team> teams = teamRepository.findAllByExhibitionYear(year);

        for (Team team : teams) {
            FindTeamPostInfoByYearDTO findTeamPostInfoByYearDTO = new FindTeamPostInfoByYearDTO();
            Post post = team.getPost();
            // PK 암호화 후 저장
            String encryptedPrimaryKey = encryptionService.encryptPrimaryKey(team.getId());
            findTeamPostInfoByYearDTO.setTeamName(team.getName());
            findTeamPostInfoByYearDTO.setTeamId(encryptedPrimaryKey);
            findTeamPostInfoByYearDTO.setTitle(post.getTitle());
            findTeamPostInfoByYearDTO.setTeamProfileImage(null);
            findTeamPostInfoByYearDTO.setCategory(team.getCategory());

            findTeamPostInfoByYearDTOS.add(findTeamPostInfoByYearDTO);
        }
        return findTeamPostInfoByYearDTOS;

    }

    @Transactional
    public void deleteTeam(List<String> encryptionTeamIds) throws Exception {

        for (String encryptionTeamId : encryptionTeamIds) {
            Long teamId = encryptionService.decryptPrimaryKey(encryptionTeamId);
            accountRepository.deleteByTeamId(teamId);
            teamRepository.deleteById(teamId);


        }
    }






}