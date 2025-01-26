package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import com.ghostHoliday.graduationExhibitions.domain.Post;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.dto.FindPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.FindTeamInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.ResponseTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateTeamInfoDTO;
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
    public List<FindPostInfoByYearDTO> findPostsInfoByYear(int year) throws Exception {
        ArrayList<FindPostInfoByYearDTO> findTeamPostInfoByYearDTOS = new ArrayList<>();

        List<Team> teams = teamRepository.findAllByExhibitionYear(year);

        for (Team team : teams) {
            FindPostInfoByYearDTO findTeamPostInfoByYearDTO = new FindPostInfoByYearDTO();
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

    @Transactional
    public List<ResponseTeamInfoDTO> updateTeamInfo(List<UpdateTeamInfoDTO> updateTeamInfoDTOS) throws Exception {
        List<ResponseTeamInfoDTO> teams = new ArrayList<>();
        for (UpdateTeamInfoDTO updateTeamInfoDTO : updateTeamInfoDTOS) {
            Long teamId = encryptionService.decryptPrimaryKey(updateTeamInfoDTO.getEncryptedTeamId());
            Team team = teamRepository.findById(teamId).get();
            ResponseTeamInfoDTO teamInfo = updateTeam(team, teamId, null,
                    updateTeamInfoDTO.getName(),
                    updateTeamInfoDTO.getExhibitionYear(),
                    updateTeamInfoDTO.getCategory());
            teams.add(teamInfo);
        }
        return teams;
    }

//    public List<FindTeamInfoByYearDTO> findTeamInfoByYear(int year, String token) throws Exception {
//        ArrayList<FindTeamInfoByYearDTO> findTeamInfoByYearDTOS = new ArrayList<>();
//
//    }




    static ResponseTeamInfoDTO updateTeam(Team team, Long teamId, Long professorId, String name, int year, Category category){
        ResponseTeamInfoDTO teamInfo = new ResponseTeamInfoDTO();

        team.setProfessor(null);
        teamInfo.setProfessor(null);
        team.setName(name);
        teamInfo.setName(name);
        team.setExhibitionYear(year);
        teamInfo.setCategory(category);
        team.setCategory(category);
        teamInfo.setCategory(category);
        return teamInfo;
    }




}