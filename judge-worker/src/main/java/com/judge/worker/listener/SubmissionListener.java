package com.judge.worker.listener;

import com.judge.worker.config.RabbitMQConfig;
import com.judge.worker.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionListener {

    private final JudgeService judgeService;

    @RabbitListener(queues = RabbitMQConfig.SUBMISSION_QUEUE)
    public void handleSubmission(Map<String, Object> message) {
        log.info("Received submission message: {}", message.get("submissionId"));

        try {
            judgeService.judgeSubmission(message);
        } catch (Exception e) {
            log.error("Error judging submission: {}", message.get("submissionId"), e);
        }
    }
}
