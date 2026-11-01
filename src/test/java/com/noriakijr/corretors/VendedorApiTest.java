package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VendedorApiTest {

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
	}

	@Test
	void listsAndCreatesVendedores() {
		List<Map<String, Object>> before = list();

		Map<String, Object> created = client.post()
				.uri("/api/vendedores")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "  Marina Alves  "))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		assertThat(created).isNotNull();
		assertThat(created.get("id")).isNotNull();
		assertThat(created.get("nome")).isEqualTo("Marina Alves");

		List<Map<String, Object>> after = list();
		assertThat(after).hasSize(before.size() + 1);
		assertThat(after).anySatisfy(vendedor -> assertThat(vendedor.get("nome")).isEqualTo("Marina Alves"));
	}

	@Test
	void rejectsVendedorWithoutNome() {
		var response = client.post()
				.uri("/api/vendedores")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "   "))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<Map<String, String>>() {
				});

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().get("message")).contains("nome");
	}

	private List<Map<String, Object>> list() {
		List<Map<String, Object>> body = client.get()
				.uri("/api/vendedores")
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

}
