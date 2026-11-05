package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.repository.ContratoRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComissaoApiTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ContratoRepository contratos;

	private RestClient client;

	private long seguradoraId;
	private long titularId;
	private long vendedorId;
	private long outroVendedorId;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
		seguradoraId = idOf(post("/api/seguradoras", Map.of("nome", "Seguradora Comissao")));
		titularId = idOf(post("/api/titulares", Map.of("nome", "Titular Comissao")));
		vendedorId = idOf(post("/api/vendedores", Map.of("nome", "Vendedor Padrao")));
		outroVendedorId = idOf(post("/api/vendedores", Map.of("nome", "Vendedor Excecao")));
	}

	@Test
	void saleUsesPlanDefaultPercentOnDiscountedTotal() {
		long planoId = idOf(postPlano(Map.of(
				"nome", "Plano Percentual",
				"tipoComissao", "PERCENTUAL",
				"percentual", 10)));

		Map<String, Object> created = vender(planoId, vendedorId, 200f, 50d, "CT-COM-PADRAO");

		assertThat(number(created.get("valor"))).isCloseTo(150d, within(0.001));
		assertThat(number(created.get("valorComissao"))).isCloseTo(15d, within(0.001));
		assertThat(number(created.get("receitaCorretora"))).isCloseTo(135d, within(0.001));
		assertPersisted(created, 15d, 135d);
	}

	@Test
	void sellerOverrideReplacesPlanDefault() {
		long planoId = idOf(postPlano(Map.of(
				"nome", "Plano Com Excecao",
				"tipoComissao", "PERCENTUAL",
				"percentual", 10)));
		put("/api/planos/" + planoId + "/comissoes/" + outroVendedorId, Map.of(
				"tipoComissao", "VALOR_FIXO",
				"valorFixo", 40));

		Map<String, Object> padrao = vender(planoId, vendedorId, 200f, null, "CT-COM-BASE");
		assertThat(number(padrao.get("valorComissao"))).isCloseTo(20d, within(0.001));
		assertThat(number(padrao.get("receitaCorretora"))).isCloseTo(180d, within(0.001));

		Map<String, Object> excecao = vender(planoId, outroVendedorId, 200f, null, "CT-COM-EXC");
		assertThat(number(excecao.get("valorComissao"))).isCloseTo(40d, within(0.001));
		assertThat(number(excecao.get("receitaCorretora"))).isCloseTo(160d, within(0.001));
		assertPersisted(excecao, 40d, 160d);
	}

	@Test
	void missingRuleStoresZeroCommission() {
		long planoId = idOf(postPlano(Map.of("nome", "Plano Sem Comissao")));

		Map<String, Object> created = vender(planoId, vendedorId, 100f, null, "CT-COM-ZERO");

		assertThat(number(created.get("valorComissao"))).isCloseTo(0d, within(0.001));
		assertThat(number(created.get("receitaCorretora"))).isCloseTo(100d, within(0.001));
		assertPersisted(created, 0d, 100d);
	}

	@Test
	void renewalUsesPercentOnNewTotalAndKeepsFixedAmount() {
		long percentualId = idOf(postPlano(Map.of(
				"nome", "Plano Renovacao Percentual",
				"tipoComissao", "PERCENTUAL",
				"percentual", 10)));
		Map<String, Object> venda = vender(percentualId, vendedorId, 100f, null, "CT-COM-REN-P");
		assertThat(number(venda.get("valorComissao"))).isCloseTo(10d, within(0.001));

		Map<String, Object> renovado = renovar(idOf(venda), 250f);
		assertThat(number(renovado.get("valor"))).isCloseTo(250d, within(0.001));
		assertThat(number(renovado.get("valorComissao"))).isCloseTo(25d, within(0.001));
		assertThat(number(renovado.get("receitaCorretora"))).isCloseTo(225d, within(0.001));
		assertPersisted(renovado, 25d, 225d);

		long fixoId = idOf(postPlano(Map.of(
				"nome", "Plano Renovacao Fixa",
				"tipoComissao", "VALOR_FIXO",
				"valorFixo", 40)));
		Map<String, Object> vendaFixa = vender(fixoId, vendedorId, 100f, null, "CT-COM-REN-F");
		Map<String, Object> renovadoFixo = renovar(idOf(vendaFixa), 80f);
		assertThat(number(renovadoFixo.get("valorComissao"))).isCloseTo(40d, within(0.001));
		assertThat(number(renovadoFixo.get("receitaCorretora"))).isCloseTo(40d, within(0.001));
		assertPersisted(renovadoFixo, 40d, 40d);
	}

	@Test
	void renewalCommissionUsesDiscountedTotal() {
		long planoId = idOf(postPlano(Map.of(
				"nome", "Plano Renovacao Desconto",
				"tipoComissao", "PERCENTUAL",
				"percentual", 10)));
		Map<String, Object> venda = vender(planoId, vendedorId, 100f, null, "CT-COM-REN-DESC");

		Map<String, Object> renovado = post("/api/contratos/" + idOf(venda) + "/renovar", Map.of(
				"valor", 200f,
				"vlrDesconto", 50d));

		assertThat(number(renovado.get("valor"))).isCloseTo(150d, within(0.001));
		assertThat(number(renovado.get("valorComissao"))).isCloseTo(15d, within(0.001));
		assertThat(number(renovado.get("receitaCorretora"))).isCloseTo(135d, within(0.001));
		assertPersisted(renovado, 15d, 135d);
	}

	@Test
	void rejectsPercentOverOneHundredAndFixedAmountOverTotal() {
		ResponseEntity<Map<String, String>> percentual = client.post()
				.uri("/api/seguradoras/" + seguradoraId + "/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"nome", "Plano Percentual Alto",
						"tipoComissao", "PERCENTUAL",
						"percentual", 100.01))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(percentual.getStatusCode().value()).isEqualTo(400);
		assertThat(percentual.getBody()).isNotNull();
		assertThat(percentual.getBody().get("message")).contains("100");

		long planoId = idOf(postPlano(Map.of(
				"nome", "Plano Regra Alta",
				"tipoComissao", "PERCENTUAL",
				"percentual", 10)));
		ResponseEntity<Map<String, String>> excecao = client.put()
				.uri("/api/planos/" + planoId + "/comissoes/" + outroVendedorId)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"tipoComissao", "PERCENTUAL",
						"percentual", 150))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(excecao.getStatusCode().value()).isEqualTo(400);
		assertThat(excecao.getBody()).isNotNull();
		assertThat(excecao.getBody().get("message")).contains("100");

		put("/api/planos/" + planoId + "/comissao", Map.of(
				"tipoComissao", "VALOR_FIXO",
				"valorFixo", 500));
		ResponseEntity<Map<String, String>> venda = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"titularId", titularId,
						"vendedorId", vendedorId,
						"planoId", planoId,
						"valor", 200f,
						"codigo", "CT-COM-ALTO"))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(venda.getStatusCode().value()).isEqualTo(400);
		assertThat(venda.getBody()).isNotNull();
		assertThat(venda.getBody().get("message")).contains("total");

		put("/api/planos/" + planoId + "/comissao", Map.of(
				"tipoComissao", "VALOR_FIXO",
				"valorFixo", 30));
		Map<String, Object> aceita = vender(planoId, vendedorId, 80f, null, "CT-COM-OK");
		put("/api/planos/" + planoId + "/comissao", Map.of(
				"tipoComissao", "VALOR_FIXO",
				"valorFixo", 90));
		ResponseEntity<Map<String, String>> renovacao = client.post()
				.uri("/api/contratos/" + idOf(aceita) + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("valor", 50f))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(renovacao.getStatusCode().value()).isEqualTo(400);
		assertThat(renovacao.getBody()).isNotNull();
		assertThat(renovacao.getBody().get("message")).contains("total");

		Contrato stored = contratos.findById(idOf(aceita)).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ContratoStatus.ATIVO);
	}

	@Test
	void rejectsNegativeRenewalPremiumForPercentRule() {
		long planoId = idOf(postPlano(Map.of(
				"nome", "Plano Percentual Negativo",
				"tipoComissao", "PERCENTUAL",
				"percentual", 10)));
		Map<String, Object> venda = vender(planoId, vendedorId, 100f, null, "CT-COM-NEG");

		ResponseEntity<Map<String, String>> rejected = client.post()
				.uri("/api/contratos/" + idOf(venda) + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("valor", -50))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});

		assertThat(rejected.getStatusCode().value()).isEqualTo(400);
		assertThat(rejected.getBody()).isNotNull();
		assertThat(rejected.getBody().get("message")).contains("negativ");

		Contrato stored = contratos.findById(idOf(venda)).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ContratoStatus.ATIVO);
		assertThat(stored.getPlanoHistorico().getValor()).isEqualTo(100f);
		assertThat(stored.getValorComissao()).isCloseTo(10d, within(0.001));
		assertThat(contratos.findAll())
				.filteredOn(contrato -> contrato.getPlanoHistorico() != null
						&& contrato.getPlanoHistorico().getPlano() != null
						&& planoId == contrato.getPlanoHistorico().getPlano().getId())
				.allSatisfy(contrato -> assertThat(contrato.getPlanoHistorico().getValor()).isGreaterThanOrEqualTo(0f));
	}

	private void assertPersisted(Map<String, Object> body, double comissao, double receita) {
		Contrato stored = contratos.findById(idOf(body)).orElseThrow();
		assertThat(stored.getValorComissao()).isCloseTo(comissao, within(0.001));
		assertThat(stored.getReceitaCorretora()).isCloseTo(receita, within(0.001));
	}

	private Map<String, Object> vender(long planoId, long vendedor, float valor, Double desconto, String codigo) {
		Map<String, Object> body = new java.util.LinkedHashMap<>();
		body.put("titularId", titularId);
		body.put("vendedorId", vendedor);
		body.put("planoId", planoId);
		body.put("valor", valor);
		body.put("codigo", codigo);
		if (desconto != null) {
			body.put("vlrDesconto", desconto);
		}
		return post("/api/contratos", body);
	}

	private Map<String, Object> renovar(long contratoId, float valor) {
		return post("/api/contratos/" + contratoId + "/renovar", Map.of("valor", valor));
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

	private Map<String, Object> put(String path, Map<String, Object> body) {
		Map<String, Object> updated = client.put()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(updated).isNotNull();
		return updated;
	}

	private static long idOf(Map<String, Object> body) {
		return ((Number) body.get("id")).longValue();
	}

	private static double number(Object value) {
		assertThat(value).isInstanceOf(Number.class);
		return ((Number) value).doubleValue();
	}

}
