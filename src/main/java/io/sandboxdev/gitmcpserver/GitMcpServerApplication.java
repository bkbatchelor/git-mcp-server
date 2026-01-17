package io.sandboxdev.gitmcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GitMcpServerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GitMcpServerApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }
}

