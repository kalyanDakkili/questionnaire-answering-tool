package com.almabase.questionnaire.repository;

import com.almabase.questionnaire.entity.QuestionnaireRun;
import com.almabase.questionnaire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuestionnaireRunRepository extends JpaRepository<QuestionnaireRun, Long> {
    List<QuestionnaireRun> findByUserOrderByCreatedAtDesc(User user);
    Optional<QuestionnaireRun> findByIdAndUser(Long id, User user);
}
