package com.valueinvestor.bot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility component to execute external Python scripts.
 * It is configurable via application properties for the Python executable path
 * and the directory containing the scripts.
 */
@Component
public class PythonExecutor {

    private static final Logger log = LoggerFactory.getLogger(PythonExecutor.class);

    @Value("${app.python.executable:python}")
    private String pythonExecutable;

    @Value("${app.python.script-path:shioaji_bridge}")
    private String scriptPath;

    /**
     * Executes a Python script with the given arguments.
     *
     * @param scriptName The name of the script file to execute (e.g., "fetch_quote.py").
     * @param args       A list of string arguments to pass to the script.
     * @return The standard output of the script as a single String.
     * @throws IOException          if an I/O error occurs or the script times out.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public String execute(String scriptName, List<String> args) throws IOException, InterruptedException {
        
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptName);
        if (args != null) {
            command.addAll(args);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(scriptPath));
        processBuilder.redirectErrorStream(true);
        
        log.info("Executing Python script: {}", String.join(" ", command));

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        boolean finished = process.waitFor(120, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            log.error("Python script '{}' timed out after 120 seconds.", scriptName);
            throw new IOException("Python script execution timed out: " + scriptName);
        }
        
        int exitCode = process.exitValue();
        log.info("Python script '{}' finished with exit code {}.", scriptName, exitCode);

        if (exitCode != 0) {
            String errorMessage = String.format(
                "Python script '%s' exited with non-zero code: %d. Output:%n%s",
                scriptName, exitCode, output
            );
            log.error(errorMessage);
            throw new IOException(errorMessage);
        }

        return output.toString();
    }
}
