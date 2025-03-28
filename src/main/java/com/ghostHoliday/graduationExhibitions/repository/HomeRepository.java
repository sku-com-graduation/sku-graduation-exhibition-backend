package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Home;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeRepository extends JpaRepository<Home, Long> {
    Optional<Home> findByExhibitionYear(int year);
}
