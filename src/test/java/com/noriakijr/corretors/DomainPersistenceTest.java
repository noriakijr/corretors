package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.noriakijr.corretors.model.Contato;
import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.model.Dependente;
import com.noriakijr.corretors.model.Endereco;
import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.PagamentoType;
import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Usuario;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.DependenteRepository;
import com.noriakijr.corretors.repository.PagamentoRepository;
import com.noriakijr.corretors.repository.ParcelaRepository;
import com.noriakijr.corretors.repository.PlanoHistoricoRepository;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.repository.TitularRepository;
import com.noriakijr.corretors.repository.UsuarioRepository;
import com.noriakijr.corretors.repository.VendedorRepository;

@SpringBootTest
@Transactional
class DomainPersistenceTest {

	@Autowired
	private TitularRepository titulares;

	@Autowired
	private DependenteRepository dependentes;

	@Autowired
	private VendedorRepository vendedores;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PlanoRepository planos;

	@Autowired
	private PlanoHistoricoRepository historicos;

	@Autowired
	private PagamentoRepository pagamentos;

	@Autowired
	private ParcelaRepository parcelas;

	@Autowired
	private ContratoRepository contratos;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void persistsTheBrokerDomain() {
		Endereco endereco = new Endereco();
		endereco.setPais("Brasil");
		endereco.setEstado("MG");
		endereco.setCidade("Belo Horizonte");
		endereco.setBairro("Savassi");
		endereco.setRua("Rua da Bahia");
		endereco.setNumero("1200");
		endereco.setLatitude("-19.93");
		endereco.setLongitude("-43.93");

		Contato contato = new Contato();
		contato.setTelFixo("3130000000");
		contato.setTelComercial("3130000001");
		contato.setTelFax("3130000002");
		contato.setCelular("31988887777");
		contato.setEmail("titular@example.com");
		contato.setEndereco(endereco);

		Titular titular = new Titular();
		titular.setNome("Carlos Lima");
		titular.setCpf("98765432100");
		titular.setRg("MG999");
		titular.setDtNasc(LocalDate.of(1985, 5, 20));
		titular.setCartaoEnv(true);
		titular.setSeguroVidaEnv(false);
		titular.setContato(contato);
		titular = titulares.save(titular);

		Dependente dependente = new Dependente();
		dependente.setNome("Lia Lima");
		dependente.setCpf("11122233344");
		dependente.setDtNasc(LocalDate.of(2012, 8, 3));
		dependente.setTitular(titular);
		dependente = dependentes.save(dependente);

		Vendedor vendedor = new Vendedor();
		vendedor.setNome("Marina Alves");
		vendedor.setCpf("55566677788");
		vendedor = vendedores.save(vendedor);

		Usuario usuario = new Usuario();
		usuario.setNome("Operador");
		usuario.setUsername("operador");
		usuario.setPassword("segredo");
		usuario = usuarios.save(usuario);

		Plano plano = new Plano();
		plano.setNome("Plano Familia");
		plano.setDescricao("Grupo familiar");
		plano = planos.save(plano);

		PlanoHistorico historico = new PlanoHistorico();
		historico.setPlano(plano);
		historico.setValor(249.9f);
		historico.setDtModif(LocalDateTime.of(2026, 10, 25, 12, 0));
		historico = historicos.save(historico);

		Pagamento pagamento = new Pagamento();
		pagamento.setDescricao("Aquisicao");
		pagamento.setTipoPag(PagamentoType.DINHEIRO);
		pagamento.setVlrBruto(249.9);
		pagamento.setVlrDesconto(0.0);
		pagamento.setVlrLiquido(249.9);
		pagamento = pagamentos.save(pagamento);

		Parcela parcela = new Parcela();
		parcela.setVlrParcela(249.9);
		parcela.setDtParcela(LocalDateTime.of(2026, 11, 1, 0, 0));
		parcela.setPagamento(pagamento);
		parcela = parcelas.save(parcela);

		Contrato contrato = new Contrato();
		contrato.setCodigo("CT-100");
		contrato.setDescricao("Contrato familiar");
		contrato.setDtAquisicao(LocalDateTime.of(2026, 10, 25, 9, 30));
		contrato.setDtInicio(LocalDateTime.of(2026, 10, 25, 0, 0));
		contrato.setDtFim(LocalDateTime.of(2027, 10, 25, 0, 0));
		contrato.setStatus(ContratoStatus.ATIVO);
		contrato.setTitular(titular);
		contrato.setVendedor(vendedor);
		contrato.setPlanoHistorico(historico);
		contrato.setPagamento(pagamento);
		contrato = contratos.save(contrato);

		Contrato stored = contratos.findById(contrato.getId()).orElseThrow();
		assertThat(stored.getTitular().getNome()).isEqualTo("Carlos Lima");
		assertThat(stored.getTitular().getContato().getEmail()).isEqualTo("titular@example.com");
		assertThat(stored.getTitular().getContato().getEndereco().getCidade()).isEqualTo("Belo Horizonte");
		assertThat(stored.getVendedor().getNome()).isEqualTo("Marina Alves");
		assertThat(stored.getPlanoHistorico().getPlano().getNome()).isEqualTo("Plano Familia");
		assertThat(stored.getPlanoHistorico().getValor()).isEqualTo(249.9f);
		assertThat(stored.getPagamento().getTipoPag()).isEqualTo(PagamentoType.DINHEIRO);
		assertThat(stored.getStatus()).isEqualTo(ContratoStatus.ATIVO);
		assertThat(stored.getDtInicio()).isEqualTo(LocalDateTime.of(2026, 10, 25, 0, 0));
		assertThat(stored.getDtFim()).isEqualTo(LocalDateTime.of(2027, 10, 25, 0, 0));
		assertThat(dependentes.findById(dependente.getId()).orElseThrow().getTitular().getId()).isEqualTo(titular.getId());
		assertThat(parcelas.findById(parcela.getId()).orElseThrow().getPagamento().getId()).isEqualTo(pagamento.getId());
		assertThat(usuarios.findById(usuario.getId()).orElseThrow().getUsername()).isEqualTo("operador");

		String codigo = jdbc.queryForObject(
				"select tipo_pag from pagamento where id = ?",
				String.class,
				pagamento.getId());
		assertThat(codigo).isEqualTo("DINH");
	}

