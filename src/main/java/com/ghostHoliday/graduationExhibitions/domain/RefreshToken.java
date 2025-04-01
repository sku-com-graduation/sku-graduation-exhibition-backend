package com.ghostHoliday.graduationExhibitions.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accountId")
    private Account account;

    private String refreshToken;
    private LocalDateTime expiryDate;


}
