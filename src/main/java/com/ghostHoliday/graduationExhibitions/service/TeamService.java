package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.post.FindPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.FindTeamInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.ResponseTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.UpdateTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.ProfessorRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.Base64Utility;
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
    private final Base64Utility base64Utility;
    private final ProfessorRepository professorRepository;

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
            findTeamPostInfoByYearDTO.setTeamName(team.getName());
            findTeamPostInfoByYearDTO.setUuid(post.getUuid());
            findTeamPostInfoByYearDTO.setTitle(post.getTitle());
            findTeamPostInfoByYearDTO.setTeamProfileImage(post != null && post.getTeamProfileUrl() != null && !post.getTeamProfileUrl().isEmpty() ? base64Utility.encodeFileToBase64(post.getTeamProfileUrl()) : null);
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
            Long professorId = encryptionService.decryptPrimaryKey(updateTeamInfoDTO.getEncryptedProfessorId());
            Team team = teamRepository.findById(teamId).get();
            Professor professor = professorRepository.findById(professorId).get();

            ResponseTeamInfoDTO teamInfo = updateTeam(team, professor, updateTeamInfoDTO.getName(), updateTeamInfoDTO.getCategory());
            teams.add(teamInfo);
        }
        return teams;
    }


    /**
     * 해당 년도 정보 조회 후 리턴
     */
    @Transactional
    public List<FindTeamInfoByYearDTO> findTeamInfoByYear(int year) throws Exception {

        List<Team> teams = teamRepository.findAllByExhibitionYear(year);
        ArrayList<FindTeamInfoByYearDTO> findTeamInfoByYearDTOs = new ArrayList<>();

        for (Team team : teams) {
            FindTeamInfoByYearDTO findTeamInfoByYearDTO = new FindTeamInfoByYearDTO();
            findTeamInfoByYearDTO.setEncryptedTeamId(encryptionService.encryptPrimaryKey(team.getId()));
            String encrptionProfessorId = null;
            if (team.getProfessor() != null) {
                encrptionProfessorId = (encryptionService.encryptPrimaryKey(team.getProfessor().getId()));
                findTeamInfoByYearDTO.setProfessor( team.getProfessor().getName());
            }
            findTeamInfoByYearDTO.setEncryptedProfessorId(encrptionProfessorId);
            findTeamInfoByYearDTO.setName(team.getName());
            findTeamInfoByYearDTO.setCategory(team.getCategory());


            findTeamInfoByYearDTOs.add(findTeamInfoByYearDTO);
        }
        return findTeamInfoByYearDTOs;
    }

    @Transactional
    public List<String> findTeamProfileImageByYear(int year){
        List<String> response = new ArrayList<>();
        List<Team> teams = teamRepository.findAllByExhibitionYear(year);
        for (Team team : teams) {
            String teamProfileUrl = team.getPost().getTeamProfileUrl();
            String image = base64Utility.encodeFileToBase64(teamProfileUrl);
            if (image != null){
                response.add(image);
            }
        }
        return response;
    }




    static ResponseTeamInfoDTO updateTeam(Team team, Professor professor, String requestedName, Category requestedCategory){
        ResponseTeamInfoDTO teamInfo = new ResponseTeamInfoDTO();

        team.setProfessor(professor);
        teamInfo.setProfessor(professor.getName());

        team.setName(requestedName);
        teamInfo.setName(requestedName);

        team.setCategory(requestedCategory);
        teamInfo.setCategory(requestedCategory);

        return teamInfo;
    }





}