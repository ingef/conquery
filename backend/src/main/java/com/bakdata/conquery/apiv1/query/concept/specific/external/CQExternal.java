package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Allows uploading lists of entities.
 */
@Slf4j
@CPSType(id = "EXTERNAL", base = CQElement.class)
public class CQExternal extends CQElement {

	/**
	 * List of Type-Ids of Format Columns.
	 */
	@Getter
	@NotEmpty
	private final List<String> format;

	@Getter
	@NotEmpty
	private final String[][] values;

	@Getter
	@InternalOnly
	private Map<Integer, CDateSet> valuesResolved;

	public CQExternal(@NotEmpty List<String> format, @NotEmpty String[][] values) {
		this.format = format;
		this.values = values;
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		if (valuesResolved == null) {
			throw new IllegalStateException("CQExternal needs to be resolved before creating a plan");
		}
		return new ExternalNode(context.getStorage().getDataset().getAllIdsTable(), valuesResolved);
	}


	@Override
	public void resolve(QueryResolveContext context) {
		valuesResolved = new Int2ObjectOpenHashMap<>();

		final EntityIdMap mapping = context.getNamespace().getStorage().getIdMapping();

		final DateReader dateReader = context.getConfig().getPreprocessor().getParsers().getDateReader();

		final IdMappingConfig mappingConfig = context.getConfig().getIdMapping();

		// extract dates from rows
		final Int2ObjectMap<CDateSet> rowDates = mappingConfig.readDates(values, format, dateReader);

		final int idIndex = mappingConfig.getIdIndex(format);

		final IdMappingConfig.InputMapper reader = mappingConfig.getIdMapper(format.get(idIndex));

		final List<String[]> unresolved = new ArrayList<>();

		// ignore the first row, because this is the header
		for (int rowNum = 1; rowNum < values.length; rowNum++) {
			final String[] row = values[rowNum];
			final String externalId = reader.read(row[idIndex]);

			final int resolvedId = mapping.resolve(reader.getName(), externalId);

			if (resolvedId == -1) {
				unresolved.add(row);
				continue;
			}

			if (!rowDates.containsKey(rowNum)) {
				unresolved.add(row);
				continue;
			}

			//read the dates from the row
			valuesResolved.put(resolvedId, rowDates.get(rowNum));
		}

		if (!unresolved.isEmpty()) {
			log.warn(
					"Could not resolve {} of the {} rows. Not resolved: {}",
					unresolved.size(),
					values.length - 1,
					unresolved.subList(0, Math.min(unresolved.size(), 10))
			);
		}

		if (valuesResolved.isEmpty()) {
			throw new ConqueryError.ExternalResolveEmptyError();
		}
	}


	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
	}


	@JsonIgnore
	@ValidationMethod(message = "Values and Format are not of same width.")
	public boolean isAllSameLength() {
		final int expected = format.size();
		return Arrays.stream(values).mapToInt(a -> a.length).allMatch(v -> expected == v);
	}
}
