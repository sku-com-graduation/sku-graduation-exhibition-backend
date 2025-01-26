package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Post {

    @Id@GeneratedValue
    @Column(name = "postId")
    private Long id;

    private String title;

    @Column(nullable = false, unique = true)
    private String uuid;

    public Post(){
        this.uuid = UUID.randomUUID().toString();
    }

    private String content;

    private String slideUrl;

    private String posterUrl;

    private String demoUrl;

    private String teamProfileUrl;


}
