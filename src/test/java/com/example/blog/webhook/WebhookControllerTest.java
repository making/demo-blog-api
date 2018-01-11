package com.example.blog.webhook;

import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;
import com.example.blog.Fixtures;
import com.example.blog.entry.EntryRepository;
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
import reactor.core.publisher.Mono;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    }

}