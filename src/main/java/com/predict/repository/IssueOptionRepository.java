package com.predict.repository;

import com.predict.IssueOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueOptionRepository extends JpaRepository<IssueOption, Long> {

    List<IssueOption> findByIssueIdOrderByDisplayOrderAsc(Long issueId);
}
