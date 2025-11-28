package com.judge.service;

import com.judge.config.RabbitMQConfig;
import com.judge.dto.SubmissionRequest;
import com.judge.model.Problem;
import com.judge.model.Submission;
import com.judge.model.User;
import com.judge.repository.ProblemRepository;
import com.judge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Submission submitSolution(SubmissionRequest request, User user) {
        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Submission submission = Submission.builder()
                .user(user)
                .problem(problem)
                .code(request.getCode())
                .language(request.getLanguage().toUpperCase())
                .status("PENDING")
                .totalTestCases(problem.getTestCases().size())
                .build();

        submission = submissionRepository.save(submission);

        // Send to RabbitMQ
        Map<String, Object> message = new HashMap<>();
        message.put("submissionId", submission.getId());
        message.put("problemId", problem.getId());
        message.put("code", request.getCode());
        message.put("language", request.getLanguage().toUpperCase());
        message.put("timeLimit", problem.getTimeLimit());
        message.put("memoryLimit", problem.getMemoryLimit());

        rabbitTemplate.convertAndSend(RabbitMQConfig.SUBMISSION_QUEUE, message);

        return submission;
    }

    public Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    public List<Submission> getUserSubmissions(Long userId) {
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    public List<Submission> getProblemSubmissions(Long problemId) {
        return submissionRepository.findByProblemIdOrderBySubmittedAtDesc(problemId);
    }
}
