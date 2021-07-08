package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
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

		final Map<String, FormatColumn> stringFormatColumnMap = context.getConfig().getIdMapping().getFormatColumns();

		final FormatColumn[] resolvedFormats = resolveFormats(stringFormatColumnMap);

		final DateFormat dateFormat = getDateFormat(resolvedFormats);

		final int idIndex = getIdIndex(resolvedFormats);
		final IdColumn idColumn = (IdColumn) resolvedFormats[idIndex];


		final EncodedDictionary primary = context.getNamespace().getStorage().getPrimaryDictionary();
		final DateFormats dateFormats = context.getConfig().getPreprocessor().getParsers().getDateFormats();

		List<String[]> unresolved = new ArrayList<>();

		// ignore the first row, because this is the header
		for (int i = 1; i < values.length; i++) {
			final String[] row = values[i];
			final String[] externalId = idColumn.read(row, idIndex);

			final Optional<String> id = mapping.toInternal(externalId);

			if (id.isEmpty()) {
				continue;
			}

			final int resolvedId;

			if ((resolvedId = primary.getId(id.get())) == -1) {
				unresolved.add(row);
				continue;
			}

			//read the dates from the row
			try {
				CDateSet dates = dateFormat.readDates(resolvedFormats, row, dateFormats);

				valuesResolved.put(resolvedId, dates);
			}
			catch (Exception e) {
				log.warn("Failed to parse Date from {}", row, e);
				unresolved.add(row);
			}
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

	@NotNull
	private DateFormat getDateFormat(FormatColumn[] resolvedFormats) {
		DateFormat dateFormat = null;

		for (FormatColumn col : resolvedFormats) {
			if (!(col instanceof DateColumn)) {
				continue;
			}

			if (dateFormat != null && !dateFormat.equals(((DateColumn) col).getFormat())) {
				throw new IllegalStateException("Use of multiple Date Formats.");
			}

			dateFormat = ((DateColumn) col).getFormat();
		}

		return dateFormat == null ? DateFormat.ALL : dateFormat;
	}

	private int getIdIndex(FormatColumn[] resolvedFormats) {
		for (int index = 0; index < resolvedFormats.length; index++) {
			if (resolvedFormats[index] instanceof IdColumn) {
				return index;
			}
		}

		throw new IllegalStateException("No IdColumn provided");
	}

	@NotNull
	private FormatColumn[] resolveFormats(Map<String, FormatColumn> stringFormatColumnMap) {
		final List<FormatColumn> resolvedFormats = new ArrayList<>();

		for (String columnType : format) {
			FormatColumn formatColumn = stringFormatColumnMap.get(columnType);

			if (formatColumn == null) {
				throw new IllegalStateException(String.format("Don't know of format %s", columnType));
			}

			resolvedFormats.add(formatColumn);
		}
		return resolvedFormats.toArray(FormatColumn[]::new);
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
