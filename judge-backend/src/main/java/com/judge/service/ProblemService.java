package com.judge.service;

import com.judge.model.Problem;
import com.judge.model.TestingCase;
import com.judge.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public List<Problem> getAllProblems() {
        return problemRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Problem getProblemById(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));
    }

    @Transactional
    public Problem createProblem(Problem problem) {
        // Set bidirectional relationship
        if (problem.getTestCases() != null) {
            for (TestingCase testCase : problem.getTestCases()) {
                testCase.setProblem(problem);
            }
        }
        return problemRepository.save(problem);
    }

    @Transactional
    public Problem updateProblem(Long id, Problem problemDetails) {
        Problem problem = getProblemById(id);

        problem.setTitle(problemDetails.getTitle());
        problem.setDescription(problemDetails.getDescription());
        problem.setInputFormat(problemDetails.getInputFormat());
        problem.setOutputFormat(problemDetails.getOutputFormat());
        problem.setTimeLimit(problemDetails.getTimeLimit());
        problem.setMemoryLimit(problemDetails.getMemoryLimit());
        problem.setDifficulty(problemDetails.getDifficulty());

        return problemRepository.save(problem);
    }

    @Transactional
    public void deleteProblem(Long id) {
        Problem problem = getProblemById(id);
        problem.setIsActive(false);
        problemRepository.save(problem);
    }
}
