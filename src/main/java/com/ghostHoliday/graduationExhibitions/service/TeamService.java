package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.*;
import com.ghostHoliday.graduationExhibitions.dto.post.FindPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.*;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.ProfessorRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
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
    private final StudentRepository studentRepository;

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

            // teamId를 참조하는 학생들의 team을 null로 설정
            List<Student> students = studentRepository.findAllByTeamId(teamId);  // 팀 ID로 학생 찾기
            for (Student student : students) {
                student.setTeam(null);  // 해당 학생의 team을 null로 설정
                studentRepository.save(student);  // 학생 정보 저장
            }


            accountRepository.deleteByTeamId(teamId);
            teamRepository.deleteById(teamId);
        }
    }

    @Transactional
    public List<ResponseTeamInfoDTO> updateTeamInfo(List<UpdateTeamInfoDTO> updateTeamInfoDTOS) throws Exception {
        List<ResponseTeamInfoDTO> teams = new ArrayList<>();

        for (UpdateTeamInfoDTO updateTeamInfoDTO : updateTeamInfoDTOS) {
            Team team = null;
            Professor professor = null;

            if (updateTeamInfoDTO.getEncryptedTeamId() != null) {
                Long teamId = encryptionService.decryptPrimaryKey(updateTeamInfoDTO.getEncryptedTeamId());
                team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다.")); // 안전하게 조회
            }

            if (updateTeamInfoDTO.getEncryptedProfessorId() != null) {
                Long professorId = encryptionService.decryptDeterministic(updateTeamInfoDTO.getEncryptedProfessorId());
                professor = professorRepository.findById(professorId).orElse(null); // 안전하게 조회
            }

            // 💡 무조건 updateTeam 호출 (null을 인자로 넘겨도 괜찮도록)
            ResponseTeamInfoDTO teamInfo = updateTeam(
                    team,
                    professor,
                    updateTeamInfoDTO.getName(),
                    updateTeamInfoDTO.getCategory()
            );
            teams.add(teamInfo);
        }

        return teams;
    }



    /**
     * 해당 년도 정보 조회 후 리턴
     */
    @Transactional
    public FindTeamInfoByYearDTO findTeamInfoByYear(int year) throws Exception {

        List<Team> teams = teamRepository.findAllByExhibitionYear(year);
        List<FindTeamInfoByYearTeamsDTO> requestedTeams = new ArrayList<>();
        for (Team team : teams) {
            FindTeamInfoByYearTeamsDTO requestedTeam = new FindTeamInfoByYearTeamsDTO();
            requestedTeam.setEncryptedTeamId(encryptionService.encryptPrimaryKey(team.getId()));
            String encrptionProfessorId = null;
            if (team.getProfessor() != null) {
                encrptionProfessorId = (encryptionService.encryptDeterministic(team.getProfessor().getId()));
                requestedTeam.setProfessor( team.getProfessor().getName());
            }
            requestedTeam.setEncryptedProfessorId(encrptionProfessorId);
            requestedTeam.setName(team.getName());
            requestedTeam.setCategory(team.getCategory());

            requestedTeams.add(requestedTeam);
        }

        List<FindTeamInfoByYearProfessorsDTO> requestedProfessors = new ArrayList<>();
        for (Professor professor : professorRepository.findAll()) {
            FindTeamInfoByYearProfessorsDTO requestedProfessor = new FindTeamInfoByYearProfessorsDTO();
            requestedProfessor.setEncryptedProfessorId(encryptionService.encryptDeterministic(professor.getId()));
            requestedProfessor.setProfessorName(professor.getName());
            requestedProfessors.add(requestedProfessor);
        }


        return new FindTeamInfoByYearDTO(requestedTeams, requestedProfessors);
    }

    @Transactional
    public List<String> findTeamProfileImageByYear(int year){
        List<String> response = new ArrayList<>();
        List<Team> teams = teamRepository.findAllByExhibitionYear(year);
        for (Team team : teams) {
            String teamProfileUrl = team.getPost().getTeamProfileUrl();
            if (teamProfileUrl != null){
                response.add(teamProfileUrl);
            }
        }
        return response;
    }




    static ResponseTeamInfoDTO updateTeam(Team team, Professor professor, String requestedName, Category requestedCategory) {
        ResponseTeamInfoDTO teamInfo = new ResponseTeamInfoDTO();

        if (team != null) {
            // 교수 설정 (있으면 연결, 없으면 해제)
            team.setProfessor(professor);  // professor == null이면 연결 끊김
            teamInfo.setProfessor(professor != null ? professor.getName() : null);

            // 이름 설정 (null이면 이름 제거)
            team.setName(requestedName);
            teamInfo.setName(requestedName);

            // 카테고리 설정 (null이면 제거)
            team.setCategory(requestedCategory);
            teamInfo.setCategory(requestedCategory);
        }

        return teamInfo;
    }





}