package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
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

import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.Seguradora;
import com.noriakijr.corretors.model.TipoComissao;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ParcelaRepository;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.repository.SeguradoraRepository;
import com.noriakijr.corretors.repository.TitularRepository;
import com.noriakijr.corretors.repository.VendedorRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardFinanceiroApiTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TitularRepository titulares;

	@Autowired
	private VendedorRepository vendedores;

	@Autowired
	private PlanoRepository planos;

	@Autowired
	private SeguradoraRepository seguradoras;

	@Autowired
	private ParcelaRepository parcelas;

	private RestClient client;
	private Titular titular;
	private LocalDate hoje;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
		hoje = LocalDate.now();
		titular = new Titular();
		titular.setNome("Titular Painel");
		titular = titulares.save(titular);
	}

	@Test
	void dashboardCountsActiveEndingOverdueSalesRenewalsAndInstallments() {
		Vendedor vendedor = vendedor("Vend Painel");
		Plano plano = plano(null, null, null);

		Map<String, Object> antes = dashboard();
		criar(vendedor, plano, "CT-VENC-30", 100f, hoje.minusMonths(2), hoje.minusMonths(2), hoje.plusDays(10), null);
		assertDelta(antes, dashboard(), 1, 1, 0, 0, 0, 0, 0, 0d, 0d);
		assertThat(codigos(dashboard(), "vencendo")).contains("CT-VENC-30");

		antes = dashboard();
		criar(vendedor, plano, "CT-PASSADO", 100f, hoje.minusMonths(2), hoje.minusMonths(2), hoje.minusDays(1), null);
		assertDelta(antes, dashboard(), 1, 0, 1, 0, 0, 0, 0, 0d, 0d);
		assertThat(codigos(dashboard(), "vencidos")).contains("CT-PASSADO");
		assertThat(codigos(dashboard(), "vencendo")).doesNotContain("CT-PASSADO");

		antes = dashboard();
		Map<String, Object> longe = criar(
				vendedor, plano, "CT-LONGE", 80f, hoje.minusMonths(2), hoje.minusMonths(2), hoje.plusDays(40), null);
		assertDelta(antes, dashboard(), 1, 0, 0, 0, 0, 0, 0, 0d, 0d);
		assertThat(codigos(dashboard(), "vencendo")).doesNotContain("CT-LONGE");
		assertThat(codigos(dashboard(), "vencidos")).doesNotContain("CT-LONGE");

		antes = dashboard();
		criar(vendedor, plano, "CT-HOJE", 40f, hoje, hoje, hoje.plusYears(1), null);
		assertDelta(antes, dashboard(), 1, 0, 0, 1, 0, 0, 0, 0d, 40d);
		assertThat(codigos(dashboard(), "vendas")).contains("CT-HOJE");

		antes = dashboard();
		Map<String, Object> renovado = post("/api/contratos/" + id(longe) + "/renovar", Map.of(
				"dtInicio", iso(hoje),
				"dtFim", iso(hoje.plusYears(1)),
				"valor", 80f));
		assertThat(renovado.get("codigo")).isEqualTo("CT-LONGE-R");
		assertDelta(antes, dashboard(), 0, 0, 0, 0, 1, 0, 0, 0d, 80d);
		assertThat(codigos(dashboard(), "renovacoes")).contains("CT-LONGE-R");
		assertThat(codigos(dashboard(), "vendas")).doesNotContain("CT-LONGE-R");
		assertThat(comissaoDe(dashboard(), vendedor.getNome())).isCloseTo(0d, within(0.001));
		assertThat(receitaDe(dashboard(), vendedor.getNome()))
				.isCloseTo(receitaDe(antes, vendedor.getNome()) + 80d, within(0.001));

		antes = dashboard();
		criar(vendedor, plano, "CT-PARC-DASH", 90f, hoje.minusMonths(2), hoje.minusDays(1), hoje.plusYears(1), 2);
		assertDelta(antes, dashboard(), 1, 0, 0, 0, 0, 1, 1, 0d, 0d);
		assertThat(codigosParcelas(dashboard())).contains("CT-PARC-DASH");

		Map<String, Object> financeiro = financeiro(
				hoje.minusDays(2),
				hoje,
				null,
				vendedor.getId());
		Map<String, Object> vencida = parcelasDe(financeiro).stream()
				.filter(parcela -> "CT-PARC-DASH".equals(parcela.get("codigo")))
				.filter(parcela -> "VENCIDA".equals(parcela.get("status")))
				.findFirst()
				.orElseThrow();

		antes = dashboard();
		Map<String, Object> paga = post("/api/parcelas/" + id(vencida) + "/pagar", Map.of());
		assertThat(paga.get("status")).isEqualTo("PAGA");
		assertThat(paga.get("dtPagamento").toString()).startsWith(hoje.toString());
		assertDelta(antes, dashboard(), 0, 0, 0, 0, 0, 0, -1, 0d, 0d);
		assertThat(codigosParcelas(dashboard())).doesNotContain("CT-PARC-DASH");

		Parcela stored = parcelas.findById(id(paga)).orElseThrow();
		assertThat(stored.getDtPagamento().toLocalDate()).isEqualTo(hoje);
		Map<String, Object> deNovo = post("/api/parcelas/" + id(paga) + "/pagar", Map.of());
		assertThat(deNovo.get("dtPagamento")).isEqualTo(paga.get("dtPagamento"));
		assertThat(parcelas.findById(id(paga)).orElseThrow().getDtPagamento()).isEqualTo(stored.getDtPagamento());
	}

	@Test
	void financeiroFiltersPeriodInsurerAndSellerAndTotalsInstallments() {
		Vendedor ana = vendedor("Ana Fin");
		Vendedor bruno = vendedor("Bruno Fin");
		Seguradora alfa = seguradora("Alfa Fin");
		Seguradora beta = seguradora("Beta Fin");
		Plano planoAlfa = plano(alfa, TipoComissao.PERCENTUAL, 10d);
		Plano planoBeta = plano(beta, null, null);
		LocalDate mesPassado = YearMonth.from(hoje).minusMonths(1).atDay(1);

		criar(ana, planoAlfa, "CT-ALFA-ANA", 200f, hoje, hoje, hoje.plusYears(1), 2, 20d);
		criar(ana, planoAlfa, "CT-ALFA-MES", 100f, mesPassado, mesPassado, mesPassado.plusYears(1), null);
		criar(bruno, planoBeta, "CT-BETA-BRUNO", 50f, hoje, hoje, hoje.plusYears(1), null);
		criar(bruno, planoAlfa, "CT-ALFA-BRUNO", 80f, hoje, hoje, hoje.plusYears(1), null);

		Map<String, Object> mes = financeiro(YearMonth.from(hoje).atDay(1), YearMonth.from(hoje).atEndOfMonth(), null, null);
		assertThat(codigos(mes, "contratos")).contains("CT-ALFA-ANA", "CT-BETA-BRUNO", "CT-ALFA-BRUNO");
		assertThat(codigos(mes, "contratos")).doesNotContain("CT-ALFA-MES");
		assertTotaisBatemComLista(mes);

		Map<String, Object> alfaAna = linha(mes, "CT-ALFA-ANA");
		assertThat(number(alfaAna.get("total"))).isCloseTo(200d, within(0.001));
		assertThat(number(alfaAna.get("desconto"))).isCloseTo(20d, within(0.001));
		assertThat(number(alfaAna.get("valorComissao"))).isCloseTo(18d, within(0.001));
		assertThat(number(alfaAna.get("receitaCorretora"))).isCloseTo(162d, within(0.001));
		assertThat(alfaAna.get("seguradoraNome")).isEqualTo(alfa.getNome());

		Map<String, Object> soAlfa = financeiro(
				YearMonth.from(hoje).atDay(1),
				YearMonth.from(hoje).atEndOfMonth(),
				alfa.getId(),
				null);
		assertThat(codigos(soAlfa, "contratos")).contains("CT-ALFA-ANA", "CT-ALFA-BRUNO");
		assertThat(codigos(soAlfa, "contratos")).doesNotContain("CT-BETA-BRUNO", "CT-ALFA-MES");
		assertMoney(soAlfa, 260d, 26d, 234d);

		Map<String, Object> anaAlfa = financeiro(
				YearMonth.from(hoje).atDay(1),
				YearMonth.from(hoje).atEndOfMonth(),
				alfa.getId(),
				ana.getId());
		assertThat(codigos(anaAlfa, "contratos")).containsExactly("CT-ALFA-ANA");
		assertMoney(anaAlfa, 180d, 18d, 162d);
		assertThat(parcelasDe(anaAlfa)).hasSize(1);
		assertThat(parcelasDe(anaAlfa).get(0).get("status")).isEqualTo("ABERTA");
		assertThat(number(parcelasDe(anaAlfa).get(0).get("valor"))).isCloseTo(90d, within(0.001));
		assertThat(parcelasDe(anaAlfa).get(0).get("dtParcela").toString()).startsWith(hoje.toString());
		assertThat(parcelasDe(anaAlfa).get(0).get("dtPagamento")).isNull();

		Map<String, Object> passado = financeiro(mesPassado, mesPassado.withDayOfMonth(mesPassado.lengthOfMonth()), null, ana.getId());
		assertThat(codigos(passado, "contratos")).contains("CT-ALFA-MES");
		assertThat(codigos(passado, "contratos")).doesNotContain("CT-ALFA-ANA");
		assertMoney(passado, 100d, 10d, 90d);
		assertThat(parcelasDe(passado).stream().map(parcela -> parcela.get("codigo"))).doesNotContain("CT-ALFA-ANA");

		ResponseEntity<Map<String, String>> invalido = client.get()
				.uri("/api/financeiro?de=" + hoje.plusDays(2) + "&ate=" + hoje)
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(invalido.getStatusCode().value()).isEqualTo(400);
		assertThat(invalido.getBody()).isNotNull();
		assertThat(invalido.getBody().get("message")).contains("periodo");

		ResponseEntity<Map<String, String>> ausente = client.post()
				.uri("/api/parcelas/999999999/pagar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of())
				.retrieve()
				.onStatus(status -> status.value() == 404, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(ausente.getStatusCode().value()).isEqualTo(404);
		assertThat(ausente.getBody()).isNotNull();
		assertThat(ausente.getBody().get("message")).contains("parcela");
	}

	private void assertDelta(
			Map<String, Object> antes,
			Map<String, Object> depois,
			long ativos,
			long vencendo,
			long vencidos,
			long vendas,
			long renovacoes,
			long abertas,
			long vencidas,
			double comissao,
			double receita) {
		assertThat(count(depois, "contratosAtivos") - count(antes, "contratosAtivos")).isEqualTo(ativos);
		assertThat(count(depois, "contratosVencendo") - count(antes, "contratosVencendo")).isEqualTo(vencendo);
		assertThat(count(depois, "contratosVencidos") - count(antes, "contratosVencidos")).isEqualTo(vencidos);
		assertThat(count(depois, "vendasHoje") - count(antes, "vendasHoje")).isEqualTo(vendas);
		assertThat(count(depois, "renovacoesHoje") - count(antes, "renovacoesHoje")).isEqualTo(renovacoes);
		assertThat(count(depois, "parcelasAbertas") - count(antes, "parcelasAbertas")).isEqualTo(abertas);
		assertThat(count(depois, "parcelasVencidas") - count(antes, "parcelasVencidas")).isEqualTo(vencidas);
		assertThat(number(depois.get("comissaoMes")) - number(antes.get("comissaoMes"))).isCloseTo(comissao, within(0.001));
		assertThat(number(depois.get("receitaCorretoraMes")) - number(antes.get("receitaCorretoraMes")))
				.isCloseTo(receita, within(0.001));
	}

	private void assertMoney(Map<String, Object> body, double faturado, double comissao, double receita) {
		assertThat(number(body.get("valorFaturado"))).isCloseTo(faturado, within(0.001));
		assertThat(number(body.get("comissao"))).isCloseTo(comissao, within(0.001));
		assertThat(number(body.get("receitaCorretora"))).isCloseTo(receita, within(0.001));
	}

	private void assertTotaisBatemComLista(Map<String, Object> body) {
		double faturado = 0;
		double comissao = 0;
		double receita = 0;
		for (Map<String, Object> row : rows(body, "contratos")) {
			double total = row.get("total") == null ? 0d : number(row.get("total"));
			double desconto = row.get("desconto") == null ? 0d : number(row.get("desconto"));
			faturado += total - desconto;
			comissao += row.get("valorComissao") == null ? 0d : number(row.get("valorComissao"));
			receita += row.get("receitaCorretora") == null ? 0d : number(row.get("receitaCorretora"));
		}
		assertMoney(body, faturado, comissao, receita);
	}

	private Map<String, Object> criar(
			Vendedor vendedor,
			Plano plano,
			String codigo,
			float valor,
			LocalDate aquisicao,
			LocalDate inicio,
			LocalDate fim,
			Integer quantidade) {
		return criar(vendedor, plano, codigo, valor, aquisicao, inicio, fim, quantidade, null);
	}

	private Map<String, Object> criar(
			Vendedor vendedor,
			Plano plano,
			String codigo,
			float valor,
			LocalDate aquisicao,
			LocalDate inicio,
			LocalDate fim,
			Integer quantidade,
			Double desconto) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("titularId", titular.getId());
		body.put("vendedorId", vendedor.getId());
		body.put("planoId", plano.getId());
		body.put("valor", valor);
		body.put("codigo", codigo);
		body.put("dtAquisicao", iso(aquisicao));
		body.put("dtInicio", iso(inicio));
		body.put("dtFim", iso(fim));
		if (quantidade != null) {
			body.put("quantidadeParcelas", quantidade);
		}
		if (desconto != null) {
			body.put("vlrDesconto", desconto);
		}
		return post("/api/contratos", body);
	}

	private Vendedor vendedor(String nome) {
		Vendedor vendedor = new Vendedor();
		vendedor.setNome(nome + " " + System.nanoTime());
		return vendedores.save(vendedor);
	}

	private Seguradora seguradora(String nome) {
		Seguradora seguradora = new Seguradora();
		seguradora.setNome(nome + " " + System.nanoTime());
		return seguradoras.save(seguradora);
	}

	private Plano plano(Seguradora seguradora, TipoComissao tipo, Double percentual) {
		Plano plano = new Plano();
		plano.setNome("Plano Painel " + System.nanoTime());
		plano.setSeguradora(seguradora);
		plano.setTipoComissao(tipo);
		plano.setPercentualComissao(percentual);
		return planos.save(plano);
	}

	private Map<String, Object> dashboard() {
		Map<String, Object> body = client.get()
				.uri("/api/dashboard")
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

	private Map<String, Object> financeiro(LocalDate de, LocalDate ate, Long seguradoraId, Long vendedorId) {
		StringBuilder uri = new StringBuilder("/api/financeiro?de=").append(de).append("&ate=").append(ate);
		if (seguradoraId != null) {
			uri.append("&seguradoraId=").append(seguradoraId);
		}
		if (vendedorId != null) {
			uri.append("&vendedorId=").append(vendedorId);
		}
		Map<String, Object> body = client.get()
				.uri(uri.toString())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		assertThat(body.get("de").toString()).startsWith(de.toString());
		assertThat(body.get("ate").toString()).startsWith(ate.toString());
		return body;
	}

	private Map<String, Object> post(String path, Map<String, Object> body) {
		Map<String, Object> response = client.post()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(response).isNotNull();
		return response;
	}

	private static long id(Map<String, Object> body) {
		return ((Number) body.get("id")).longValue();
	}

	private static long count(Map<String, Object> body, String key) {
		return ((Number) body.get(key)).longValue();
	}

	private static double number(Object value) {
		assertThat(value).isInstanceOf(Number.class);
		return ((Number) value).doubleValue();
	}

	private static List<String> codigos(Map<String, Object> body, String key) {
		return rows(body, key).stream().map(row -> (String) row.get("codigo")).toList();
	}

	private static List<String> codigosParcelas(Map<String, Object> body) {
		return rows(body, "parcelasVencidasLista").stream().map(row -> (String) row.get("codigo")).toList();
	}

	private static Map<String, Object> linha(Map<String, Object> body, String codigo) {
		return rows(body, "contratos").stream()
				.filter(row -> codigo.equals(row.get("codigo")))
				.findFirst()
				.orElseThrow();
	}

	private static List<Map<String, Object>> parcelasDe(Map<String, Object> body) {
		return rows(body, "parcelas");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> rows(Map<String, Object> body, String key) {
		Object value = body.get(key);
		assertThat(value).isInstanceOf(List.class);
		return (List<Map<String, Object>>) value;
	}

	private static double comissaoDe(Map<String, Object> body, String nome) {
		return rows(body, "vendedores").stream()
				.filter(row -> nome.equals(row.get("vendedorNome")))
				.mapToDouble(row -> number(row.get("valorComissao")))
				.sum();
	}

	private static double receitaDe(Map<String, Object> body, String nome) {
		return rows(body, "vendedores").stream()
				.filter(row -> nome.equals(row.get("vendedorNome")))
				.mapToDouble(row -> number(row.get("receitaCorretora")))
				.sum();
	}

	private static String iso(LocalDate data) {
		return data.atStartOfDay().toString();
	}

}
