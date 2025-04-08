package com.ghostHoliday.graduationExhibitions.service;


import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import com.ghostHoliday.graduationExhibitions.dto.studentProfile.UpdateStudentProfileDTO;
import com.ghostHoliday.graduationExhibitions.repository.StudentProfileRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final FileUtility fileUtility;

    @Transactional
    public StudentProfile saveStudentProfile(){
        StudentProfile studentProfile = studentProfileRepository.save(new StudentProfile());
        return studentProfile;
    }



}
