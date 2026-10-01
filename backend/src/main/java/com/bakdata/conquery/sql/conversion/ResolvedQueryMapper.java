package com.bakdata.conquery.sql.conversion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptIdMapping;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingSelection;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.node.AndNode;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.node.DateRestrictionNode;
import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.node.NegationNode;
import com.bakdata.conquery.sql.model.node.OrNode;
import com.bakdata.conquery.sql.model.node.QueryNode;
import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;

/** Resolves the initialized backend concept-query tree into the SQL connector API. */
public final class ResolvedQueryMapper {

	private final IdColumnConfig idColumns;
	private final LegacyCompilerDialect dialect;
	private final Clock clock;
	private final String defaultPrimaryColumn;

	public ResolvedQueryMapper(
			IdColumnConfig idColumns,
			LegacyCompilerDialect dialect,
			Clock clock,
			String defaultPrimaryColumn
	) {
		this.idColumns = idColumns;
		this.dialect = dialect;
		this.clock = clock;
		this.defaultPrimaryColumn = defaultPrimaryColumn;
	}

	ResolvedQuery map(
			ConceptQuery query,
			Optional<SecondaryIdDescription> secondaryId,
			List<ResultInfo> resultInfos
	) {
		NodeContext context = new NodeContext(secondaryId, Optional.empty());
		return new ResolvedQuery(
				EntitySchemaAdapter.from(idColumns),
				mapNode(query.getRoot(), context),
				query.getResolvedDateAggregationMode() != DateAggregationMode.NONE,
				ResolvedOperationAdapter.resultColumns(resultInfos)
		);
	}

	private QueryNode mapNode(CQElement node, NodeContext context) {
		return switch (node) {
			case CQYes ignored -> new AllEntitiesNode();
			case CQAnd and -> new AndNode(and.getChildren().stream().map(child -> mapNode(child, context)).toList(),
					and.getDateAction(), and.getCreateExists().orElse(false));
			case CQOr or -> new OrNode(or.getChildren().stream().map(child -> mapNode(child, context)).toList(),
					or.getDateAction(), or.getCreateExists().orElse(false));
			case CQNegation negation -> new NegationNode(mapNode(negation.getChild(), context), negation.getDateAction());
			case CQDateRestriction restriction -> {
				DateRange range = new DateRange(Optional.ofNullable(restriction.getDateRange().getMin()),
						Optional.ofNullable(restriction.getDateRange().getMax()));
				yield new DateRestrictionNode(range, mapNode(restriction.getChild(), context.withDateRestriction(range)));
			}
			case CQExternal external -> external(external);
			case CQReusedQuery reused -> mapNode(reused.getResolvedQuery().getReusableComponents(),
					reused.isExcludeFromSecondaryId() ? context.withSecondaryId(Optional.empty()) : context);
			case CQConcept concept -> concept(concept, context);
			default -> throw new UnsupportedOperationException("SQL resolved query node is not implemented for " + node.getClass());
		};
	}

	private ConceptNode concept(CQConcept query, NodeContext context) {
		TreeConcept concept = (TreeConcept) query.getConcept();
		List<Connector> connectors = query.getTables().stream().map(table -> table.getConnector().resolve()).toList();
		List<ResolvedColumn> conceptColumns = connectors.stream()
				.map(Connector::getColumn).filter(java.util.Objects::nonNull)
				.map(id -> EntitySchemaAdapter.from(id.resolve())).toList();
		LocalDate endDate = context.dateRestriction().flatMap(DateRange::endInclusive).orElseGet(() -> LocalDate.now(clock));
		List<ResolvedSelect> conceptSelects = query.getSelects().stream().map(SelectId::resolve)
				.map(select -> ResolvedOperationAdapter.select(select, conceptColumns, endDate)).toList();
		boolean resolveConceptIds = query.getSelects().stream().map(SelectId::resolve)
				.filter(ConceptColumnSelect.class::isInstance).map(ConceptColumnSelect.class::cast)
				.anyMatch(ConceptColumnSelect::isAsIds);
		List<ConceptElement<?>> selected = query.getElements().stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList();
		List<ResolvedConnector> resolvedConnectors = query.getTables().stream()
				.map(table -> connector(query, table, concept, selected, resolveConceptIds, conceptColumns, endDate, context))
				.toList();
		return new ConceptNode(
				query.userLabel(java.util.Locale.ROOT),
				resolvedConnectors,
				conceptSelects,
				query.isAggregateEventDates() ? DateAggregationAction.MERGE : DateAggregationAction.BLOCK
		);
	}

