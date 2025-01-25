package com.ghostHoliday.graduationExhibitions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class GraduationExhibitionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GraduationExhibitionsApplication.class, args);
	}

}
