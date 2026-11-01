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
class SeguradoraApiTest {

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
	}

	@Test
	void insurerOwnsSeveralPlansAndAnotherInsurerDoesNot() {
		Map<String, Object> porto = post("/api/seguradoras", Map.of("nome", "Porto Seguro"));
		Map<String, Object> sul = post("/api/seguradoras", Map.of("nome", "SulAmerica"));
		long portoId = idOf(porto);
		long sulId = idOf(sul);

		Map<String, Object> saude = post("/api/seguradoras/" + portoId + "/planos", Map.of(
				"nome", "Saude Essencial",
				"descricao", "Ambulatorial",
				"valor", 200.0));
		Map<String, Object> vida = post("/api/seguradoras/" + portoId + "/planos", Map.of(
				"nome", "Vida Familiar",
				"valor", 80.0,
				"valorRenovacao", 90.0));

		assertThat(idOf(saude.get("seguradoraId"))).isEqualTo(portoId);
		assertThat(saude.get("seguradoraNome")).isEqualTo("Porto Seguro");
		assertThat(idOf(vida.get("seguradoraId"))).isEqualTo(portoId);
		assertThat(vida.get("valorRenovacao")).isNotNull();

		List<Map<String, Object>> daPorto = list("/api/seguradoras/" + portoId + "/planos");
		assertThat(daPorto).extracting(item -> item.get("nome")).contains("Saude Essencial", "Vida Familiar");
		assertThat(daPorto).allSatisfy(item -> assertThat(idOf(item.get("seguradoraId"))).isEqualTo(portoId));

		List<Map<String, Object>> daSul = list("/api/seguradoras/" + sulId + "/planos");
		assertThat(daSul).isEmpty();

		List<Map<String, Object>> todas = list("/api/planos");
		assertThat(todas).anySatisfy(item -> {
			assertThat(item.get("nome")).isEqualTo("Saude Essencial");
			assertThat(item.get("seguradoraNome")).isEqualTo("Porto Seguro");
		});
	}

	@Test
	void rejectsInsurerWithoutNomeAndUnknownInsurer() {
		var invalid = client.post()
				.uri("/api/seguradoras")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", " "))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<Map<String, String>>() {
				});
		assertThat(invalid.getStatusCode().value()).isEqualTo(400);
		assertThat(invalid.getBody()).isNotNull();
		assertThat(invalid.getBody().get("message")).contains("nome");

		var missing = client.get()
				.uri("/api/seguradoras/999999/planos")
				.retrieve()
				.onStatus(status -> status.value() == 404, (req, res) -> {
				})
				.toEntity(String.class);
		assertThat(missing.getStatusCode().value()).isEqualTo(404);
	}

	private Map<String, Object> post(String path, Map<String, Object> body) {
		Map<String, Object> created = client.post()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(created).isNotNull();
		return created;
	}

	private List<Map<String, Object>> list(String path) {
		List<Map<String, Object>> body = client.get()
				.uri(path)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

	private static long idOf(Map<String, Object> body) {
		return idOf(body.get("id"));
	}

	private static long idOf(Object value) {
		assertThat(value).isInstanceOf(Number.class);
		return ((Number) value).longValue();
	}

}