	private ResolvedConnector connector(
			CQConcept query,
			CQTable table,
			TreeConcept concept,
			List<ConceptElement<?>> selected,
			boolean resolveConceptIds,
			List<ResolvedColumn> conceptColumns,
			LocalDate endDate,
			NodeContext context
	) {
		Connector connector = table.getConnector().resolve();
		SqlTable sqlTable = EntitySchemaAdapter.from(connector.getResolvedTable());
		org.jooq.Field<String> primary = TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), defaultPrimaryColumn);
		ResolvedColumn primaryId = new ResolvedColumn(
				sqlTable.logicalId() + ".primary-id", sqlTable, primary.getName(),
				com.bakdata.conquery.models.datasets.ColumnType.STRING, false);
		Optional<ResolvedColumn> secondaryId = Optional.empty();
		if (!query.isExcludeFromSecondaryId() && context.secondaryId().isPresent()
				&& table.hasSelectedSecondaryId(context.secondaryId().orElseThrow().getId())) {
			Column column = connector.getResolvedTable().findSecondaryIdColumn(context.secondaryId().orElseThrow().getId());
			secondaryId = Optional.of(EntitySchemaAdapter.from(column));
		}
		Optional<ResolvedColumn> connectorColumn = Optional.ofNullable(connector.getColumn())
				.map(id -> EntitySchemaAdapter.from(id.resolve()));
		List<ResolvedCondition> conditions = new ArrayList<>();
		if (connector.getCondition() != null) {
			conditions.add(ResolvedOperationAdapter.condition(connector.getCondition(), sqlTable, connectorColumn));
		}
		connectorColumn.ifPresent(column -> conditions.add(new BuiltInConditions.Presence(column, true)));
		List<ResolvedSelect> selects = table.getSelects().stream().map(SelectId::resolve)
				.map(select -> ResolvedOperationAdapter.select(select, conceptColumns, endDate)).toList();
		ConceptIdMapping mapping = new ConceptIdMapping(concept, dialect.getFunctionProvider());
		ConceptIdMappingSelection mappingSelection = new ConceptIdMappingSelection(
				mapping.source(connector), mapping.includedLocalIds(selected), mapping.includesRoot(selected),
				resolveConceptIds, concept.getLocalId());
		return new ResolvedConnector(
				connector.getName(), sqlTable, primaryId, secondaryId, validityDate(table.findValidityDate()),
				table.getFilters().stream().map(filter -> ResolvedOperationAdapter.filter(filter, endDate)).toList(),
				selects, conditions, Optional.of(mappingSelection)
		);
	}

	private static ResolvedValidityDate validityDate(ValidityDate validityDate) {
		if (validityDate == null) {
			return new ResolvedValidityDate.None();
		}
		if (validityDate.isSingleColumnDaterange()) {
			return new ResolvedValidityDate.Point(EntitySchemaAdapter.from(validityDate.getColumn().resolve()));
		}
		return new ResolvedValidityDate.Range(
				EntitySchemaAdapter.from(validityDate.getStartColumn().resolve()),
				EntitySchemaAdapter.from(validityDate.getEndColumn().resolve()));
	}

	private static ExternalNode external(CQExternal external) {
		List<ExternalEntity> entities = external.getValuesResolved().entrySet().stream()
				.map(entry -> externalEntity(external, entry.getKey(), entry.getValue().asRanges())).toList();
		return new ExternalNode(entities, external.getExtraHeaders());
	}

	private static ExternalEntity externalEntity(
			CQExternal external,
			String entityId,
			Collection<CDateRange> validityDates
	) {
		Map<String, List<String>> values = new LinkedHashMap<>();
		external.getExtrasForId(entityId).forEach(entry -> values.put(entry.getKey(), entry.getValue()));
		return new ExternalEntity(entityId, validityDates.stream().map(ResolvedQueryMapper::dateRange).toList(), values);
	}

	private static DateRange dateRange(CDateRange range) {
		return new DateRange(Optional.ofNullable(range.getMin()), Optional.ofNullable(range.getMax()));
	}

	private record NodeContext(Optional<SecondaryIdDescription> secondaryId, Optional<DateRange> dateRestriction) {
		private NodeContext withSecondaryId(Optional<SecondaryIdDescription> value) {
			return new NodeContext(value, dateRestriction);
		}

		private NodeContext withDateRestriction(DateRange value) {
			return new NodeContext(secondaryId, Optional.of(value));
		}
	}
}
