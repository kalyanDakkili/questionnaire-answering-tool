package com.almabase.questionnaire.repository;

import com.almabase.questionnaire.entity.ReferenceDocument;
import com.almabase.questionnaire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ReferenceDocumentRepository extends JpaRepository<ReferenceDocument, Long> {
    List<ReferenceDocument> findByUser(User user);
    List<ReferenceDocument> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReferenceDocument r WHERE r.id = :id AND r.user.id = :userId")
    void deleteByIdAndUserId(Long id, Long userId);
}