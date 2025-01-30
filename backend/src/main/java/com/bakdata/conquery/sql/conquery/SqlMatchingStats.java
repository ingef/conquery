package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Case;
import org.jooq.CaseConditionStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.ResultOrRows;
import org.jooq.Results;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;

@Slf4j
public class SqlMatchingStats {

	@NotNull
	private static Field<String> idField(Identifiable<?, ?> current) {
		return field(val(current.getId().toString()));
	}

	@NotNull
	private static Name resolveConceptFunction(TreeConcept concept) {
		return name("resolve_id_%s".formatted(concept.getName()));
	}

	@NotNull
	private static List<Field<?>> collectValidityDateFields(Connector connector, PostgreSqlFunctionProvider provider) {
		List<Field<?>> validityDates = new ArrayList<>();

		for (ValidityDate validityDate : connector.getValidityDates()) {
			if (validityDate.isSingleColumnDaterange()) {
				Column column = validityDate.getColumn().get();
				if (column.getType() == MajorTypeId.DATE) {
					validityDates.add(field(name(column.getName()), LocalDate.class));
				}
				else if (column.getType() == MajorTypeId.DATE_RANGE) {
					Field<Object> rangeField = field(name(column.getName()));

					validityDates.add(provider.lower(rangeField));
					validityDates.add(provider.upper(rangeField));
				}
			}
			else {
				validityDates.add(field(name(validityDate.getStartColumn().getColumn())));
				validityDates.add(field(name(validityDate.getEndColumn().getColumn())));
			}
		}
		return validityDates;
	}

	@NotNull
	private static Field<String> getResolveIdFunctionInvocation(TreeConcept concept, String connectorColumn, Set<String> columns) {
		List<Field<?>> params = new ArrayList<>();

		if (connectorColumn != null) {
			params.add(field(name(connectorColumn)));
		}
		else {
			params.add(inline(null, String.class));
		}

		columns.stream().sorted().map(nm -> field(name(nm))).forEachOrdered(params::add);

		return function(resolveConceptFunction(concept), String.class, params);
	}

	public void collectMatchingStatsForConcept(TreeConcept concept, SqlFunctionProvider _provider, DSLContext dslContext) {

		PostgreSqlFunctionProvider provider = (PostgreSqlFunctionProvider) _provider;

		List<Select<?>> connectorTables =  new ArrayList<>();

		Field<Date> positiveInfinitty = provider.toDateField(provider.getMaxDateExpression());
		Field<Date> negativeInifnity = provider.toDateField(provider.getMinDateExpression());

		for (Connector connector : concept.getConnectors()) {
			String connectorColumn = null;
			if (connector.getColumn() != null) {
				connectorColumn = connector.getColumn().get().getName();
			}

			CTConditionContext context = new CTConditionContext(connectorColumn, provider);

			com.bakdata.conquery.models.datasets.Table resolvedTable = connector.getResolvedTable();
			Table<Record> tableName = table(name(resolvedTable.getName()));
			Name pid = name(resolvedTable.getPrimaryColumn().getName());

			Set<String> columns = getAuxiliaryColumns(concept);
			if (connectorColumn != null) {
				columns.remove(connectorColumn);
			}

			Field<String> resolveFunction = getResolveIdFunctionInvocation(concept, connectorColumn, columns);

			Field[] validityDatesArray = collectValidityDateFields(connector, provider).toArray(Field[]::new);

			SelectConditionStep<?> connectorTable = select(
					field(pid).as("pid"),
					least(positiveInfinitty, validityDatesArray).as("lowerBound"),
					greatest(negativeInifnity, validityDatesArray).as("upperBound"),
					resolveFunction.as("resolvedId")
			).from(tableName)
			 .where(connector.getCondition() != null ? connector.getCondition().convertToSqlCondition(context).condition() : noCondition());

			connectorTables.add(connectorTable);

		}

		Table<?> unioned = getUnioned(connectorTables);

		SelectJoinStep<Record4<String, String, Date, Date>> records =
				select(
						field(name("resolvedId"), String.class),
						field(name("pid"), String.class).as("entity"),
						// The infinities are intentionally swapped
						nullif(field(name("lowerBound"), Date.class), positiveInfinitty).as("lb"),
						nullif(field(name("upperBound"), Date.class), negativeInifnity).as("ub")
				)
						.from(unioned);

		// Results results = dslContext.fetchMany(records);



		//		for (Result<Record> result : results) {
//
//			ConceptElementId<?> resolvedId = ConceptElementId.Parser.INSTANCE.parse(result.ge());
//			resolvedId.setDomain(concept.getDomain());
//
//
//			String entityId = record.component2();
//			Date min = record.component3();
//			Date max = record.component3();
//
//		}


		//TODO might be that grouping in SQL is too complicated because we are interested in the whole tree and this currently only maps to anything that ends up being a leaf

		log.info("{}", records);

	}

