package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptIdMapping;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingTableManager;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Stopwatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

@Slf4j
@Data
public class SqlMatchingStats {

    /**
     * Legacy backend implementation requires separation by source (in that case different shards). For sql it's a constant.
     */
    private static final String SQL_SOURCE_MATCHING_STATS_LABEL = "sql";

    private static final Field<String> PID_FIELD = field(name("pid"), String.class);
    private static final Field<Date> LB_FIELD = field(name("lower_bound"), Date.class);
    private static final Field<Date> UB_FIELD = field(name("upper_bound"), Date.class);
    private static final Field<Integer> CONCEPT_ID_FIELD = field(name(ConceptIdMapping.RESOLVED_ID_COLUMN), Integer.class);

    private final DSLContext dslContext;
    private final SqlFunctionProvider functionProvider;
    private final String defaultPrimaryColumn;
    private final int fetchBatchSize = 100;
    private final int matchingStatsWorkers;
    private final int matchingStatsRetries;

    private static void assignStatsToPath(
            ConceptElement<?> element,
            MatchingStats.Accumulator[] matchingStats,
            String entity,
            int minDate,
            int maxDate
    ) {
        final long hashedEntity = MatchingStats.Accumulator.hashEntity(entity);
        while (element != null) {
            int localId = element.getLocalId();
            matchingStats[localId].addEvents(hashedEntity, 1, minDate, maxDate);
            element = element.getParent();
        }
    }

    private static <T extends Record> Select<T> unionSelects(List<Select<? extends T>> connectorTableSelects) {
        Select<T> unioned = null;

        for (Select<? extends T> connectorTable : connectorTableSelects) {
            if (unioned == null) {
                unioned = (Select<T>) connectorTable;
                continue;
            }

            unioned = unioned.unionAll(connectorTable);
        }


        return unioned;
    }

    /**
     * Assembles the join table and inserts it into the database.
     *
     * @param concept
     */
    public void createConceptIdJoinTable(TreeConcept concept) {
        ConceptIdMapping mapping = new ConceptIdMapping(concept, functionProvider);
        new ConceptIdMappingTableManager(dslContext).recreate(mapping.mappingTable());
    }

    @NotNull
    private Field<Date>[] collectValidityDateFields(Connector connector) {
        List<Field<Date>> validityDates = new ArrayList<>();

        for (ValidityDate validityDate : connector.getValidityDates()) {
            if (validityDate.isSingleColumnDaterange()) {
                Column column = validityDate.getColumn().get();
                validityDates.add(field(name(column.getName()), Date.class));
            } else {
                validityDates.add(field(name(validityDate.getStartColumn().getColumn()), Date.class));
                validityDates.add(field(name(validityDate.getEndColumn().getColumn()), Date.class));
            }

        }
        return (Field<Date>[]) validityDates.toArray(Field[]::new);
    }

    private void assignStats(TreeConcept concept, MatchingStats.Accumulator[] matchingStats) {
        for (int localId = 0; localId < matchingStats.length; localId++) {
            ConceptElement<?> element = concept.getElementByLocalId(localId);
            element.setMatchingStats(MatchingStats.singleEntry(SQL_SOURCE_MATCHING_STATS_LABEL, matchingStats[localId].toEntry()));
        }
    }

