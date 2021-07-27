package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;

import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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


	private static Int2ObjectMap<CDateSet> readDates(String[][] values, List<String> format, DateReader dateReader, FrontendConfig.UploadConfig queryUpload) {
		Int2ObjectMap<CDateSet> out = new Int2ObjectAVLTreeMap<>();


		List<DateFormat> dateFormats = format.stream().map(queryUpload::resolveDateFormat).collect(Collectors.toList());

		//validate structures

		for (int row = 1; row < values.length; row++) {
			try {
				for (int col = 0; col < dateFormats.size(); col++) {
					final DateFormat dateFormat = dateFormats.get(col);

					if(dateFormat == null){
						continue;
					}

					CDateSet dates = CDateSet.create();

					dateFormat.readDates(values[row][col], dateReader, dates);

					if (dates.isEmpty()) {
						continue;
					}

					out.put(row, dates);
				}
			}
			catch (Exception e) {
				log.warn("Failed to parse Date from {}", row, e);
			}
		}

		return out;
	}

	@Override
	public void resolve(QueryResolveContext context) {
		final ResolveStatistic resolved = resolveEntities(values, format, context.getNamespace().getStorage().getIdMapping(), context.getConfig().getFrontend().getQueryUpload(), context.getConfig().getPreprocessor().getParsers().getDateReader());

		if (resolved.getResolved().isEmpty()) {
			throw new ConqueryError.ExternalResolveEmptyError();
		}

		if (!resolved.getUnreadableDate().isEmpty()) {
			log.warn(
					"Could not read dates {} of the {} rows. Not resolved: {}",
					resolved.getUnreadableDate().size(),
					values.length - 1,
					resolved.getUnreadableDate().subList(0, Math.min(resolved.getUnreadableDate().size(), 10))
			);
		}

		if (!resolved.getUnresolvedId().isEmpty()) {
			log.warn(
					"Could not read dates {} of the {} rows. Not resolved: {}",
					resolved.getUnresolvedId().size(),
					values.length - 1,
					resolved.getUnresolvedId().subList(0, Math.min(resolved.getUnresolvedId().size(), 10))
			);
		}

		valuesResolved = resolved.getResolved();
	}

	@Data
	public static class ResolveStatistic {

		@JsonIgnore
		private final Map<Integer, CDateSet> resolved;

		private final List<String[]> unreadableDate;
		private final List<String[]> unresolvedId;

	}

	public static ResolveStatistic resolveEntities(@NotEmpty String[][] values, @NotEmpty List<String> format, EntityIdMap mapping, FrontendConfig.UploadConfig queryUpload, @NotNull DateReader dateReader) {
		Map<Integer, CDateSet> resolved = new Int2ObjectOpenHashMap<>();

		List<String[]> unresolvedDate = new ArrayList<>();
		List<String[]> unresolvedId = new ArrayList<>();

		// extract dates from rows
		final FrontendConfig.UploadConfig uploadConfig = queryUpload;

		final Int2ObjectMap<CDateSet> rowDates = readDates(values, format, dateReader, uploadConfig);

		final int idIndex = uploadConfig.getIdIndex(format);

		final ColumnConfig reader = uploadConfig.getIdMapper(format.get(idIndex));


		// ignore the first row, because this is the header
		for (int rowNum = 1; rowNum < values.length; rowNum++) {
			final String[] row = values[rowNum];
			final EntityIdMap.ExternalId externalId = reader.read(row[idIndex]);

			final int resolvedId = mapping.resolve(externalId);

			if (resolvedId == -1) {
				unresolvedId.add(row);
				continue;
			}

			if (!rowDates.containsKey(rowNum)) {
				unresolvedDate.add(row);
				continue;
			}

			//read the dates from the row
			resolved.put(resolvedId, rowDates.get(rowNum));
		}

		return new ResolveStatistic(resolved,unresolvedDate,unresolvedId);
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