	@Nullable
	private static Table getUnioned(List<Select<?>> connectorTables) {
		Select unioned = null;

		for (Select connectorTable : connectorTables) {
			if (unioned == null) {
				unioned = connectorTable;
				continue;
			}

			unioned = unioned.unionAll(connectorTable);
		}
		return table(unioned);
	}

	public void createFunctionForConcept(Concept<?> maybeTree, SqlFunctionProvider provider, DSLContext dslContext) {
		if (!(maybeTree instanceof TreeConcept concept)) {
			return;
		}

		CTConditionContext context = new CTConditionContext("value", provider);
		Name name = resolveConceptFunction(concept);

		Set<String> auxiliaryColumns = getAuxiliaryColumns(concept);
		auxiliaryColumns.remove("value");

		Field<String> forConcept = forNode(idField(concept), concept.getChildren(), context);

		List<String> params = new ArrayList<>();
		params.add("value");

		auxiliaryColumns.stream()
						.sorted()
						.forEachOrdered(params::add);

		String statement = """
				DROP FUNCTION IF EXISTS %s;
				CREATE FUNCTION %s(%s) RETURNS TEXT
				LANGUAGE SQL
				RETURN
					%s;
				""".formatted(name, name, params.stream().map("%s text"::formatted).collect(Collectors.joining(", ")), forConcept);

		dslContext.execute(statement);

		log.info("{}", statement);

		collectMatchingStatsForConcept(concept, provider, dslContext);
	}

	@NotNull
	private Set<String> getAuxiliaryColumns(TreeConcept concept) {
		return concept.getChildren().stream()
					  .map(this::collectAuxiliaryColumns)
					  .flatMap(Collection::stream)
					  .collect(Collectors.toSet());
	}

	public Field<String> createForConceptTreeNode(ConceptTreeChild current, CTConditionContext context) {
		Field<String> currentId = idField(current);

		return forNode(currentId, current.getChildren(), context);
	}

	private Set<String> collectAuxiliaryColumns(ConceptTreeChild current) {
		Set<String> auxiliaryColumns = new HashSet<>();
		if (current.getCondition() != null) {
			auxiliaryColumns.addAll(current.getCondition().auxiliaryColumns());
		}

		for (ConceptTreeChild child : current.getChildren()) {
			auxiliaryColumns.addAll(collectAuxiliaryColumns(child));
		}

		return auxiliaryColumns;
	}

	private Field<String> forNode(Field<String> currentId, List<ConceptTreeChild> children, CTConditionContext context) {
		if (children.isEmpty()) {
			return currentId;
		}

		Case decode = decode();
		CaseConditionStep<String> step = null;

		for (ConceptTreeChild child : children) {
			WhereCondition converted = child.getCondition().convertToSqlCondition(context);

			Field<String> result = createForConceptTreeNode(child, context);

			step = step == null ? decode.when(converted.condition(), result)
								: step.when(converted.condition(), result);
		}

		return step.otherwise(currentId);
	}
}
