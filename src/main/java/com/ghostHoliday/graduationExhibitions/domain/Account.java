package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Blob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
public class Account {

    @Id@GeneratedValue
    @Column(name = "accountId")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "teamId", nullable = true)
    private Team team;

    @Column(unique = true, nullable = false)
    private String userEmail;

    private String defaultPwd;

    private String pwd;

    private LocalDateTime recent;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;
}
