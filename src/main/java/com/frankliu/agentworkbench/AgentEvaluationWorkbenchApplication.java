package com.frankliu.agentworkbench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentEvaluationWorkbenchApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentEvaluationWorkbenchApplication.class, args);
    }
}
