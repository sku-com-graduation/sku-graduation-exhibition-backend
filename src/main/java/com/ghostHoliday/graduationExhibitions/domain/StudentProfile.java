package com.ghostHoliday.graduationExhibitions.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class StudentProfile {

    @Id @GeneratedValue
    private Long id;

    private String studentProfileUrl;
    private String info;
    private String githubUrl;
    @Column(name= "student_email")
    private String studentEmail;
    private String studentBlog;

}
