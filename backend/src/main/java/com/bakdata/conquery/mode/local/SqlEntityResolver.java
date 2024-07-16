package com.bakdata.conquery.mode.local;

import static com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolverUtil.collectExtraData;
import static com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolverUtil.readDates;
import static com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolverUtil.verifyOnlySingles;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;
import static org.jooq.impl.DSL.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolver;
import com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolverUtil;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.io.IdColumnUtil;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

@RequiredArgsConstructor
public class SqlEntityResolver implements EntityResolver {

	private static final Name IS_RESOLVED_ALIAS = name("is_resolved");
	private static final Name UNRESOLVED_CTE = name("ids_unresolved");
	public static final String ROW_INDEX = "row";

	private final IdColumnConfig idColumns;
	private final DSLContext context;
	private final SqlExecutionService executionService;

	@Override
	public ResolveStatistic resolveEntities(
			@NotEmpty String[][] values,
			List<String> format,
			EntityIdMap mapping,
			IdColumnConfig idColumnConfig,
			DateReader dateReader,
			boolean onlySingles
	) {
		final Map<String, CDateSet> resolved = new HashMap<>();
		final List<String[]> unresolvedDate = new ArrayList<>();
		final List<String[]> unresolvedId = new ArrayList<>();

		// extract dates from rows
		final CDateSet[] rowDates = readDates(values, format, dateReader);

		// Extract extra data from rows by Row, to be collected into by entities
		// Row -> Column -> Value
		final Map<String, String>[] extraDataByRow = EntityResolverUtil.readExtras(values, format);

		final List<Function<String[], EntityIdMap.ExternalId>> readers = IdColumnUtil.getIdReaders(format, idColumnConfig.getIdMappers());

		// We will not be able to resolve anything...
		if (readers.isEmpty()) {
			return EntityResolver.ResolveStatistic.forEmptyReaders(values);
		}

		// Entity -> Column -> Values
		final Map<String, Map<String, List<String>>> extraDataByEntity = new HashMap<>();

		// all IDs of this map had at least a matching reader
		final Map<Integer, IdResolveInfo> resolvedIdsMap = resolveIds(values, readers);

		// ignore the first row, because this is the header
		for (int rowNum = 1; rowNum < values.length; rowNum++) {

			final String[] row = values[rowNum];

			final IdResolveInfo idResolveInfo = resolvedIdsMap.get(rowNum);
			if (idResolveInfo == null) {
				// row had no matching reader
				unresolvedId.add(row);
				continue;
			}

			// external ID could not be resolved internally
			if (!idResolveInfo.isResolved()) {
				unresolvedDate.add(row);
				continue;
			}

			final String resolvedId = idResolveInfo.externalId();

			if (rowDates[rowNum] == null) {
				unresolvedDate.add(row);
				continue;
			}

			// read the dates from the row
			resolved.put(resolvedId, rowDates[rowNum]);

			// Entity was resolved for row, so we collect the data.
			collectExtraData(extraDataByRow, rowNum, extraDataByEntity, resolvedId);
		}

		verifyOnlySingles(onlySingles, extraDataByEntity);
		return new EntityResolver.ResolveStatistic(resolved, extraDataByEntity, unresolvedDate, unresolvedId);

	}

	/**
	 * Create a SQL query like this
	 * <pre>
	 *     {@code
	 *      with "ids_unresolved" as (select 1   as "row",
	 *                                      '1'  as "primary_id"
	 *                                -- will select more ids here via union all)
	 * 		select "row",
	 * 		       "primary_id",
	 * 		       case
	 * 		           when "persons"."id" is not null then true
	 * 		           else false
	 * 		           end as "is_resolved"
	 * 		from "persons"
	 * 		         join "ids_unresolved"
	 * 		              on "primary_id" = "persons"."id"
	 * 		where "primary_id" = "pid"
	 *     }
	 * </pre>
	 * <p>
	 * For each ID, that had a matching reader, it will return an entry in the map with row number -> IdResolveInfo.
	 */
	private Map<Integer, IdResolveInfo> resolveIds(String[][] values, List<Function<String[], EntityIdMap.ExternalId>> readers) {

		CommonTableExpression<?> unresolvedCte = createUnresolvedCte(values, readers);

		Field<Integer> rowIndex = field(name(ROW_INDEX), Integer.class);
		Field<String> externalPrimaryColumn = field(name(SharedAliases.PRIMARY_COLUMN.getAlias()), String.class);
		Field<String> innerPrimaryColumn = field(name(idColumns.findPrimaryIdColumn().getField()), String.class);
		Field<Boolean> isResolved = when(innerPrimaryColumn.isNotNull(), val(true))
				.otherwise(false)
				.as(IS_RESOLVED_ALIAS);

		Table<Record> allIdsTable = table(name(idColumns.getTable()));
		SelectConditionStep<Record3<Integer, String, Boolean>> resolveIdsQuery =
				context.with(unresolvedCte)
					   .select(rowIndex, externalPrimaryColumn, isResolved)
					   .from(allIdsTable)
					   .innerJoin(unresolvedCte)
					   .on(externalPrimaryColumn.eq(innerPrimaryColumn))
					   .where(externalPrimaryColumn.eq(innerPrimaryColumn));

		return executionService.fetchStream(resolveIdsQuery)
							   .collect(Collectors.toMap(
									   record -> record.get(rowIndex),
									   record -> new IdResolveInfo(record.get(externalPrimaryColumn), record.get(isResolved))
							   ));
	}

	private CommonTableExpression<?> createUnresolvedCte(String[][] values, List<Function<String[], EntityIdMap.ExternalId>> readers) {

		List<Select<Record2<Integer, String>>> selects = new ArrayList<>(values.length);
		for (int i = 1; i < values.length; i++) {

			final String[] row = values[i];

			String resolvedId = null;
			for (Function<String[], EntityIdMap.ExternalId> reader : readers) {
				final EntityIdMap.ExternalId externalId = reader.apply(row);
				resolvedId = externalId.getId();
			}

			// no matching reader found
			if (resolvedId == null) {
				continue;
			}

			Field<Integer> rowIndex = val(i).as(ROW_INDEX);
			Field<String> externalPrimaryColumn = val(resolvedId).as(SharedAliases.PRIMARY_COLUMN.getAlias());
			selects.add(context.select(rowIndex, externalPrimaryColumn));
		}

		return UNRESOLVED_CTE.as(selects.stream().reduce(Select::unionAll).orElseThrow(IllegalStateException::new));
	}


	private record IdResolveInfo(String externalId, boolean isResolved) {
	}

}
