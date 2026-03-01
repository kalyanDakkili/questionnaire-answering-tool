package com.almabase.questionnaire.repository;

import com.almabase.questionnaire.entity.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findByRunIdOrderByQuestionOrderAsc(Long runId);
}
