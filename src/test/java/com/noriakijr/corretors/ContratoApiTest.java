package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.repository.TitularRepository;
import com.noriakijr.corretors.repository.VendedorRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoApiTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TitularRepository titulares;

	@Autowired
	private VendedorRepository vendedores;

	@Autowired
	private PlanoRepository planos;

	@Autowired
	private ContratoRepository contratos;

	private RestClient client;
	private Titular titular;
	private Vendedor vendedor;
	private Plano planoSaude;
	private Plano planoVida;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();

		titular = new Titular();
		titular.setNome("Titular Contrato");
		titular = titulares.save(titular);

		vendedor = new Vendedor();
		vendedor.setNome("Vendedor Contrato");
		vendedor = vendedores.save(vendedor);

		planoSaude = new Plano();
		planoSaude.setNome("Plano Saude");
		planoSaude = planos.save(planoSaude);

		planoVida = new Plano();
		planoVida.setNome("Plano Vida");
		planoVida = planos.save(planoVida);
	}

	@Test
	void createsAndListsMultipleActiveContractsForSameTitular() {
		Map<String, Object> saude = createContrato(titular.getId(), planoSaude.getId(), "CT-SAUDE", 199.9f,
				"2026-01-01T00:00:00", "2027-01-01T00:00:00");
		Map<String, Object> vida = createContrato(titular.getId(), planoVida.getId(), "CT-VIDA", 89.5f,
				"2026-03-01T00:00:00", "2027-03-01T00:00:00");

		assertThat(saude.get("status")).isEqualTo("ATIVO");
		assertThat(vida.get("status")).isEqualTo("ATIVO");
		assertThat(saude.get("id")).isNotEqualTo(vida.get("id"));
		assertThat(saude.get("titularId")).isEqualTo(titular.getId().intValue());
		assertThat(vida.get("titularId")).isEqualTo(titular.getId().intValue());

		List<Map<String, Object>> listed = list("/api/titulares/" + titular.getId() + "/contratos");
		assertThat(listed).hasSizeGreaterThanOrEqualTo(2);
		assertThat(listed).anySatisfy(c -> {
			assertThat(c.get("codigo")).isEqualTo("CT-SAUDE");
			assertThat(c.get("status")).isEqualTo("ATIVO");
			assertThat(((Number) c.get("valor")).floatValue()).isEqualTo(199.9f);
		});
		assertThat(listed).anySatisfy(c -> {
			assertThat(c.get("codigo")).isEqualTo("CT-VIDA");
			assertThat(c.get("status")).isEqualTo("ATIVO");
		});

		assertThat(contratos.findByTitularIdAndStatus(titular.getId(), ContratoStatus.ATIVO))
				.hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	void renewCreatesNewContractLinkedToPrevious() {
		Map<String, Object> original = createContrato(titular.getId(), planoSaude.getId(), "CT-REN", 150f,
				"2025-01-01T00:00:00", "2026-01-01T00:00:00");
		Number originalId = (Number) original.get("id");

		Map<String, Object> renewed = client.post()
				.uri("/api/contratos/" + originalId.longValue() + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"dtInicio", "2026-01-01T00:00:00",
						"dtFim", "2027-01-01T00:00:00",
						"valor", 160f,
						"codigo", "CT-REN-2026"))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		assertThat(renewed).isNotNull();
		assertThat(renewed.get("id")).isNotEqualTo(originalId.intValue());
		assertThat(renewed.get("status")).isEqualTo("ATIVO");
		assertThat(renewed.get("codigo")).isEqualTo("CT-REN-2026");
		assertThat(renewed.get("contratoAnteriorId")).isEqualTo(originalId.intValue());
		assertThat(renewed.get("titularId")).isEqualTo(titular.getId().intValue());
		assertThat(((Number) renewed.get("valor")).floatValue()).isEqualTo(160f);
		assertThat(renewed.get("planoHistoricoId")).isNotEqualTo(original.get("planoHistoricoId"));

		Map<String, Object> previous = client.get()
				.uri("/api/contratos/" + originalId.longValue())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(previous).isNotNull();
		assertThat(previous.get("status")).isEqualTo("RENOVADO");

		Contrato storedPrevious = contratos.findById(originalId.longValue()).orElseThrow();
		Contrato storedNew = contratos.findById(((Number) renewed.get("id")).longValue()).orElseThrow();
		assertThat(storedPrevious.getStatus()).isEqualTo(ContratoStatus.RENOVADO);
		assertThat(storedNew.getStatus()).isEqualTo(ContratoStatus.ATIVO);
		assertThat(storedNew.getContratoAnterior().getId()).isEqualTo(storedPrevious.getId());
		assertThat(storedNew.getId()).isNotEqualTo(storedPrevious.getId());
	}

	@Test
	void renewRejectsNonActiveContract() {
		Map<String, Object> original = createContrato(titular.getId(), planoSaude.getId(), "CT-ONCE", 100f,
				"2025-01-01T00:00:00", "2026-01-01T00:00:00");
		Number originalId = (Number) original.get("id");

		client.post()
				.uri("/api/contratos/" + originalId.longValue() + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of())
				.retrieve()
				.body(new ParameterizedTypeReference<Map<String, Object>>() {
				});

		var response = client.post()
				.uri("/api/contratos/" + originalId.longValue() + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of())
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<Map<String, String>>() {
				});

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().get("message")).contains("ATIVO");
	}

	private Map<String, Object> createContrato(
			Long titularId,
			Long planoId,
			String codigo,
			float valor,
			String dtInicio,
			String dtFim) {
		Map<String, Object> body = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"titularId", titularId,
						"vendedorId", vendedor.getId(),
						"planoId", planoId,
						"valor", valor,
						"codigo", codigo,
						"descricao", "Contrato " + codigo,
						"dtAquisicao", dtInicio,
						"dtInicio", dtInicio,
						"dtFim", dtFim))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		assertThat(body.get("id")).isNotNull();
		assertThat(body.get("dtInicio").toString()).startsWith(dtInicio.substring(0, 16));
		assertThat(LocalDateTime.parse(body.get("dtInicio").toString())).isEqualTo(LocalDateTime.parse(dtInicio));
		return body;
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
