package com.cameronleger.neuralstylegui;

import com.cameronleger.neuralstyle.Image;
import com.cameronleger.neuralstyle.NeuralStyle;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

class NeuralService extends Service {
    private static final Logger log = Logger.getLogger(NeuralService.class.getName());
    private NeuralStyle neuralStyle;

    NeuralService() {
        // only allow one of these to run at a time
//        setExecutor(Executors.newSingleThreadExecutor(Thread::new));
    }

    NeuralStyle getNeuralStyle() {
        return neuralStyle;
    }

    void setNeuralStyle(NeuralStyle neuralStyle) {
        this.neuralStyle = neuralStyle;
    }

    void addLogHandler(Handler handler) {
        log.addHandler(handler);
    }

    @Override
    protected Task<Image> createTask() {
        log.log(Level.FINE, "Getting neural style for task.");
        final NeuralStyle neuralStyleForTask = getNeuralStyle();

        log.log(Level.FINE, "Checking that style is valid.");
        if (neuralStyleForTask == null)
            return null;
        log.log(Level.FINE, "Checking that style can be run with arguments.");
        if (!neuralStyleForTask.checkArguments())
            return null;

        log.log(Level.FINE, "Generating run command.");
        final String[] buildCommand = neuralStyleForTask.buildCommand();
        for (String buildCommandPart : buildCommand)
            log.log(Level.FINE, buildCommandPart);

        return new Task<Image>() {
            @Override protected Image call() throws InterruptedException {
                updateMessage("Starting neural-style.");
                log.log(Level.FINE, "Starting neural-style process.");

                int exitCode = -1;
                String line;
                ProcessBuilder builder = new ProcessBuilder(buildCommand);
                builder.directory(NeuralStyle.getNeuralStylePath());
                builder.redirectErrorStream(true);

                try {
                    Process p = builder.start();
                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    log.log(Level.FINE, "Gathering input.");
                    try {
                        while ((line = input.readLine()) != null) {
                            log.log(Level.INFO, line);
                            if (isCancelled()) {
                                p.destroy();
                                return null;
                            }
                        }
                        input.close();
                    } catch (IOException e) {
                        log.log(Level.SEVERE, e.toString(), e);
                    }

                    exitCode = p.waitFor();
                    log.log(Level.FINE, "Neural-style process exit code: " + String.valueOf(exitCode));
                } catch (Exception e) {
                    log.log(Level.SEVERE, e.toString(), e);
                }

                if (exitCode != 1)
                    throw new RuntimeException("Exit Code: " + String.valueOf(exitCode));
                return null;

//                updateProgress(0, 10);
//                for (int i = 0; i < 10; i++) {
//                    if (isCancelled())
//                        break;
//                    Thread.sleep(300);
//                    updateProgress(i + 1, 10);
//                }
//                return null;
            }

            @Override protected void succeeded() {
                super.succeeded();
                updateMessage("Success!");
            }

            @Override protected void cancelled() {
                super.cancelled();
                updateMessage("Cancelled!");
            }

            @Override protected void failed() {
                super.failed();
                updateMessage("Failed!");
            }
        };
    }
}