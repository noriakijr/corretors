package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
class PlanoTitularApiTest {

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
	}

	@Test
	void listsAndCreatesPlanos() {
		List<Map<String, Object>> before = list("/api/planos");

		Map<String, Object> created = client.post()
				.uri("/api/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Plano Essencial", "descricao", "Cobertura basica"))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		assertThat(created).isNotNull();
		assertThat(created.get("id")).isNotNull();
		assertThat(created.get("nome")).isEqualTo("Plano Essencial");
		assertThat(created.get("descricao")).isEqualTo("Cobertura basica");

		List<Map<String, Object>> after = list("/api/planos");
		assertThat(after).hasSize(before.size() + 1);
		assertThat(after).anySatisfy(plano -> assertThat(plano.get("nome")).isEqualTo("Plano Essencial"));
	}

	@Test
	void listsAndCreatesTitulares() {
		List<Map<String, Object>> before = list("/api/titulares");

		Map<String, Object> created = client.post()
				.uri("/api/titulares")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"nome", "Ana Souza",
						"cpf", "12345678901",
						"rg", "MG123",
						"dtNasc", LocalDate.of(1990, 1, 15).toString(),
						"cartaoEnv", false,
						"seguroVidaEnv", true,
						"contato", Map.of(
								"telFixo", "3133334444",
								"celular", "31999998888",
								"email", "ana@example.com",
								"endereco", Map.of(
										"pais", "Brasil",
										"estado", "MG",
										"cidade", "Belo Horizonte",
										"bairro", "Centro",
										"rua", "Afonso Pena",
										"numero", "100"))))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		assertThat(created).isNotNull();
		assertThat(created.get("nome")).isEqualTo("Ana Souza");
		assertThat(created.get("cpf")).isEqualTo("12345678901");
		assertThat(created.get("dtNasc")).isEqualTo("1990-01-15");
		assertThat(created.get("cartaoEnv")).isEqualTo(false);
		assertThat(created.get("seguroVidaEnv")).isEqualTo(true);

		@SuppressWarnings("unchecked")
		Map<String, Object> contato = (Map<String, Object>) created.get("contato");
		assertThat(contato.get("celular")).isEqualTo("31999998888");
		assertThat(contato.get("email")).isEqualTo("ana@example.com");

		@SuppressWarnings("unchecked")
		Map<String, Object> endereco = (Map<String, Object>) contato.get("endereco");
		assertThat(endereco.get("cidade")).isEqualTo("Belo Horizonte");
		assertThat(endereco.get("rua")).isEqualTo("Afonso Pena");

		List<Map<String, Object>> after = list("/api/titulares");
		assertThat(after).hasSize(before.size() + 1);
		assertThat(after).anySatisfy(titular -> assertThat(titular.get("nome")).isEqualTo("Ana Souza"));
	}

	@Test
	void rejectsPlanoWithoutNome() {
		var response = client.post()
				.uri("/api/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("descricao", "sem nome"))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<Map<String, String>>() {
				});

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().get("message")).contains("nome");
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

}
