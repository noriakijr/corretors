package com.noriakijr.corretors.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;

/**
 * Drops unique indexes that {@code @OneToOne} left on {@code contrato}.
 * Hibernate {@code ddl-auto=update} never removes them, so a file database
 * created before {@code titular}, {@code vendedor} and {@code planoHistorico}
 * became {@code @ManyToOne} still rejects a second row for the same id.
 */
@Component
public class ContratoLegacyUniqueIndexCleaner implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(ContratoLegacyUniqueIndexCleaner.class);

	static final Set<String> LEGACY_UNIQUE_COLUMNS = Set.of(
			"TITULAR_ID",
			"VENDEDOR_ID",
			"PLANO_HISTORICO_ID");

	private static final String UNIQUE_CONSTRAINTS = """
			select tc.constraint_name, kcu.column_name
			from information_schema.table_constraints tc
			join information_schema.key_column_usage kcu
			  on tc.constraint_schema = kcu.constraint_schema
			 and tc.constraint_name = kcu.constraint_name
			 and tc.table_name = kcu.table_name
			where upper(tc.table_name) = ?
			  and tc.constraint_type = 'UNIQUE'
			""";

	private final DataSource dataSource;

	public ContratoLegacyUniqueIndexCleaner(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
		this.dataSource = dataSource;
		// Schema update runs while the factory is created, so this bean runs after it.
		if (entityManagerFactory == null) {
			throw new IllegalArgumentException("entityManagerFactory is required");
		}
	}

	@Override
	public void afterPropertiesSet() {
		try (Connection connection = dataSource.getConnection()) {
			dropLegacyUniques(connection);
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Could not drop legacy unique indexes on contrato", ex);
		}
	}

	static void dropLegacyUniques(Connection connection) throws SQLException {
		if (!"H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
			return;
		}
		String table = resolveContratoTable(connection);
		if (table == null) {
			return;
		}
		for (String constraint : uniqueConstraints(connection, table)) {
			execute(connection, "alter table " + quote(table) + " drop constraint " + quote(constraint));
			log.info("Dropped legacy unique constraint {} on {}", constraint, table);
		}
		for (String index : uniqueIndexes(connection, table)) {
			execute(connection, "drop index " + quote(index));
			log.info("Dropped legacy unique index {} on {}", index, table);
		}
	}

	private static String resolveContratoTable(Connection connection) throws SQLException {
		DatabaseMetaData meta = connection.getMetaData();
		try (ResultSet tables = meta.getTables(connection.getCatalog(), null, "%", new String[] { "TABLE" })) {
			while (tables.next()) {
				String name = tables.getString("TABLE_NAME");
				if ("CONTRATO".equalsIgnoreCase(name)) {
					return name;
				}
			}
		}
		return null;
	}

	private static List<String> uniqueConstraints(Connection connection, String table) throws SQLException {
		Map<String, List<String>> columnsByConstraint = new LinkedHashMap<>();
		try (PreparedStatement statement = connection.prepareStatement(UNIQUE_CONSTRAINTS)) {
			statement.setString(1, table.toUpperCase(Locale.ROOT));
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					String name = rows.getString("constraint_name");
					String column = rows.getString("column_name");
					if (name == null || column == null) {
						continue;
					}
					columnsByConstraint.computeIfAbsent(name, ignored -> new ArrayList<>())
							.add(column.toUpperCase(Locale.ROOT));
				}
			}
		}
		return namesCoveringOnlyLegacyColumns(columnsByConstraint);
	}

	private static List<String> uniqueIndexes(Connection connection, String table) throws SQLException {
		Map<String, List<String>> columnsByIndex = new LinkedHashMap<>();
		DatabaseMetaData meta = connection.getMetaData();
		try (ResultSet rows = meta.getIndexInfo(connection.getCatalog(), null, table, true, false)) {
			while (rows.next()) {
				String name = rows.getString("INDEX_NAME");
				String column = rows.getString("COLUMN_NAME");
				if (name == null || column == null) {
					continue;
				}
				columnsByIndex.computeIfAbsent(name, ignored -> new ArrayList<>())
						.add(column.toUpperCase(Locale.ROOT));
			}
		}
		return namesCoveringOnlyLegacyColumns(columnsByIndex);
	}

	private static List<String> namesCoveringOnlyLegacyColumns(Map<String, List<String>> columnsByName) {
		List<String> names = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : columnsByName.entrySet()) {
			if (!entry.getValue().isEmpty() && LEGACY_UNIQUE_COLUMNS.containsAll(entry.getValue())) {
				names.add(entry.getKey());
			}
		}
		return names;
	}

	private static void execute(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private static String quote(String identifier) {
		if (!identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			throw new IllegalStateException("Refusing to quote identifier " + identifier);
		}
		return "\"" + identifier + "\"";
	}

}
