package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.dto.FindTeamPostInfoByYearDTO;
import com.ghostHoliday.graduationExhibitions.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
