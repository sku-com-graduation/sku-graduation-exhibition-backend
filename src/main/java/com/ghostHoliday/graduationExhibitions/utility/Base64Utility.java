package com.ghostHoliday.graduationExhibitions.utility;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

@Service
public class Base64Utility {

    public String encodeFileToBase64(String filePath) {
        File imageFile = new File(filePath);
        if (!imageFile.exists() || imageFile.isDirectory()) {
            return null;
        }

        try (FileInputStream fileInputStream = new FileInputStream(imageFile)) {
            byte[] imageBytes = fileInputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            return null;
        }
    }


    public List<String> ImagesToBase64(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }

        try (Stream<Path> paths = Files.list(Paths.get(folderPath))) {
            List<String> images = new ArrayList<>();
            paths.filter(Files::isRegularFile) // 파일만 선택 (디렉토리 제외)
                    .forEach(path -> {
                        String base64 = encodeFileToBase64(String.valueOf(path));
                        if (base64 != null) {
                            images.add(base64);
                        }
                    });
            return images.isEmpty() ? null : images;
        } catch (IOException e) {
            return null;
        }
    }
}
