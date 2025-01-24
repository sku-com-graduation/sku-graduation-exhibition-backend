package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
}
