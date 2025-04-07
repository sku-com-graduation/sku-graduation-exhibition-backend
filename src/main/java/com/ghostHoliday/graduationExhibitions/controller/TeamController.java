package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.dto.post.FindPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.FindTeamInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.FindTeamInfoByYearTeamsDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.ResponseTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.dto.team.UpdateTeamInfoDTO;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.service.TeamService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;
    private final HttpOnlyService httpOnlyService;


    @GetMapping("public/team/search/teamPost")
    public ResponseEntity<?> findPostsInfoByYear(
            @RequestParam int year) throws Exception {
        List<FindPostInfoByYearDTO> teams = teamService.findPostsInfoByYear(year);

        if (teams.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(teams);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @DeleteMapping("admin/team/delete")
    public ResponseEntity<String> deleteTeam(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody List<String> encryptionTeamIds) throws Exception {
        try {

            teamService.deleteTeam(encryptionTeamIds);
            return ResponseEntity.ok("성공적으로 팀 정보를 삭제했습니다.");
        }catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PutMapping("admin/team/update")
    public ResponseEntity<?> updateTeamInfo(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody List<UpdateTeamInfoDTO> updateTeamInfoDTOS) throws Exception {

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
    public ResponseEntity<?> findTeamInfoByYear(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestParam int year) throws Exception {

        FindTeamInfoByYearDTO teamInfo = teamService.findTeamInfoByYear(year);
        return ResponseEntity.ok(teamInfo);
    }

    @GetMapping("public/team/search/teamProflie")
    public ResponseEntity<?> findTeamProfileImageByYear(
            @RequestParam int year) throws Exception {

        List<String> result = teamService.findTeamProfileImageByYear(year);
        return ResponseEntity.ok(result);
    }

}


