package com.example.blog.webhook;

import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;
import com.example.blog.Fixtures;
import com.example.blog.entry.EntryRepository;
import com.example.blog.entry.event.EntryDeleteEvent;
import com.example.blog.entry.event.EntryUpdateEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.enabled=false", "blog.github.access-token=foo",
        "blog.github.webhook-secret=bar"})
public class WebhookControllerTest {
    @Autowired
    ObjectMapper objectMapper;
    @LocalServerPort
    int port;
    WebTestClient webClient;
    @MockBean
    EntryFetcher entryFetcher;
    @MockBean
    EntryRepository entryRepository;

    @Before
    public void setup() {
        this.webClient = WebTestClient.bindToServer() //
                .baseUrl("http://localhost:" + port) //
                .build();
    }

    @Test
    public void webhookAdded() throws Exception {
        Entry entry = Fixtures.entry(new EntryId(100L));
        given(entryFetcher.fetch("example", "blog.example.com", "content/00100.md"))
                .willReturn(Mono.just(entry));

        ObjectNode body = this.objectMapper.createObjectNode();
        body.putObject("repository").put("full_name", "example/blog.example.com");
        ArrayNode commits = body.putArray("commits");
        ObjectNode commit = commits.addObject();
        commit.putArray("added").add("content/00100.md");
        commit.putArray("modified");
        commit.putArray("removed");

        WebhookVerifier verifier = new WebhookVerifier("bar");

        Flux<JsonNode> notification = this.webClient.get() //
                .uri("/v1/notification") //
                .exchange() //
                .expectStatus().isOk() //
                .expectHeader().contentType(new MediaType(TEXT_EVENT_STREAM, UTF_8)) //
                .returnResult(JsonNode.class) //
                .getResponseBody();

        this.webClient.post() //
                .uri("/webhook") //
                .syncBody(body) //
                .accept(MediaType.APPLICATION_JSON) //
                .header("X-Hub-Signature", verifier.signature(body.toString())) //
                .exchange() //
                .expectStatus() //
                .isOk() //
                .expectBody() //
                .jsonPath("$").isArray() //
                .jsonPath("$.length()").isEqualTo(1) //
                .jsonPath("$[0].added").isEqualTo(100);

        verify(entryRepository).create(entry);

        StepVerifier.create(notification) //
                .assertNext(e -> {
                    assertThat(e.get("type").asText()).isEqualTo("CREATE");
                    JsonNode ent = e.get("entry");
                    assertThat(ent).isNotNull();
                    assertThat(ent.get("entryId").asInt()).isEqualTo(100);
                    assertThat(ent.get("content").asText()).isEqualTo("Hello");
                })
                .thenCancel() //
                .verify();
    }

    @Test
    public void webhookModified() throws Exception {
        Entry entry = Fixtures.entry(new EntryId(100L));
        given(entryFetcher.fetch("example", "blog.example.com", "content/00100.md"))
                .willReturn(Mono.just(entry));

        ObjectNode body = this.objectMapper.createObjectNode();
        body.putObject("repository").put("full_name", "example/blog.example.com");
        ArrayNode commits = body.putArray("commits");
        ObjectNode commit = commits.addObject();
        commit.putArray("added");
        commit.putArray("modified").add("content/00100.md");
        commit.putArray("removed");

        WebhookVerifier verifier = new WebhookVerifier("bar");

        Flux<JsonNode> notification = this.webClient.get() //
                .uri("/v1/notification") //
                .exchange() //
                .expectStatus().isOk() //
                .expectHeader().contentType(new MediaType(TEXT_EVENT_STREAM, UTF_8)) //
                .returnResult(JsonNode.class) //
                .getResponseBody();

        this.webClient.post() //
                .uri("/webhook") //
                .syncBody(body) //
                .accept(MediaType.APPLICATION_JSON) //
                .header("X-Hub-Signature", verifier.signature(body.toString())) //
                .exchange() //
                .expectStatus() //
                .isOk() //
                .expectBody() //
                .jsonPath("$").isArray() //
                .jsonPath("$.length()").isEqualTo(1) //
                .jsonPath("$[0].modified").isEqualTo(100);

        verify(entryRepository).update(entry);

        StepVerifier.create(notification) //
                .assertNext(e -> {
                    assertThat(e.get("type").asText()).isEqualTo("UPDATE");
                    JsonNode ent = e.get("entry");
                    assertThat(ent).isNotNull();
                    assertThat(ent.get("entryId").asInt()).isEqualTo(100);
                    assertThat(ent.get("content").asText()).isEqualTo("Hello");
                })
                .thenCancel() //
                .verify();
    }


    @Test
    public void webhookRemoved() throws Exception {
        Entry entry = Fixtures.entry(new EntryId(100L));
        given(entryFetcher.fetch("example", "blog.example.com", "content/00100.md"))
                .willReturn(Mono.just(entry));

        ObjectNode body = this.objectMapper.createObjectNode();
        body.putObject("repository").put("full_name", "example/blog.example.com");
        ArrayNode commits = body.putArray("commits");
        ObjectNode commit = commits.addObject();
        commit.putArray("added");
        commit.putArray("modified");
        commit.putArray("removed").add("content/00100.md");

        WebhookVerifier verifier = new WebhookVerifier("bar");

        Flux<JsonNode> notification = this.webClient.get() //
                .uri("/v1/notification") //
                .exchange() //
                .expectStatus().isOk() //
                .expectHeader().contentType(new MediaType(TEXT_EVENT_STREAM, UTF_8)) //
                .returnResult(JsonNode.class) //
                .getResponseBody();

        this.webClient.post() //
                .uri("/webhook") //
                .syncBody(body) //
                .accept(MediaType.APPLICATION_JSON) //
                .header("X-Hub-Signature", verifier.signature(body.toString())) //
                .exchange() //
                .expectStatus() //
                .isOk() //
                .expectBody() //
                .jsonPath("$").isArray() //
                .jsonPath("$.length()").isEqualTo(1) //
                .jsonPath("$[0].removed").isEqualTo(100);

        verify(entryRepository).delete(entry.getEntryId());

        StepVerifier.create(notification) //
                .assertNext(e -> {
                    assertThat(e.get("type").asText()).isEqualTo("DELETE");
                    assertThat(e.get("entryId").asInt()).isEqualTo(100);
                })
                .thenCancel() //
                .verify();
    }

}