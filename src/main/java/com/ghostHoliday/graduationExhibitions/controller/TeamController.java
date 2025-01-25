package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.dto.FindTeamPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("team")
public class TeamController {
    private final TeamService teamService;

    @GetMapping("/search")
    public ResponseEntity<List<FindTeamPostInfoByYearDTO>> findPostsInfoByYear(@RequestParam int year) throws Exception {
        List<FindTeamPostInfoByYearDTO> teams = teamService.findPostsInfoByYear(year);
        if (teams.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(teams);
    }

    @DeleteMapping("delete")
    public ResponseEntity<String> deleteTeam(@RequestBody List<String> encryptionTeamIds) throws Exception {
        try{
            teamService.deleteTeam(encryptionTeamIds);
            return ResponseEntity.ok("성공적으로 팀 정보를 삭제했습니다.");
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }

    }
}
