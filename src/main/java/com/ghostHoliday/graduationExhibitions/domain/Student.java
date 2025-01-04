package com.ghostHoliday.graduationExhibitions.domain;


import jakarta.persistence.*;
import lombok.Data;

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
    private Team team;

    private String name;
    private String studentNumber;

    @Enumerated(EnumType.STRING)
    private Role role;
    private String exhibitionYear;
}
