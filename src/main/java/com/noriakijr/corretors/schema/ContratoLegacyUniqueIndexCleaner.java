package com.noriakijr.corretors.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
		Map<String, List<String>> constraints = uniqueConstraints(connection, table);
		Map<String, List<String>> indexes = uniqueIndexes(connection, table);
		if (constraints.isEmpty() && indexes.isEmpty()) {
			return;
		}
		Set<String> lockedColumns = new LinkedHashSet<>();
		constraints.values().forEach(lockedColumns::addAll);
		indexes.values().forEach(lockedColumns::addAll);
		// Drop the foreign key before the unique constraint. H2 otherwise keeps the
		// unique index and reassigns it to the foreign key, so duplicates still fail.
		List<ForeignKey> foreignKeys = foreignKeys(connection, table, lockedColumns);
		for (ForeignKey foreignKey : foreignKeys) {
			execute(connection, "alter table " + quote(table) + " drop constraint " + quote(foreignKey.name()));
			log.info("Dropped foreign key {} on {} before removing its legacy unique index", foreignKey.name(), table);
		}
		for (String constraint : constraints.keySet()) {
			execute(connection, "alter table " + quote(table) + " drop constraint " + quote(constraint));
			log.info("Dropped legacy unique constraint {} on {}", constraint, table);
		}
		for (String index : uniqueIndexes(connection, table).keySet()) {
			execute(connection, "drop index " + quote(index));
			log.info("Dropped legacy unique index {} on {}", index, table);
		}
		for (ForeignKey foreignKey : foreignKeys) {
			execute(connection, foreignKey.recreateSql(table));
			log.info("Restored foreign key {} on {}", foreignKey.name(), table);
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

	private static Map<String, List<String>> uniqueConstraints(Connection connection, String table) throws SQLException {
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
		return coveringOnlyLegacyColumns(columnsByConstraint);
	}

	private static Map<String, List<String>> uniqueIndexes(Connection connection, String table) throws SQLException {
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
		return coveringOnlyLegacyColumns(columnsByIndex);
	}

	private static List<ForeignKey> foreignKeys(Connection connection, String table, Set<String> lockedColumns)
			throws SQLException {
		Map<String, ForeignKey> keys = new LinkedHashMap<>();
		try (ResultSet rows = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, table)) {
			while (rows.next()) {
				String name = rows.getString("FK_NAME");
				String column = rows.getString("FKCOLUMN_NAME");
				String pkTable = rows.getString("PKTABLE_NAME");
				String pkColumn = rows.getString("PKCOLUMN_NAME");
				int updateRule = rows.getInt("UPDATE_RULE");
				int deleteRule = rows.getInt("DELETE_RULE");
				short sequence = rows.getShort("KEY_SEQ");
				if (name == null || column == null) {
					continue;
				}
				ForeignKey foreignKey = keys.computeIfAbsent(name,
						ignored -> new ForeignKey(name, pkTable, updateRule, deleteRule));
				foreignKey.addColumn(sequence, column, pkColumn);
			}
		}
		List<ForeignKey> legacy = new ArrayList<>();
		for (ForeignKey foreignKey : keys.values()) {
			if (!foreignKey.fkColumns().isEmpty() && lockedColumns.containsAll(foreignKey.fkColumns())) {
				legacy.add(foreignKey);
			}
		}
		return legacy;
	}

	private static Map<String, List<String>> coveringOnlyLegacyColumns(Map<String, List<String>> columnsByName) {
		Map<String, List<String>> matches = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : columnsByName.entrySet()) {
			if (!entry.getValue().isEmpty() && LEGACY_UNIQUE_COLUMNS.containsAll(entry.getValue())) {
				matches.put(entry.getKey(), entry.getValue());
			}
		}
		return matches;
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

	private static String referentialAction(int rule) {
		return switch (rule) {
			case DatabaseMetaData.importedKeyCascade -> "cascade";
			case DatabaseMetaData.importedKeySetNull -> "set null";
			case DatabaseMetaData.importedKeySetDefault -> "set default";
			case DatabaseMetaData.importedKeyRestrict -> "restrict";
			default -> "no action";
		};
	}

	private static final class ForeignKey {

		private final String name;
		private final String pkTable;
		private final int updateRule;
		private final int deleteRule;
		private final List<Column> columns = new ArrayList<>();

		private ForeignKey(String name, String pkTable, int updateRule, int deleteRule) {
			this.name = name;
			this.pkTable = pkTable;
			this.updateRule = updateRule;
			this.deleteRule = deleteRule;
		}

		private String name() {
			return name;
		}

		private void addColumn(int sequence, String fkColumn, String pkColumn) {
			columns.add(new Column(sequence, fkColumn, pkColumn));
			columns.sort(Comparator.comparingInt(Column::sequence));
		}

		private List<String> fkColumns() {
			return columns.stream().map(column -> column.fkColumn().toUpperCase(Locale.ROOT)).toList();
		}

		private String recreateSql(String table) {
			StringBuilder sql = new StringBuilder();
			sql.append("alter table ").append(quote(table));
			sql.append(" add constraint ").append(quote(name));
			sql.append(" foreign key (");
			for (int i = 0; i < columns.size(); i++) {
				if (i > 0) {
					sql.append(", ");
				}
				sql.append(quote(columns.get(i).fkColumn()));
			}
			sql.append(") references ").append(quote(pkTable)).append(" (");
			for (int i = 0; i < columns.size(); i++) {
				if (i > 0) {
					sql.append(", ");
				}
				sql.append(quote(columns.get(i).pkColumn()));
			}
			sql.append(") on update ").append(referentialAction(updateRule));
			sql.append(" on delete ").append(referentialAction(deleteRule));
			return sql.toString();
		}

	}

	private record Column(int sequence, String fkColumn, String pkColumn) {
	}

}
