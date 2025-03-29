package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.dto.*;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
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
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam int year) throws Exception {
        List<FindPostInfoByYearDTO> teams = teamService.findPostsInfoByYear(year);

        try{

            // 1️⃣ 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }

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

            // 1️⃣ 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            teamService.deleteTeam(encryptionTeamIds);
            return ResponseEntity.ok("성공적으로 팀 정보를 삭제했습니다.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
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
            // 1️⃣ 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            List<ResponseTeamInfoDTO> updatedTeams = teamService.updateTeamInfo(updateTeamInfoDTOS);
            return ResponseEntity.ok(updatedTeams);

        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }  catch (IllegalArgumentException e) {
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

        try{
            // 1️⃣ 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
        }catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }


        List<FindTeamInfoByYearDTO> teamInfo = teamService.findTeamInfoByYear(year);
        return ResponseEntity.ok(teamInfo);
    }

    @GetMapping("public/team/search/teamProflie")
    public ResponseEntity<?> findTeamProfileImageByYear(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestParam int year) throws Exception {

        try{
            // 1️⃣ 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }

        List<String> result = teamService.findTeamProfileImageByYear(year);
        return ResponseEntity.ok(result);
    }

}


