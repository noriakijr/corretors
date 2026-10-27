package com.noriakijr.corretors;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.noriakijr.corretors.model.Contato;
import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.model.Endereco;
import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.TitularRepository;
import com.noriakijr.corretors.repository.VendedorRepository;

import legacy.schema.LegacyContrato;

@SpringBootTest
class LegacyContratoUniqueIndexTest {

	private static final Path DIRECTORY = createDirectory();

	@Autowired
	private TitularRepository titulares;

	@Autowired
	private VendedorRepository vendedores;

	@Autowired
	private ContratoRepository contratos;

	@Autowired
	private DataSource dataSource;

	@DynamicPropertySource
	static void legacyFileDatabase(DynamicPropertyRegistry registry) {
		seedLegacySchema(url());
		registry.add("spring.datasource.url", LegacyContratoUniqueIndexTest::url);
	}

	@Test
	void secondContractForSameTitularAndVendedorIsStored() throws Exception {
		Titular titular = new Titular();
		titular.setNome("Carlos Lima");
		titular = titulares.save(titular);

		Vendedor vendedor = new Vendedor();
		vendedor.setNome("Marina Alves");
		vendedor = vendedores.save(vendedor);

		Contrato first = contract(titular, vendedor, "CT-1");
		Contrato second = contract(titular, vendedor, "CT-2");
		first = contratos.saveAndFlush(first);
		second = contratos.saveAndFlush(second);

		assertThat(first.getId()).isNotEqualTo(second.getId());
		assertThat(contratos.findByTitularIdAndStatus(titular.getId(), ContratoStatus.ATIVO))
				.extracting(Contrato::getCodigo)
				.containsExactlyInAnyOrder("CT-1", "CT-2");

		List<String> uniqueColumns = uniqueIndexColumns();
		assertThat(uniqueColumns).doesNotContain("TITULAR_ID", "VENDEDOR_ID", "PLANO_HISTORICO_ID");
		assertThat(uniqueColumns).contains("PAGAMENTO_ID");
		assertThat(foreignKeyColumns()).contains("TITULAR_ID", "VENDEDOR_ID", "PLANO_HISTORICO_ID", "PAGAMENTO_ID");
	}

	private List<String> foreignKeyColumns() throws Exception {
		List<String> columns = new ArrayList<>();
		try (Connection connection = dataSource.getConnection();
				ResultSet keys = connection.getMetaData().getImportedKeys(null, null, "CONTRATO")) {
			while (keys.next()) {
				String column = keys.getString("FKCOLUMN_NAME");
				if (column != null) {
					columns.add(column.toUpperCase());
				}
			}
		}
		return columns;
	}

	private List<String> uniqueIndexColumns() throws Exception {
		List<String> columns = new ArrayList<>();
		try (Connection connection = dataSource.getConnection();
				ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "CONTRATO", true, false)) {
			while (indexes.next()) {
				String column = indexes.getString("COLUMN_NAME");
				if (column != null) {
					columns.add(column.toUpperCase());
				}
			}
		}
		return columns;
	}

	private static Contrato contract(Titular titular, Vendedor vendedor, String codigo) {
		Contrato contrato = new Contrato();
		contrato.setCodigo(codigo);
		contrato.setDtAquisicao(LocalDateTime.of(2026, 10, 25, 9, 0));
		contrato.setDtInicio(LocalDateTime.of(2026, 10, 25, 0, 0));
		contrato.setDtFim(LocalDateTime.of(2027, 10, 25, 0, 0));
		contrato.setStatus(ContratoStatus.ATIVO);
		contrato.setTitular(titular);
		contrato.setVendedor(vendedor);
		return contrato;
	}

	private static Path createDirectory() {
		try {
			return Files.createTempDirectory("corretors-legacy-uk");
		}
		catch (Exception ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	private static String url() {
		return "jdbc:h2:file:" + DIRECTORY.resolve("corretors").toString().replace('\\', '/')
				+ ";DB_CLOSE_ON_EXIT=FALSE";
	}

	private static void seedLegacySchema(String url) {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.JAKARTA_JDBC_URL, url)
				.applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
				.applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
				.applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "update")
				.applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
						"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
				.applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY,
						"org.springframework.boot.hibernate.SpringImplicitNamingStrategy")
				.build();
		try (SessionFactory factory = new MetadataSources(registry)
				.addAnnotatedClass(LegacyContrato.class)
				.addAnnotatedClass(Titular.class)
				.addAnnotatedClass(Vendedor.class)
				.addAnnotatedClass(Contato.class)
				.addAnnotatedClass(Endereco.class)
				.addAnnotatedClass(Plano.class)
				.addAnnotatedClass(PlanoHistorico.class)
				.addAnnotatedClass(Pagamento.class)
				.buildMetadata()
				.buildSessionFactory()) {
			// Schema update runs while the session factory is built.
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

}
