package com.judge.repository;

import com.judge.model.TestingCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestingCase, Long> {

    List<TestingCase> findByProblemIdOrderByTestOrderAsc(Long problemId);
}
