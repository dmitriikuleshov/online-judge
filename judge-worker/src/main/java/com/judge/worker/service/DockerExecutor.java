package com.judge.worker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DockerExecutor {

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        log.info("Docker client initialized");
    }

    public ExecutionResult executeJavaCode(String code, String input, int timeLimit) {
        String workDir = "/tmp/judge-" + UUID.randomUUID();
        Path workDirPath = Path.of(workDir);

        try {
            Files.createDirectories(workDirPath);

            // Write Java code
            File javaFile = workDirPath.resolve("Solution.java").toFile();
            try (FileWriter writer = new FileWriter(javaFile)) {
                writer.write(code);
            }

            // Write input
            File inputFile = workDirPath.resolve("input.txt").toFile();
            try (FileWriter writer = new FileWriter(inputFile)) {
                writer.write(input != null ? input : "");
            }

            // Create container
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(workDir, new Volume("/workspace")))
                    .withMemory(256L * 1024 * 1024) // 256 MB
                    .withMemorySwap(256L * 1024 * 1024)
                    .withCpuQuota(50000L)
                    .withNetworkMode("none")
                    .withReadonlyRootfs(false);

            String command = String.format(
                    "cd /workspace && " +
                            "javac Solution.java 2>&1 && " +
                            "timeout %ds java Solution < input.txt 2>&1",
                    (timeLimit / 1000) + 1
            );

            CreateContainerResponse container = dockerClient.createContainerCmd("eclipse-temurin:21-jre-alpine")
                    .withHostConfig(hostConfig)
                    .withCmd("/bin/sh", "-c", command)
                    .withWorkingDir("/workspace")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            String containerId = container.getId();
            log.debug("Created container: {}", containerId);

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Wait for completion with timeout
            long startTime = System.currentTimeMillis();
            Integer statusCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeLimit + 5000, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            // Get logs
            StringBuilder output = new StringBuilder();
            try {
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                output.append(new String(frame.getPayload()));
                            }
                        })
                        .awaitCompletion(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while reading logs", e);
            }

            // Cleanup container
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception e) {
                log.error("Error removing container", e);
            }

            // Cleanup directory
            deleteDirectory(workDirPath);

            ExecutionResult result = new ExecutionResult();
            result.setOutput(output.toString().trim());
            result.setExecutionTime((int) executionTime);
            result.setExitCode(statusCode != null ? statusCode : -1);

            if (executionTime > timeLimit) {
                result.setStatus("TIME_LIMIT_EXCEEDED");
            } else if (statusCode == null) {
                result.setStatus("TIME_LIMIT_EXCEEDED");
            } else if (statusCode == 124) { // timeout exit code
                result.setStatus("TIME_LIMIT_EXCEEDED");
            } else if (statusCode != 0) {
                if (output.toString().contains("error:") || output.toString().contains("Exception")) {
                    result.setStatus("COMPILATION_ERROR");
                } else {
                    result.setStatus("RUNTIME_ERROR");
                }
            } else {
                result.setStatus("SUCCESS");
            }

            return result;

        } catch (Exception e) {
            log.error("Error executing code", e);
            ExecutionResult result = new ExecutionResult();
            result.setStatus("SYSTEM_ERROR");
            result.setOutput("System error: " + e.getMessage());
            result.setExitCode(-1);
            return result;
        }
    }

    private void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            log.error("Error deleting directory: {}", directory, e);
        }
    }

    public static class ExecutionResult {
        private String output;
        private int executionTime;
        private int exitCode;
        private String status;

        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public int getExecutionTime() { return executionTime; }
        public void setExecutionTime(int executionTime) { this.executionTime = executionTime; }
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
