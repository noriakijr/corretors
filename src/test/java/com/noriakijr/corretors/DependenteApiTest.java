package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
class DependenteApiTest {

	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
	private Plano plano;

	@BeforeEach
	void setUp() {
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();

		titular = new Titular();
		titular.setNome("Titular Dependente");
		titular = titulares.save(titular);

		vendedor = new Vendedor();
		vendedor.setNome("Vendedor Dependente");
		vendedor = vendedores.save(vendedor);

		plano = new Plano();
		plano.setNome("Plano Dependente");
		plano = planos.save(plano);
	}

	@Test
	void storesOptionalDependentLimitOnPlan() {
		Map<String, Object> seguradora = client.post()
				.uri("/api/seguradoras")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Seguradora Limite"))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(seguradora).isNotNull();
		long seguradoraId = ((Number) seguradora.get("id")).longValue();

		Map<String, Object> semLimite = client.post()
				.uri("/api/seguradoras/" + seguradoraId + "/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Sem limite"))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(semLimite).isNotNull();
		assertThat(semLimite.get("limiteDependentes")).isNull();

		Map<String, Object> comLimite = client.post()
				.uri("/api/seguradoras/" + seguradoraId + "/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Com limite", "limiteDependentes", 3))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(comLimite).isNotNull();
		assertThat(comLimite.get("limiteDependentes")).isEqualTo(3);

		Map<String, Object> atualizado = client.put()
				.uri("/api/planos/" + ((Number) comLimite.get("id")).longValue())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("limiteDependentes", 0))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(atualizado).isNotNull();
		assertThat(atualizado.get("limiteDependentes")).isEqualTo(0);

		ResponseEntity<Map<String, String>> invalido = client.post()
				.uri("/api/seguradoras/" + seguradoraId + "/planos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Negativo", "limiteDependentes", -1))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(invalido.getStatusCode().value()).isEqualTo(400);
		assertThat(invalido.getBody()).isNotNull();
		assertThat(invalido.getBody().get("message")).contains("limite");
	}

	@Test
	void allowsDependentsPastTheLimitAndClearsAttentionWhenCountReturns() {
		plano.setLimiteDependentes(1);
		planos.save(plano);
		long contratoId = contratoVigente("CT-LIMITE");
		assertThat(contrato(contratoId).get("pontoAtencao")).isEqualTo(false);

		Map<String, Object> primeiro = criarDependente(contratoId, "Ana", "filha", "2014-02-03");
		assertThat(primeiro.get("nome")).isEqualTo("Ana");
		assertThat(primeiro.get("parentesco")).isEqualTo("filha");
		assertThat(primeiro.get("dtNasc").toString()).isEqualTo("2014-02-03");
		assertThat(contrato(contratoId).get("pontoAtencao")).isEqualTo(false);

		Map<String, Object> segundo = criarDependente(contratoId, "Bruno", "filho", "2016-05-01");
		assertThat(segundo.get("nome")).isEqualTo("Bruno");
		assertThat(listar(contratoId)).extracting(item -> item.get("nome")).containsExactly("Ana", "Bruno");
		assertThat(contrato(contratoId).get("pontoAtencao")).isEqualTo(true);
		assertThat(listarContratos()).extracting(item -> item.get("pontoAtencao")).containsExactly(true);

		Map<String, Object> editado = client.put()
				.uri("/api/contratos/" + contratoId + "/dependentes/" + ((Number) primeiro.get("id")).longValue())
				.contentType(MediaType.APPLICATION_JSON)
				.body(dependente("Ana Clara", "filha", "2014-02-03"))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(editado).isNotNull();
		assertThat(editado.get("nome")).isEqualTo("Ana Clara");
		assertThat(listar(contratoId)).hasSize(2);
		assertThat(contrato(contratoId).get("pontoAtencao")).isEqualTo(true);

		ResponseEntity<Void> removido = client.delete()
				.uri("/api/contratos/" + contratoId + "/dependentes/" + ((Number) segundo.get("id")).longValue())
				.retrieve()
				.toBodilessEntity();
		assertThat(removido.getStatusCode().value()).isEqualTo(204);
		assertThat(listar(contratoId)).extracting(item -> item.get("nome")).containsExactly("Ana Clara");
		assertThat(contrato(contratoId).get("pontoAtencao")).isEqualTo(false);
		assertThat(listarContratos()).extracting(item -> item.get("pontoAtencao")).containsExactly(false);
	}

	@Test
	void rejectsChangesOutsideVigenciaButKeepsTheList() {
		long passado = criarContrato("CT-PASSADO", LocalDate.now().minusYears(2), LocalDate.now().minusDays(1));
		ResponseEntity<Map<String, String>> fora = postDependente(passado, "Lia", "sobrinha", "2018-01-01", 400);
		assertThat(fora.getBody()).isNotNull();
		assertThat(fora.getBody().get("message")).contains("vigente");

		long futuro = criarContrato("CT-FUTURO", LocalDate.now().plusDays(1), LocalDate.now().plusYears(1));
		assertThat(postDependente(futuro, "Lia", "sobrinha", "2018-01-01", 400).getStatusCode().value()).isEqualTo(400);

		long hoje = criarContrato("CT-HOJE", LocalDate.now(), LocalDate.now());
		criarDependente(hoje, "No dia", "cônjuge", "1990-01-01");

		long maisTardeHoje = criarContrato(
				"CT-HOJE-TARDE",
				LocalDate.now().atTime(23, 30),
				LocalDate.now().plusYears(1).atStartOfDay());
		criarDependente(maisTardeHoje, "Noite", "mãe", "1960-01-01");

		long ateHoje = criarContrato(
				"CT-HOJE-FIM",
				LocalDate.now().minusDays(10).atStartOfDay(),
				LocalDate.now().atStartOfDay());
		Map<String, Object> guardado = criarDependente(ateHoje, "Fim", "pai", "1958-03-03");
		long dependenteId = ((Number) guardado.get("id")).longValue();

		encerrar(ateHoje, LocalDate.now().minusDays(1).atStartOfDay(), ContratoStatus.ATIVO);
		assertThat(putDependente(ateHoje, dependenteId, 400).getBody().get("message")).contains("vigente");
		assertThat(deleteDependente(ateHoje, dependenteId, 400).getBody().get("message")).contains("vigente");
		assertThat(postDependente(ateHoje, "Outro", "irmão", "2001-01-01", 400).getStatusCode().value()).isEqualTo(400);
		assertThat(listar(ateHoje)).hasSize(1);

		long cancelado = contratoVigente("CT-CANC");
		Map<String, Object> doCancelado = criarDependente(cancelado, "Cancelado", "filho", "2010-01-01");
		encerrar(cancelado, null, ContratoStatus.CANCELADO);
		assertThat(putDependente(cancelado, ((Number) doCancelado.get("id")).longValue(), 400).getStatusCode().value())
				.isEqualTo(400);

		long encerrado = contratoVigente("CT-ENC");
		Map<String, Object> doEncerrado = criarDependente(encerrado, "Encerrado", "filho", "2011-01-01");
		encerrar(encerrado, null, ContratoStatus.ENCERRADO);
		assertThat(deleteDependente(encerrado, ((Number) doEncerrado.get("id")).longValue(), 400).getStatusCode().value())
				.isEqualTo(400);
		assertThat(listar(encerrado)).hasSize(1);
	}

	@Test
	void copiesEveryDependentOnRenewAndMarksWhenOverTheRenewedPlanLimit() {
		long contratoId = contratoVigente("CT-COPIA");
		criarDependente(contratoId, "Ana", "filha", "2014-02-03");
		criarDependente(contratoId, "Bruno", "filho", "2016-05-01");
		criarDependente(contratoId, "Caio", "filho", "2018-07-09");

		plano.setLimiteDependentes(2);
		planos.save(plano);

		Map<String, Object> renovado = client.post()
				.uri("/api/contratos/" + contratoId + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"dtInicio", stamp(LocalDate.now().atStartOfDay()),
						"dtFim", stamp(LocalDate.now().plusYears(1).atStartOfDay()),
						"codigo", "CT-COPIA-NOVO"))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(renovado).isNotNull();
		assertThat(renovado.get("pontoAtencao")).isEqualTo(true);
		long novoId = ((Number) renovado.get("id")).longValue();

		assertThat(listar(contratoId)).extracting(item -> item.get("nome")).containsExactly("Ana", "Bruno", "Caio");
		assertThat(listar(novoId)).extracting(item -> item.get("nome")).containsExactly("Ana", "Bruno", "Caio");
		assertThat(contrato(novoId).get("pontoAtencao")).isEqualTo(true);
		assertThat(contrato(contratoId).get("pontoAtencao")).isEqualTo(true);
		assertThat(postDependente(contratoId, "Dora", "filha", "2020-01-01", 400).getBody().get("message"))
				.contains("vigente");

		long semVaga = contratoVigente("CT-ZERO");
		criarDependente(semVaga, "Eva", "mãe", "1970-01-01");
		plano.setLimiteDependentes(0);
		planos.save(plano);
		Map<String, Object> zerado = client.post()
				.uri("/api/contratos/" + semVaga + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"dtInicio", stamp(LocalDate.now().atStartOfDay()),
						"dtFim", stamp(LocalDate.now().plusYears(1).atStartOfDay())))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(zerado).isNotNull();
		long zeradoId = ((Number) zerado.get("id")).longValue();
		assertThat(listar(zeradoId)).extracting(item -> item.get("nome")).containsExactly("Eva");
		assertThat(zerado.get("pontoAtencao")).isEqualTo(true);
		assertThat(listar(semVaga)).hasSize(1);

		plano.setLimiteDependentes(null);
		planos.save(plano);
		long semLimite = contratoVigente("CT-LIVRE");
		criarDependente(semLimite, "Lia", "filha", "2012-01-01");
		criarDependente(semLimite, "Nilo", "filho", "2013-01-01");
		Map<String, Object> livre = client.post()
				.uri("/api/contratos/" + semLimite + "/renovar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"dtInicio", stamp(LocalDate.now().atStartOfDay()),
						"dtFim", stamp(LocalDate.now().plusYears(1).atStartOfDay())))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(livre).isNotNull();
		assertThat(livre.get("pontoAtencao")).isEqualTo(false);
		assertThat(listar(((Number) livre.get("id")).longValue()))
				.extracting(item -> item.get("nome"))
				.containsExactly("Lia", "Nilo");
	}

	@Test
	void acceptsDependentsOverTheLimitOnSaleAndRejectsThemWhenNotVigente() {
		plano.setLimiteDependentes(1);
		planos.save(plano);

		Map<String, Object> acima = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(contratoBody("CT-ACIMA", LocalDate.now(), LocalDate.now().plusYears(1), List.of(
						dependente("Ana", "filha", "2014-02-03"),
						dependente("Bruno", "filho", "2016-05-01"))))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(acima).isNotNull();
		assertThat(acima.get("pontoAtencao")).isEqualTo(true);
		long acimaId = ((Number) acima.get("id")).longValue();
		assertThat(listar(acimaId)).extracting(item -> item.get("nome")).containsExactly("Ana", "Bruno");
		assertThat(listarContratos()).extracting(item -> item.get("codigo")).containsExactly("CT-ACIMA");

		ResponseEntity<Map<String, String>> futuro = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(contratoBody("CT-FUTURO-DEP", LocalDate.now().plusDays(2), LocalDate.now().plusYears(1), List.of(
						dependente("Ana", "filha", "2014-02-03"))))
				.retrieve()
				.onStatus(status -> status.value() == 400, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(futuro.getStatusCode().value()).isEqualTo(400);
		assertThat(futuro.getBody()).isNotNull();
		assertThat(futuro.getBody().get("message")).contains("vigente");
		assertThat(listarContratos()).extracting(item -> item.get("codigo")).containsExactly("CT-ACIMA");

		Map<String, Object> criado = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(contratoBody("CT-COM-DEP", LocalDate.now(), LocalDate.now().plusYears(1), List.of(
						dependente("Ana", "filha", "2014-02-03"))))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(criado).isNotNull();
		assertThat(criado.get("pontoAtencao")).isEqualTo(false);
		assertThat(listar(((Number) criado.get("id")).longValue()))
				.extracting(item -> item.get("nome"))
				.containsExactly("Ana");

		plano.setLimiteDependentes(null);
		planos.save(plano);
		Map<String, Object> semLimite = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(contratoBody("CT-SEM-LIMITE", LocalDate.now(), LocalDate.now().plusYears(1), List.of(
						dependente("Caio", "filho", "2018-07-09"),
						dependente("Dora", "filha", "2020-01-01"))))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(semLimite).isNotNull();
		assertThat(semLimite.get("pontoAtencao")).isEqualTo(false);
		assertThat(contrato(((Number) semLimite.get("id")).longValue()).get("pontoAtencao")).isEqualTo(false);
	}

	private long contratoVigente(String codigo) {
		return criarContrato(codigo, LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));
	}

	private long criarContrato(String codigo, LocalDate inicio, LocalDate fim) {
		return criarContrato(codigo, inicio.atStartOfDay(), fim.atStartOfDay());
	}

	private long criarContrato(String codigo, LocalDateTime inicio, LocalDateTime fim) {
		Map<String, Object> body = client.post()
				.uri("/api/contratos")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
						"titularId", titular.getId(),
						"vendedorId", vendedor.getId(),
						"planoId", plano.getId(),
						"valor", 100f,
						"codigo", codigo,
						"dtInicio", stamp(inicio),
						"dtFim", stamp(fim)))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return ((Number) body.get("id")).longValue();
	}

	private Map<String, Object> contratoBody(String codigo, LocalDate inicio, LocalDate fim, List<Map<String, String>> dependentes) {
		return Map.of(
				"titularId", titular.getId(),
				"vendedorId", vendedor.getId(),
				"planoId", plano.getId(),
				"valor", 100f,
				"codigo", codigo,
				"dtInicio", stamp(inicio.atStartOfDay()),
				"dtFim", stamp(fim.atStartOfDay()),
				"dependentes", dependentes);
	}

	private Map<String, Object> criarDependente(long contratoId, String nome, String parentesco, String dtNasc) {
		Map<String, Object> body = client.post()
				.uri("/api/contratos/" + contratoId + "/dependentes")
				.contentType(MediaType.APPLICATION_JSON)
				.body(dependente(nome, parentesco, dtNasc))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

	private ResponseEntity<Map<String, String>> postDependente(
			long contratoId,
			String nome,
			String parentesco,
			String dtNasc,
			int status) {
		ResponseEntity<Map<String, String>> response = client.post()
				.uri("/api/contratos/" + contratoId + "/dependentes")
				.contentType(MediaType.APPLICATION_JSON)
				.body(dependente(nome, parentesco, dtNasc))
				.retrieve()
				.onStatus(httpStatus -> httpStatus.value() == status, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(response.getStatusCode().value()).isEqualTo(status);
		return response;
	}

	private ResponseEntity<Map<String, String>> putDependente(long contratoId, long dependenteId, int status) {
		ResponseEntity<Map<String, String>> response = client.put()
				.uri("/api/contratos/" + contratoId + "/dependentes/" + dependenteId)
				.contentType(MediaType.APPLICATION_JSON)
				.body(dependente("Alterado", "outro", "2000-01-01"))
				.retrieve()
				.onStatus(httpStatus -> httpStatus.value() == status, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(response.getStatusCode().value()).isEqualTo(status);
		return response;
	}

	private ResponseEntity<Map<String, String>> deleteDependente(long contratoId, long dependenteId, int status) {
		ResponseEntity<Map<String, String>> response = client.delete()
				.uri("/api/contratos/" + contratoId + "/dependentes/" + dependenteId)
				.retrieve()
				.onStatus(httpStatus -> httpStatus.value() == status, (req, res) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
		assertThat(response.getStatusCode().value()).isEqualTo(status);
		return response;
	}

	private List<Map<String, Object>> listar(long contratoId) {
		List<Map<String, Object>> body = client.get()
				.uri("/api/contratos/" + contratoId + "/dependentes")
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

	private Map<String, Object> contrato(long contratoId) {
		Map<String, Object> body = client.get()
				.uri("/api/contratos/" + contratoId)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

	private List<Map<String, Object>> listarContratos() {
		List<Map<String, Object>> body = client.get()
				.uri("/api/titulares/" + titular.getId() + "/contratos")
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		assertThat(body).isNotNull();
		return body;
	}

	private void encerrar(long contratoId, LocalDateTime dtFim, ContratoStatus status) {
		Contrato contrato = contratos.findById(contratoId).orElseThrow();
		if (dtFim != null) {
			contrato.setDtFim(dtFim);
		}
		contrato.setStatus(status);
		contratos.saveAndFlush(contrato);
	}

	private static Map<String, String> dependente(String nome, String parentesco, String dtNasc) {
		return Map.of("nome", nome, "parentesco", parentesco, "dtNasc", dtNasc);
	}

	private static String stamp(LocalDateTime value) {
		return value.format(ISO);
	}

}
