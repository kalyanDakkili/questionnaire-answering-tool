package com.almabase.questionnaire.service;

import com.almabase.questionnaire.entity.ReferenceDocument;
import com.almabase.questionnaire.entity.User;
import com.almabase.questionnaire.repository.ReferenceDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ReferenceDocumentService {

    @Autowired
    private ReferenceDocumentRepository documentRepository;

    @Autowired
    private DocumentParserService parserService;

    public ReferenceDocument upload(MultipartFile file, User user) throws IOException {
        String content = parserService.extractText(file);
        ReferenceDocument doc = new ReferenceDocument();
        doc.setOriginalFilename(file.getOriginalFilename());
        doc.setFilename(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setContent(content);
        doc.setUser(user);
        return documentRepository.save(doc);
    }

    public List<ReferenceDocument> findByUser(User user) {
        return documentRepository.findByUser(user);
    }

    public void delete(Long id, Long userId) {
        documentRepository.deleteByIdAndUserId(id, userId);
    }
}
