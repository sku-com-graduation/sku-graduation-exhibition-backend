package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Home {

    @Id @GeneratedValue
    @Column(name = "homeId")
    private Long id;

    private String exhibitionYear;

    private String exhibitionDate;

    private String exhibitionHour;
}
