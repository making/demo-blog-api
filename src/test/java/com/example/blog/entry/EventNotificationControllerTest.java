package com.example.blog.entry;

import com.example.blog.entry.event.EntryCreateEvent;
import com.example.blog.entry.event.EntryDeleteEvent;
import com.example.blog.entry.event.EntryUpdateEvent;
import com.example.blog.entry.event.EventNotifyer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.flyway.enabled=false", "blog.github.access-token=foo",
		"blog.github.webhook-secret=bar" })
@ActiveProfiles("test")
public class EventNotificationControllerTest {
	@LocalServerPort
	int port;
	@Autowired
	EventNotifyer eventNotifyer;
	WebTestClient webClient;

	@Before
	public void setup() {
		this.webClient = WebTestClient.bindToServer() //
				.baseUrl("http://localhost:" + port) //
				.build();
	}

	@Test
	public void notification() throws Exception {
		Flux<JsonNode> body1 = this.webClient.get() //
				.uri("/v1/notification") //
				.exchange() //
				.expectStatus().isOk() //
				.expectHeader().contentType(new MediaType(TEXT_EVENT_STREAM, UTF_8)) //
				.returnResult(JsonNode.class) //
				.getResponseBody();

		this.eventNotifyer.notify(new EntryCreateEvent(null));

		Flux<JsonNode> body2 = this.webClient.get() //
				.uri("/v1/notification") //
				.exchange() //
				.expectStatus().isOk() //
				.expectHeader().contentType(new MediaType(TEXT_EVENT_STREAM, UTF_8)) //
				.returnResult(JsonNode.class) //
				.getResponseBody();

		StepVerifier.create(body1) //
				.assertNext(e -> assertThat(e.get("type").asText()).isEqualTo("CREATE"))
				.then(() -> this.eventNotifyer.notify(new EntryUpdateEvent(null)))
				.assertNext(e -> assertThat(e.get("type").asText()).isEqualTo("UPDATE"))
				.then(() -> this.eventNotifyer.notify(new EntryDeleteEvent(null)))
				.assertNext(e -> assertThat(e.get("type").asText()).isEqualTo("DELETE"))
				.thenCancel() //
				.verify();

		StepVerifier.create(body2) //
				.assertNext(e -> assertThat(e.get("type").asText()).isEqualTo("UPDATE")) //
				.assertNext(e -> assertThat(e.get("type").asText()).isEqualTo("DELETE")) //
				.thenCancel().verify();
	}
}