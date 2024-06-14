package com.example.code.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.code.dao.IDaiDao;
import com.example.code.po.Dai;
import com.example.code.service.IDaiService;
import com.example.code.util.Constants;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class DaiServiceImpl implements IDaiService {
    private static Logger logger = LoggerFactory.getLogger(DaiServiceImpl.class);

    @Resource
    private IDaiDao daiDao;
    private final DockerClient dockerClient;

    public DaiServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public Dai getCode(Integer id) {
        return daiDao.selectById(id);
    }

    @Override
    public String updateCodeAndrun(Map<String, Object> request) {
        String code = (String) request.get("code");
        String type = (String) request.get("language");
        Integer id = (Integer) request.get("userId");
        String inputData = (String) request.get("inputData");
        inputData = escapeDoubleQuotes(inputData);
        Integer language = getByType(type);

        // 运行代码并获取结果
        String result = runCodeInDocker(code, type, inputData);

        // 更新数据库
        Dai dai = new Dai();
        dai.setId(id);
        dai.setLanguage(language);
        dai.setCode(code);
        daiDao.updateById(dai);

        return result;
    }

    private String runCodeInDocker(String code, String language, String input) {
        String imageName = getDockerImageName(language);
        String containerId = null;
        try {
            logger.info("Creating and starting Docker container...");
            containerId = createAndStartContainer(imageName, code, language, input);
            logger.info("Container started with ID: {}", containerId);
            String output = getContainerOutput(containerId);
            logger.info("Container output: {}", output);
            return parseOutput(output);
        } catch (Exception e) {
            logger.error("Error running code in Docker", e);
            return "Error running code in Docker: " + e.getMessage();
        } finally {
            if (containerId != null) {
                cleanUpContainer(containerId);
            }
        }
    }

    private String createAndStartContainer(String imageName, String code, String language, String input) throws IOException {
        String inputFile = "input.txt";
        String inputDataCmd = "echo \"" + escapeDoubleQuotes(input) + "\" > " + inputFile;

        String command = inputDataCmd + " && " + getCommand(language, code, inputFile);

        logger.info("Creating container with image: {} and command: {}", imageName, command);

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withCmd("sh", "-c", command)
                .withTty(true)
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }

    private String getContainerOutput(String containerId) {
        StringBuilder output = new StringBuilder();
        try {
            // 等待一段时间以确保容器有时间生成输出
            Thread.sleep(5000); // 等待 5 秒，你可以根据需要调整这个时间

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(new LogContainerResultCallback() {
                        @Override
                        public void onNext(Frame item) {
                            output.append(new String(item.getPayload()));
                        }
                    }).awaitCompletion();

            logger.info("Container logs retrieved for ID: {}", containerId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread was interrupted", e);
            return "Thread was interrupted: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Error retrieving container output", e);
            return "Error retrieving container output: " + e.getMessage();
        }

        return output.toString();
    }

    private void cleanUpContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            logger.info("Container stopped with ID: {}", containerId);
        } catch (NotModifiedException e) {
            logger.info("Container {} is already stopped.", containerId);
        } catch (Exception e) {
            logger.error("Error stopping container {}", containerId, e);
        }

        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            logger.info("Container removed with ID: {}", containerId);
        } catch (Exception e) {
            logger.error("Error removing container {}", containerId, e);
        }
    }

    private String getCommand(String language, String code, String inputFile) {
        switch (language) {
            case "java":
                return "echo \"" + escapeDoubleQuotes(code) + "\" > Main.java && javac Main.java && java Main < " + inputFile;
            case "cpp":
                return "echo \"" + escapeDoubleQuotes(code) + "\" > main.cpp && g++ main.cpp -o main && ./main < " + inputFile;
            case "python":
                return "echo \"" + escapeDoubleQuotes(code) + "\" > main.py && python main.py < " + inputFile;
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private String escapeDoubleQuotes(String code) {
        return code.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String getDockerImageName(String language) {
        switch (language) {
            case "java":
                return "openjdk:latest";
            case "cpp":
                return "gcc:latest";
            case "python":
                return "python:latest";
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    public Integer getByType(String type) {
        switch (type) {
            case "java":
                return Constants.Language.JAVA.getCode();
            case "cpp":
                return Constants.Language.Cpp.getCode();
            case "python":
                return Constants.Language.Python.getCode();
            case "others":
                return Constants.Language.Others.getCode();
            default:
                throw new RuntimeException("Unsupported language type");
        }
    }

    private String parseOutput(String output) {
        String[] lines = output.split("\n");
        return lines[lines.length - 1].trim();
    }
}
