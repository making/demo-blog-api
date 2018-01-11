package com.example.blog;

import am.ik.github.AccessToken;
import am.ik.github.GitHubClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class DemoBlogApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoBlogApiApplication.class, args);
	}

	@Bean
	GitHubClient gitHubClient(BlogProperties props, WebClient.Builder builder) {
		return new GitHubClient(builder,
				new AccessToken(props.getGithub().getAccessToken()));
	}

	@Bean
	ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(4);
		taskExecutor.setQueueCapacity(25);
		taskExecutor.setMaxPoolSize(40);
		return taskExecutor;
	}
}
