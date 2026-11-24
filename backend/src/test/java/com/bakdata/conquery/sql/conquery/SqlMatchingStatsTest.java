package com.bakdata.conquery.sql.conquery;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptIdMapping;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import org.jooq.DSLContext;
import org.jooq.DropTableStep;
import org.jooq.Field;
import org.jooq.Meta;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

class SqlMatchingStatsTest {

	private static final ConceptId CONCEPT_ID = new ConceptId(new DatasetId("dataset"), "concept");
	private static final Name TABLE_NAME = ConceptIdMapping.tableName(CONCEPT_ID);

	@Test
	void shouldAcceptCompatibleSchemaIndependentOfColumnOrder() {
		final List<Field<?>> expectedFields = List.of(
				field(name("resolved_id"), SQLDataType.INTEGER),
				field(name("key"), SQLDataType.VARCHAR),
				field(name("flag"), SQLDataType.BOOLEAN)
		);
		final Field<?>[] actualFields = {
				field(name("flag"), SQLDataType.BOOLEAN),
				field(name("resolved_id"), SQLDataType.BIGINT),
				field(name("key"), SQLDataType.CLOB)
		};

		assertThatCode(() -> SqlMatchingStats.validateConceptIdTableSchema(TABLE_NAME, expectedFields, actualFields))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldDescribeIncompatibleSchema() {
		final List<Field<?>> expectedFields = List.of(
				field(name("resolved_id"), SQLDataType.INTEGER),
				field(name("flag"), SQLDataType.BOOLEAN)
		);
		final Field<?>[] actualFields = {
				field(name("resolved_id"), SQLDataType.VARCHAR),
				field(name("unexpected"), SQLDataType.INTEGER)
		};

		assertThatThrownBy(() -> SqlMatchingStats.validateConceptIdTableSchema(TABLE_NAME, expectedFields, actualFields))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("missing columns [flag]")
				.hasMessageContaining("unexpected columns [unexpected]")
				.hasMessageContaining("incompatible columns [resolved_id");
	}

	@Test
	void shouldReuseCompatibleExistingTableWithoutMutation() {
		final DSLContext dslContext = mock(DSLContext.class);
		final Meta meta = mock(Meta.class);
		final Table<?> existingTable = mock(Table.class);
		final TreeConcept concept = mock(TreeConcept.class);
		when(concept.getId()).thenReturn(CONCEPT_ID);
		when(concept.getChildren()).thenReturn(List.of());
		when(dslContext.meta()).thenReturn(meta);
		when(meta.getTables(TABLE_NAME)).thenReturn(List.of(existingTable));
		when(existingTable.fields()).thenReturn(new Field[]{field(name("resolved_id"), SQLDataType.INTEGER)});
		final SqlMatchingStats matchingStats = createMatchingStats(dslContext, true);

		matchingStats.createConceptIdJoinTable(concept);

		verify(dslContext, never()).dropTable(any(Name.class));
		verify(dslContext, never()).createTable(any(Name.class));
	}

	@Test
	void shouldPreserveTableOnDeleteWhenReuseIsEnabled() {
		final DSLContext dslContext = mock(DSLContext.class);
		final SqlMatchingStats matchingStats = createMatchingStats(dslContext, true);

		matchingStats.deleteConceptIdJoinTable(CONCEPT_ID);

		verifyNoInteractions(dslContext);
	}

	@Test
	void shouldDropTableOnDeleteByDefault() {
		final DSLContext dslContext = mock(DSLContext.class);
		final DropTableStep dropTableStep = mock(DropTableStep.class);
		when(dslContext.dropTable(TABLE_NAME)).thenReturn(dropTableStep);
		final SqlMatchingStats matchingStats = createMatchingStats(dslContext, false);

		matchingStats.deleteConceptIdJoinTable(CONCEPT_ID);

		verify(dropTableStep).execute();
	}

	private static SqlMatchingStats createMatchingStats(DSLContext dslContext, boolean reuseExistingConceptIdTables) {
		return new SqlMatchingStats(
				dslContext,
				mock(SqlFunctionProvider.class),
				"pid",
				1,
				0,
				reuseExistingConceptIdTables
		);
	}
}
