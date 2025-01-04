package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Post {

    @Id@GeneratedValue
    @Column(name = "postId")
    private Long id;

    private String title;

    private String content;

    private String slide_url;

    private String poster_url;

    private String demo_url;

    private String team_profile_url;


}
