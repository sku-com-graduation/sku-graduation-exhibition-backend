package com.ghostHoliday.graduationExhibitions.domain;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class Student {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name= "studentProfileId")
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name= "teamId")
    private Team team = null;

    @Column(name = "name", columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String name;
    private String studentNumber;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;

    // exhibitionYear 필드의 기본값을 시스템의 현재 연도로 설정
    private String exhibitionYear = String.valueOf(LocalDate.now().getYear());
}