    /**
     * @implSpec array uses {@link TreeConcept#getLocalId()} as index.
     */
    @NotNull
    private MatchingStats.Accumulator[] readStats(TreeConcept concept, SelectJoinStep<Record4<Integer, String, Integer, Integer>> selectJoinStep) {
        MatchingStats.Accumulator[] matchingStats = new MatchingStats.Accumulator[concept.countElements()];

        for (int index = 0; index < matchingStats.length; index++) {
            matchingStats[index] = new MatchingStats.Accumulator();
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        log.info("BEGIN fetching matching stats for {}", concept.getId());
        log.trace("{}", selectJoinStep);

        try (Stream<Record4<Integer, String, Integer, Integer>> stream = selectJoinStep.fetchSize(fetchBatchSize).stream()) {
            //TODO if this is still too slow, access the raw results somehow instead of boxing etc.
            //TODO We can also try and rewrite the logic to scan every table once with all connectors for that table at the same time; though that might create more memory pressure.

            stream.forEach(
                    record -> {
                        Integer rawId = record.value1();
                        ConceptElement<?> resolvedId;
                        if (rawId == null) {
                            resolvedId = concept;
                        } else {
                            resolvedId = concept.getElementByLocalId(rawId);
                        }

                        String entity = record.value2();
                        Integer min = record.value3();
                        Integer max = record.value4();

                        int minDate = min != null ? min : Integer.MAX_VALUE;
                        int maxDate = max != null ? max : Integer.MIN_VALUE;

                        assignStatsToPath(resolvedId, matchingStats, entity, minDate, maxDate);
                    });
        }

        log.debug("DONE fetching matching stats for {} within {}", concept.getId(), stopwatch);

        return matchingStats;
    }

    public void collectMatchingStatsForConcept(TreeConcept concept, int tries) {
        int remainingTries = tries;
        while (true) {
            try {
                SelectJoinStep<Record4<Integer, String, Integer, Integer>> matchingStatsStatement = createMatchingStatsStatement(concept);
                MatchingStats.Accumulator[] matchingStats = readStats(concept, matchingStatsStatement);
                assignStats(concept, matchingStats);
                return;
            } catch (DataAccessException e) {
                if (remainingTries == 0) {
                    log.error("Failed to collect matching stats for concept {}. No retries remaining.", concept.getId(), e);
                    throw e;
                }

                log.debug("Failed to collect matching stats for concept {}. Retrying.", concept.getId(), (Exception) (log.isTraceEnabled() ? e : null));
                remainingTries--;
            }
        }
    }

    @NotNull
    private SelectJoinStep<Record4<Integer, String, Integer, Integer>> createMatchingStatsStatement(TreeConcept concept) {

        List<Select<? extends Record>> connectorTables = new ArrayList<>();
        ConceptIdMapping mapping = new ConceptIdMapping(concept, functionProvider);

        Field<Date> positiveInfinity = functionProvider.getMaxDateExpression();
        Field<Date> negativeInfinity = functionProvider.getMinDateExpression();

        for (Connector connector : concept.getConnectors()) {

            Field<Date>[] validityDates = collectValidityDateFields(connector);

            Name tableName = name(connector.getResolvedTable().getName());

            Condition condition = noCondition();

            if (connector.getColumn() != null) {
                condition = field(name(tableName, name(connector.getColumn().getColumn()))).isNotNull();
            }

            if (connector.getCondition() != null) {
                CTConditionContext context = CTConditionContext.forConnector(connector, functionProvider);
                condition = condition.and(connector.getCondition().convertToSqlCondition(context).condition());
            }

            SelectConditionStep<? extends Record> connectorTable =
                    dslContext.select(
                                    TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), defaultPrimaryColumn).as(PID_FIELD),
                                    // The infinities are intentionally swapped
                                    least(positiveInfinity, validityDates).as(LB_FIELD),
                                    greatest(negativeInfinity, validityDates).as(UB_FIELD),
                                    CONCEPT_ID_FIELD)
                            .from(table(tableName))
                            .leftJoin(mapping.table())
                            // join onto the concept-ids table to assign the most specific id.
                            .on(mapping.joinCondition(connector))
                            .where(condition);

            connectorTables.add(connectorTable);
        }

        Name ct_name = name("connector_tables");
        CommonTableExpression<?> unioned = ct_name.as(unionSelects(connectorTables));

        SelectJoinStep<Record4<Integer, String, Integer, Integer>> records = dslContext.with(unioned).select(unioned.field(CONCEPT_ID_FIELD), PID_FIELD,
                // The infinities are intentionally swapped
                functionProvider.cast(nullif(unioned.field(LB_FIELD), positiveInfinity), SQLDataType.INTEGER).as(LB_FIELD), functionProvider.cast(nullif(unioned.field(UB_FIELD), negativeInfinity), SQLDataType.INTEGER).as(UB_FIELD)).from(ct_name);


        return records;
    }

    public void deleteConceptIdJoinTable(ConceptId concept) {
        Name tableName = ConceptIdMapping.tableName(concept);
        log.debug("Trying to delete id-table {}", tableName);
        new ConceptIdMappingTableManager(dslContext).delete(tableName);
    }

}
