package com.judge.worker.service;

import com.judge.worker.model.Problem;
import com.judge.worker.model.Submission;
import com.judge.worker.model.TestingCase;
import com.judge.worker.repository.ProblemRepository;
import com.judge.worker.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final DockerExecutor dockerExecutor;

    @Transactional
    public void judgeSubmission(Map<String, Object> message) {
        Long submissionId = ((Number) message.get("submissionId")).longValue();
        Long problemId = ((Number) message.get("problemId")).longValue();
        String code = (String) message.get("code");
        String language = (String) message.get("language");
        Integer timeLimit = (Integer) message.get("timeLimit");

        log.info("Judging submission {} for problem {}", submissionId, problemId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));

        // Update status to judging
        submission.setStatus("JUDGING");
        submissionRepository.save(submission);

        boolean allPassed = true;
        int totalTime = 0;
        int passedTests = 0;
        String verdict = "ACCEPTED";
        String message_text = "";

        // Run through all test cases
        for (TestingCase testCase : problem.getTestCases()) {
            log.debug("Running test case {} for submission {}", testCase.getId(), submissionId);

            DockerExecutor.ExecutionResult result =
                    dockerExecutor.executeJavaCode(code, testCase.getInput(), timeLimit);

            totalTime += result.getExecutionTime();

            if (!result.getStatus().equals("SUCCESS")) {
                allPassed = false;
                verdict = result.getStatus();
                message_text = result.getOutput();
                log.info("Submission {} failed with status: {}", submissionId, verdict);
                break;
            }

            String actualOutput = result.getOutput().trim();
            String expectedOutput = testCase.getExpectedOutput().trim();

            if (!actualOutput.equals(expectedOutput)) {
                allPassed = false;
                verdict = "WRONG_ANSWER";
                message_text = String.format(
                        "Test case %d failed\nExpected:\n%s\n\nGot:\n%s",
                        testCase.getTestOrder() != null ? testCase.getTestOrder() : testCase.getId(),
                        expectedOutput,
                        actualOutput
                );
                log.info("Submission {} - Wrong answer on test {}", submissionId, testCase.getId());
                break;
            }

            passedTests++;
        }

        if (allPassed) {
            log.info("Submission {} - All tests passed!", submissionId);
            message_text = "All test cases passed!";
        }

        // Update submission with results
        submission.setStatus(verdict);
        submission.setMessage(message_text);
        submission.setExecutionTime(totalTime);
        submission.setTestCasesPassed(passedTests);
        submission.setJudgedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        log.info("Finished judging submission {}: {}", submissionId, verdict);
    }
}
