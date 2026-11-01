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

	private long seguradoraId;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
		seguradoraId = idOf(post("/api/seguradoras", Map.of("nome", "Seguradora Preco")));
	}

	@Test
	void keepsFullPriceHistoryNewestFirstAndIgnoresSaleSnapshot() {
		Map<String, Object> plano = postPlano(Map.of(
				"nome", "Plano Familia Preco",
				"descricao", "Com historico",
				"valor", 100.0));
		long planoId = idOf(plano);
		assertThat(number(plano.get("valor"))).isCloseTo(100.0, within(0.001));
		assertThat(plano.get("valorRenovacao")).isNull();

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
		assertThat(historico).allSatisfy(item -> assertThat(item.get("renovacao")).isEqualTo(false));

		List<Map<String, Object>> planos = list("/api/planos");
		assertThat(planos).anySatisfy(item -> {
			if (idOf(item) == planoId) {
				assertThat(number(item.get("valor"))).isCloseTo(250.0, within(0.001));
			}
		});
	}

	@Test
	void planoWithoutPriceHasEmptyHistory() {
		Map<String, Object> plano = postPlano(Map.of("nome", "Plano Sem Preco"));
		assertThat(plano.get("valor")).isNull();
		assertThat(plano.get("valorRenovacao")).isNull();

		List<Map<String, Object>> historico = list("/api/planos/" + idOf(plano) + "/precos");
		assertThat(historico).isEmpty();
	}

	@Test
	void renewalPriceIsOptionalAndKeptInHistory() {
		Map<String, Object> semRenovacao = postPlano(Map.of(
				"nome", "Plano Sem Renovacao",
				"valor", 80.0));
		assertThat(semRenovacao.get("valorRenovacao")).isNull();
		List<Map<String, Object>> historicoSem = list("/api/planos/" + idOf(semRenovacao) + "/precos");
		assertThat(historicoSem).hasSize(1);
		assertThat(historicoSem.get(0).get("renovacao")).isEqualTo(false);

		Map<String, Object> comRenovacao = postPlano(Map.of(
				"nome", "Plano Com Renovacao",
				"valor", 100.0,
				"valorRenovacao", 120.0));
		long planoId = idOf(comRenovacao);
		assertThat(number(comRenovacao.get("valor"))).isCloseTo(100.0, within(0.001));
		assertThat(number(comRenovacao.get("valorRenovacao"))).isCloseTo(120.0, within(0.001));

		Map<String, Object> atualizado = post("/api/planos/" + planoId + "/precos", Map.of(
				"valor", 150.0,
				"renovacao", true));
		assertThat(atualizado.get("renovacao")).isEqualTo(true);
		assertThat(atualizado.get("dtModif")).asString().contains("T");

		List<Map<String, Object>> historico = list("/api/planos/" + planoId + "/precos");
		assertThat(historico).hasSize(3);
		assertThat(historico.get(0).get("renovacao")).isEqualTo(true);
		assertThat(number(historico.get(0).get("valor"))).isCloseTo(150.0, within(0.001));
		assertThat(historico.get(0).get("dtModif")).isNotNull();
		assertThat(historico).anySatisfy(item -> {
			assertThat(item.get("renovacao")).isEqualTo(true);
			assertThat(number(item.get("valor"))).isCloseTo(120.0, within(0.001));
		});

		List<Map<String, Object>> planos = list("/api/planos");
		assertThat(planos).anySatisfy(item -> {
			if (idOf(item) == planoId) {
				assertThat(number(item.get("valor"))).isCloseTo(100.0, within(0.001));
				assertThat(number(item.get("valorRenovacao"))).isCloseTo(150.0, within(0.001));
			}
		});
	}

	@Test
	void rejectsNegativePriceAndMissingPlano() {
		var invalid = client.post()
				.uri("/api/seguradoras/" + seguradoraId + "/planos")
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

	private Map<String, Object> postPlano(Map<String, Object> body) {
		return post("/api/seguradoras/" + seguradoraId + "/planos", body);
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
