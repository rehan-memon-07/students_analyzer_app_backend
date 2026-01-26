package com.ai.career.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class ResumeService {

    private static final String UPLOAD_DIR = "uploads";

    /**
     * This method saves the uploaded resume file to disk
     * and returns the saved file name.
     */
    public String saveResumeFile(MultipartFile file) throws IOException {

        //  Ensure uploads directory exists
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        //  Generate unique file name
        String savedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        //  Create destination file
        File destinationFile = new File(uploadDir, savedFileName);

        //  Save file to disk
        file.transferTo(destinationFile);

        return savedFileName;
    }
}
