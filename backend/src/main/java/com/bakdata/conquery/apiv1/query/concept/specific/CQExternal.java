package com.bakdata.conquery.apiv1.query.concept.specific;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.exceptions.validators.ValidCSVFormat;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdAccessorImpl;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.MoreCollectors;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "EXTERNAL", base = CQElement.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class CQExternal extends CQElement {

	@Getter
	@NotEmpty
	@ValidCSVFormat
	private final List<FormatColumn> format;
	@Getter
	@NotEmpty
	private final String[][] values;

	@Getter @InternalOnly
	private Map<Integer, CDateSet> valuesResolved;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		if (valuesResolved == null) {
			throw new IllegalStateException("CQExternal needs to be resolved before creating a plan");
		}
		return new ExternalNode(context.getStorage().getDataset().getAllIdsTable(), valuesResolved);
	}


	@Override
	public void resolve(QueryResolveContext context) {
		EncodedDictionary primary = context.getNamespace().getStorage().getPrimaryDictionary();
		Optional<DateFormat> dateFormat = format.stream()
												.map(FormatColumn::getDateFormat)
												.filter(Objects::nonNull)
												.distinct()
												.collect(MoreCollectors.toOptional());
		int[] dateIndices = format.stream()
								  .filter(fc -> fc.getDateFormat() != null)
								  .mapToInt(format::indexOf)
								  .toArray();

		valuesResolved = new Int2ObjectOpenHashMap<>();

		IdMappingConfig mapping = context.getConfig().getIdMapping();

		IdAccessor idAccessor = mapping.mappingFromCsvHeader(
				IdAccessorImpl.selectIdFields(values[0], format),
				context.getNamespace().getStorage().getIdMapping()
		);
		List<List<String>> nonResolved = new ArrayList<>();

		if (values[0].length != format.size()) {
			throw new ConqueryError.ExternalResolveFormatError(format.size(), values[0].length);
		}


		// ignore the first row, because this is the header
		for (int i = 1; i < values.length; i++) {
			String[] row = values[i];

			//read the dates from the row
			try {
				CDateSet dates = dateFormat.map(df -> {
					try {
						return df.readDates(dateIndices, row, context.getConfig().getPreprocessor().getParsers().getDateFormats());
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}).orElseGet(CDateSet::createFull);
				// remove all fields from the data line that are not id fields, in case the mapping is not possible we avoid the data columns to be joined
				CsvEntityId id = idAccessor.getCsvEntityId(IdAccessorImpl.selectIdFields(row, format));

				int resolvedId;
				if (id != null && (resolvedId = primary.getId(id.getCsvId())) != -1) {
					valuesResolved.put(resolvedId, dates);
				}
				else {
					nonResolved.add(Arrays.asList(row));
				}
			}
			catch (Exception e) {
				log.warn("Failed to parse id from " + Arrays.toString(row), e);
			}
		}
		if (!nonResolved.isEmpty()) {
			log.warn(
					"Could not resolve {} of the {} rows. Not resolved: {}",
					nonResolved.size(),
					values.length - 1,
					nonResolved.subList(0, Math.min(nonResolved.size(), 10))
			);
		}

		if (valuesResolved.isEmpty()) {
			throw new ConqueryError.ExternalResolveEmptyError();
		}
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
	}

	public enum DateFormat {
		EVENT_DATE {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row, DateFormats dateFormats) throws ParsingException {
				return CDateSet.create(Collections.singleton(CDateRange.exactly(dateFormats.parseToLocalDate(row[dateIndices[0]]))));
			}
		},
		START_END_DATE {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row, DateFormats dateFormats) throws ParsingException {
				LocalDate start = row[dateIndices[0]] == null ? null : dateFormats.parseToLocalDate(row[dateIndices[0]]);

				LocalDate end = (dateIndices.length < 2 || row[dateIndices[1]] == null) ?
								null :
						dateFormats.parseToLocalDate(row[dateIndices[1]]);

				if (start == null && end == null) {
					return null;
				}

				return CDateSet.create(Collections.singleton(CDateRange.of(start, end)));
			}
		},
		DATE_RANGE {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row, DateFormats dateFormats) throws ParsingException {
				return CDateSet.create(Collections.singleton(DateRangeParser.parseISORange(row[dateIndices[0]], dateFormats)));
			}
		},
		DATE_SET {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row, DateFormats dateFormats) throws ParsingException {
				return CDateSet.parse(row[dateIndices[0]], dateFormats);
			}
		};

		public abstract CDateSet readDates(int[] dateIndices, String[] row, DateFormats dateFormats) throws ParsingException;
	}

	@RequiredArgsConstructor
	@Getter
	public enum FormatColumn {
		ID(true, null),
		EVENT_DATE(false, DateFormat.EVENT_DATE),
		START_DATE(false, DateFormat.START_END_DATE),
		END_DATE(false, DateFormat.START_END_DATE),
		DATE_RANGE(false, DateFormat.DATE_RANGE),
		DATE_SET(false, DateFormat.DATE_SET),
		IGNORE(true, null);

		private final boolean duplicatesAllowed;
		private final DateFormat dateFormat;
	}
}
