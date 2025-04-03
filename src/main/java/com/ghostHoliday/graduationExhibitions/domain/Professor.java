package com.ghostHoliday.graduationExhibitions.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Professor {

    @Id @GeneratedValue
    @Column(name = "professor_id")
    private Long id;

    private String name;
    private String email;
    private boolean tenure;
    private String imageUrl;


}
