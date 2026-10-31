package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
class PlanoPrecoApiTest {

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
	}

	@Test
	void keepsFullPriceHistoryNewestFirstAndIgnoresSaleSnapshot() {
		Map<String, Object> plano = post("/api/planos", Map.of(
				"nome", "Plano Familia Preco",
				"descricao", "Com historico",
				"valor", 100.0));
		long planoId = idOf(plano);
		assertThat(number(plano.get("valor"))).isCloseTo(100.0, within(0.001));

		Map<String, Object> segundo = post("/api/planos/" + planoId + "/precos", Map.of("valor", 250.0));
		assertThat(segundo.get("dtModif")).asString().contains("T");

		Map<String, Object> vendedor = post("/api/vendedores", Map.of("nome", "Vendedor Preco"));
		Map<String, Object> titular = post("/api/titulares", Map.of("nome", "Titular Preco"));
		post("/api/contratos", Map.of(
				"titularId", idOf(titular),
				"vendedorId", idOf(vendedor),
				"planoId", planoId,
				"valor", 999.0));

		List<Map<String, Object>> historico = list("/api/planos/" + planoId + "/precos");
		assertThat(historico).hasSize(2);
		assertThat(number(historico.get(0).get("valor"))).isCloseTo(250.0, within(0.001));
		assertThat(number(historico.get(1).get("valor"))).isCloseTo(100.0, within(0.001));
		assertThat(historico.get(0).get("dtModif")).isNotNull();
		assertThat(historico.get(1).get("dtModif")).isNotNull();
		assertThat(historico.get(0).get("dtModif").toString())
				.isGreaterThanOrEqualTo(historico.get(1).get("dtModif").toString());

		List<Map<String, Object>> planos = list("/api/planos");
		assertThat(planos).anySatisfy(item -> {
			if (idOf(item) == planoId) {
				assertThat(number(item.get("valor"))).isCloseTo(250.0, within(0.001));
			}
		});
	}

	@Test
	void planoWithoutPriceHasEmptyHistory() {
		Map<String, Object> plano = post("/api/planos", Map.of("nome", "Plano Sem Preco"));
		assertThat(plano.get("valor")).isNull();

		List<Map<String, Object>> historico = list("/api/planos/" + idOf(plano) + "/precos");
		assertThat(historico).isEmpty();
	}

	@Test
	void rejectsNegativePriceAndMissingPlano() {
		var invalid = client.post()
				.uri("/api/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Plano Negativo", "valor", -1))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<Map<String, String>>() {
				});
		assertThat(invalid.getStatusCode().value()).isEqualTo(400);
		assertThat(invalid.getBody()).isNotNull();
		assertThat(invalid.getBody().get("message")).contains("valor");

		var missing = client.get()
				.uri("/api/planos/999999/precos")
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
		return ((Number) body.get("id")).longValue();
	}

	private static double number(Object value) {
		assertThat(value).isInstanceOf(Number.class);
		return ((Number) value).doubleValue();
	}

}
