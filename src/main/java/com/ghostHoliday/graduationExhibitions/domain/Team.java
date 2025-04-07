package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Team {
    @Id@GeneratedValue
    @Column(name = "teamId")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "postId")
    private Post post;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "ProfessorId")
    private Professor professor;

    private String name;

    private int exhibitionYear;

    @Enumerated(EnumType.STRING)
    private Category category;
}
