package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Blob;
import java.sql.Timestamp;
import java.time.LocalTime;

@Data
@Entity
public class Account {

    @Id@GeneratedValue
    @Column(name = "accountId")
    private Long id;


    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "teamId")
    private Team team;

    private String userEmail;

    private String defaultPwd;

    private String pwd;

    private LocalTime recent;
}