	@Test
	void allowsMultipleActiveContractsForSameTitular() {
		Titular titular = new Titular();
		titular.setNome("Multi Ativo");
		titular = titulares.save(titular);

		Vendedor vendedor = new Vendedor();
		vendedor.setNome("Vendedor");
		vendedor = vendedores.save(vendedor);

		Plano saude = new Plano();
		saude.setNome("Saude");
		saude = planos.save(saude);

		Plano vida = new Plano();
		vida.setNome("Vida");
		vida = planos.save(vida);

		PlanoHistorico histSaude = new PlanoHistorico();
		histSaude.setPlano(saude);
		histSaude.setValor(100f);
		histSaude.setDtModif(LocalDateTime.of(2026, 1, 1, 0, 0));
		histSaude = historicos.save(histSaude);

		PlanoHistorico histVida = new PlanoHistorico();
		histVida.setPlano(vida);
		histVida.setValor(80f);
		histVida.setDtModif(LocalDateTime.of(2026, 1, 1, 0, 0));
		histVida = historicos.save(histVida);

		Contrato c1 = new Contrato();
		c1.setCodigo("CT-S");
		c1.setDtAquisicao(LocalDateTime.of(2026, 1, 1, 10, 0));
		c1.setDtInicio(LocalDateTime.of(2026, 1, 1, 0, 0));
		c1.setDtFim(LocalDateTime.of(2027, 1, 1, 0, 0));
		c1.setStatus(ContratoStatus.ATIVO);
		c1.setTitular(titular);
		c1.setVendedor(vendedor);
		c1.setPlanoHistorico(histSaude);
		c1 = contratos.save(c1);

		Contrato c2 = new Contrato();
		c2.setCodigo("CT-V");
		c2.setDtAquisicao(LocalDateTime.of(2026, 2, 1, 10, 0));
		c2.setDtInicio(LocalDateTime.of(2026, 2, 1, 0, 0));
		c2.setDtFim(LocalDateTime.of(2027, 2, 1, 0, 0));
		c2.setStatus(ContratoStatus.ATIVO);
		c2.setTitular(titular);
		c2.setVendedor(vendedor);
		c2.setPlanoHistorico(histVida);
		c2 = contratos.save(c2);

		assertThat(contratos.findByTitularIdAndStatus(titular.getId(), ContratoStatus.ATIVO))
				.hasSize(2)
				.extracting(Contrato::getCodigo)
				.containsExactlyInAnyOrder("CT-S", "CT-V");
		assertThat(c1.getId()).isNotEqualTo(c2.getId());
	}

}
