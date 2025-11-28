package com.judge.controller;

import com.judge.dto.SubmissionRequest;
import com.judge.model.Submission;
import com.judge.model.User;
import com.judge.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<Submission> submit(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(submissionService.submitSolution(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmission(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmission(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Submission>> getUserSubmissions(@PathVariable Long userId) {
        return ResponseEntity.ok(submissionService.getUserSubmissions(userId));
    }

    @GetMapping("/problem/{problemId}")
    public ResponseEntity<List<Submission>> getProblemSubmissions(@PathVariable Long problemId) {
        return ResponseEntity.ok(submissionService.getProblemSubmissions(problemId));
    }
}
