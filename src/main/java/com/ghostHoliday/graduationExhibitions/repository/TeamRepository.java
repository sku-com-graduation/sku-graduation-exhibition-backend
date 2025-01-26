package com.ghostHoliday.graduationExhibitions.repository;


import com.ghostHoliday.graduationExhibitions.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    public List<Team> findAllByExhibitionYear(int year);
}
