package com.example.blog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@Component
@ConfigurationProperties(prefix = "blog")
@Validated
public class BlogProperties {
    @Valid
	private BlogProperties.Github github = new Github();

	public Github getGithub() {
		return github;
	}

	public void setGithub(Github github) {
		this.github = github;
	}

	public static class Github {
		@NotEmpty
		private String accessToken;
		@NotEmpty
		private String webhookSecret;

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public String getWebhookSecret() {
			return webhookSecret;
		}

		public void setWebhookSecret(String webhookSecret) {
			this.webhookSecret = webhookSecret;
		}
	}
}
