package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    private final TeamRepository teamRepository;

    public Long save(Team team){
        return teamRepository.save(team).getId();
    }

    public Team findById(Long teamId) {
        return teamRepository.findById(teamId).get();
    }

}