package com.example.blog.webhook;

import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;
import com.example.blog.BlogProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {
	private final EntryFetcher entryFetcher;
	private final ApplicationEventPublisher publisher;
	private final WebhookVerifier webhookVerifier;
	private final ObjectMapper objectMapper;

	public WebhookController(BlogProperties props, EntryFetcher entryFetcher,
			ApplicationEventPublisher publisher, ObjectMapper objectMapper)
			throws NoSuchAlgorithmException, InvalidKeyException {
		this.entryFetcher = entryFetcher;
		this.publisher = publisher;
		this.objectMapper = objectMapper;
		this.webhookVerifier = new WebhookVerifier(props.getGithub().getWebhookSecret());
	}

	@PostMapping("webhook")
	public Flux<?> webhook(@RequestBody String payload,
			@RequestHeader("X-Hub-Signature") String signature) throws Exception {
		this.webhookVerifier.verify(payload, signature);
		JsonNode node = this.objectMapper.readValue(payload, JsonNode.class);
		String[] repository = node.get("repository").get("full_name").asText().split("/");
		String owner = repository[0];
		String repo = repository[1];
		Stream<JsonNode> commits = StreamSupport.stream(node.get("commits").spliterator(),
				false);
		// TODO: 演習2
		return Flux.fromStream(commits).flatMap(commit -> {
			Flux<EntryId> added = this.paths(commit.get("added"))
					.flatMap(path -> this.entryFetcher.fetch(owner, repo, path)) //
					.map(Entry::entryId);
			Flux<EntryId> modified = this.paths(commit.get("modified")) //
					.flatMap(path -> this.entryFetcher.fetch(owner, repo, path)) //
					.map(Entry::entryId);
			Flux<EntryId> removed = this.paths(commit.get("removed")) //
					.map(path -> EntryId.fromFilePath(Paths.get(path)));
			return added.map(id -> Collections.singletonMap("added", id.getValue())) //
					.mergeWith(modified.map(
							id -> Collections.singletonMap("modified", id.getValue()))) //
					.mergeWith(removed.map(
							id -> Collections.singletonMap("removed", id.getValue())));
		});
	}

	Flux<String> paths(JsonNode paths) {
		return Flux.fromStream(
				StreamSupport.stream(paths.spliterator(), false).map(JsonNode::asText));
	}
}
