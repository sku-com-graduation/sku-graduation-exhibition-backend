package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.dto.FindPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.FindTeamInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.ResponseTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.service.AccountService;
import com.ghostHoliday.graduationExhibitions.service.TeamService;
import com.ghostHoliday.graduationExhibitions.service.TokenService;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;


    @GetMapping("team/search/teamPost")
    public ResponseEntity<List<FindPostInfoByYearDTO>> findPostsInfoByYear(@RequestParam int year) throws Exception {
        List<FindPostInfoByYearDTO> teams = teamService.findPostsInfoByYear(year);
        if (teams.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(teams);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @DeleteMapping("admin/team/delete")
    public ResponseEntity<String> deleteTeam(@RequestBody List<String> encryptionTeamIds) throws Exception {
        try {
            teamService.deleteTeam(encryptionTeamIds);
            return ResponseEntity.ok("성공적으로 팀 정보를 삭제했습니다.");
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PatchMapping("admin/team/update")
    public ResponseEntity<List<ResponseTeamInfoDTO>> updateTeamInfo(@RequestBody List<UpdateTeamInfoDTO> updateTeamInfoDTOS) throws Exception {

        try {
            List<ResponseTeamInfoDTO> updatedTeams = teamService.updateTeamInfo(updateTeamInfoDTOS);
            return ResponseEntity.ok(updatedTeams);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Error", e.getMessage())
                    .build();
        } catch (EntityNotFoundException e) {
            // 업데이트하려는 팀이 존재하지 않을 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Error", e.getMessage())
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(e.getMessage())
                    .build();
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping("admin/team/search/teamInfo")
    public ResponseEntity<FindTeamInfoByYearDTO> findTeamInfoByYear(@RequestParam int year) throws Exception {

        FindTeamInfoByYearDTO teamInfo = teamService.findTeamInfoByYear(year);
        return ResponseEntity.ok(teamInfo);
    }

}


