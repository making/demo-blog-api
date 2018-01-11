package com.example.blog.entry;

import am.ik.blog.entry.EntryId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.flyway.enabled=false", "blog.github.access-token=foo",
		"blog.github.webhook-secret=bar" })
public class EntryControllerTest {
	@Autowired
	ApplicationContext context;
	@LocalServerPort
	int port;
	WebTestClient webClient;
	@MockBean
	EntryRepository entryRepository;

	@Before
	public void setup() {
		this.webClient = WebTestClient.bindToServer() //
				.baseUrl("http://localhost:" + port) //
				.build();
	}

	@Test
	public void getEntry_NotFound() throws Exception {
		this.webClient.get() //
				.uri("/v1/entries/999") //
				.accept(MediaType.APPLICATION_JSON) //
				.exchange() //
				.expectStatus() //
				.isNotFound() //
				.expectBody() //
				.jsonPath("message").isEqualTo("EntryId 999 is not found.");
	}

	@Test
	public void getEntry() throws Exception {
		EntryId entryId = new EntryId("100");
		given(entryRepository.findById(entryId))
				.willReturn(Optional.of(Fixtures.entry(entryId)));

		this.webClient.get() //
				.uri("/v1/entries/100") //
				.accept(MediaType.APPLICATION_JSON) //
				.exchange() //
				.expectStatus() //
				.isOk() //
				.expectBody() //
				.jsonPath("entryId").isEqualTo(100) //
				.jsonPath("content").isEqualTo("Hello") //
				.jsonPath("entryId").isEqualTo(100) //
				.jsonPath("content").isEqualTo("Hello") //
				.jsonPath("created").isMap() //
				.jsonPath("created.name").isEqualTo("demo") //
				.jsonPath("created.date").isNotEmpty() //
				.jsonPath("updated").isMap() //
				.jsonPath("updated.name").isEqualTo("demo") //
				.jsonPath("updated.date").isNotEmpty() //
				.jsonPath("frontMatter").isMap() //
				.jsonPath("frontMatter.title").isEqualTo("Hello") //
				.jsonPath("frontMatter.categories").isArray() //
				.jsonPath("frontMatter.categories[0]").isEqualTo("foo") //
				.jsonPath("frontMatter.categories[1]").isEqualTo("bar") //
				.jsonPath("frontMatter.categories[2]").isEqualTo("hoge") //
				.jsonPath("frontMatter.tags").isArray() //
				.jsonPath("frontMatter.tags[0]").isEqualTo("a") //
				.jsonPath("frontMatter.tags[1]").isEqualTo("b") //
				.jsonPath("frontMatter.tags[2]").isEqualTo("c");
	}

	@Test
	public void getEntries() throws Exception {
		given(entryRepository.findAll()).willReturn(
				Collections.singletonList(Fixtures.entry(new EntryId("100"))));

		this.webClient.get() //
				.uri("/v1/entries") //
				.accept(MediaType.APPLICATION_JSON) //
				.exchange() //
				.expectStatus() //
				.isOk() //
				.expectBody() //
				.jsonPath("$").isArray() //
				.jsonPath("$[0].entryId").isEqualTo(100) //
				.jsonPath("$[0].content").isEqualTo("Hello") //
				.jsonPath("$[0].created").isMap() //
				.jsonPath("$[0].created.name").isEqualTo("demo") //
				.jsonPath("$[0].created.date").isNotEmpty() //
				.jsonPath("$[0].updated").isMap() //
				.jsonPath("$[0].updated.name").isEqualTo("demo") //
				.jsonPath("$[0].updated.date").isNotEmpty() //
				.jsonPath("$[0].frontMatter").isMap() //
				.jsonPath("$[0].frontMatter.title").isEqualTo("Hello") //
				.jsonPath("$[0].frontMatter.categories").isArray() //
				.jsonPath("$[0].frontMatter.categories[0]").isEqualTo("foo") //
				.jsonPath("$[0].frontMatter.categories[1]").isEqualTo("bar") //
				.jsonPath("$[0].frontMatter.categories[2]").isEqualTo("hoge") //
				.jsonPath("$[0].frontMatter.tags").isArray() //
				.jsonPath("$[0].frontMatter.tags[0]").isEqualTo("a") //
				.jsonPath("$[0].frontMatter.tags[1]").isEqualTo("b") //
				.jsonPath("$[0].frontMatter.tags[2]").isEqualTo("c");
	}

}