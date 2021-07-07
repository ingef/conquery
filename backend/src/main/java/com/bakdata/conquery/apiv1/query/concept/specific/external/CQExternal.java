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
import com.bakdata.conquery.models.identifiable.mapping.UnresolvedEntityId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "EXTERNAL", base = CQElement.class)
public class CQExternal extends CQElement {

	@Getter
	@NotEmpty
	private final List<FormatColumn> format;
	@Getter
	@NotEmpty
	private final String[][] values;

	@Getter
	@InternalOnly
	private Map<Integer, CDateSet> valuesResolved;

	public CQExternal(@NotEmpty List<FormatColumn> format, @NotEmpty String[][] values) {
		this.format = format;
		this.values = values;

		for (int index = 0; index < format.size(); index++) {
			format.get(index).setPosition(index);
		}
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


		//TODO probably easy to simplify.

		final DateColumn[] dateColumns = format.stream()
											   .filter(DateColumn.class::isInstance)
											   .map(DateColumn.class::cast)
											   .toArray(DateColumn[]::new);

		//TODO verify dateColumns match

		final DateFormat dateFormat = dateColumns.length > 0 ? dateColumns[0].getFormat() : DateFormat.ALL;


		final IdColumn idColumn = format.stream()
										.filter(IdColumn.class::isInstance)
										.map(IdColumn.class::cast)
										.collect(MoreCollectors.onlyElement());


		final EncodedDictionary primary = context.getNamespace().getStorage().getPrimaryDictionary();
		final EntityIdMap mapping = context.getNamespace().getStorage().getIdMapping();
		final DateFormats dateFormats = context.getConfig().getPreprocessor().getParsers().getDateFormats();


		List<String[]> nonResolved = new ArrayList<>();


		// ignore the first row, because this is the header
		for (int i = 1; i < values.length; i++) {
			final String[] row = values[i];
			final String externalId = idColumn.read(row);


			//read the dates from the row
			try {

				CDateSet dates = dateFormat.readDates(dateColumns, row, dateFormats);

				Optional<String> id = mapping.toInternal(externalId);

				int resolvedId;

				if (id.isPresent() && (resolvedId = primary.getId(id.get())) != -1) {
					valuesResolved.put(resolvedId, dates);
				}
				else {
					nonResolved.add(row);
				}
			}
			catch (Exception e) {
				log.warn("Failed to parse id from {}", row, e);
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


	@JsonIgnore
	@ValidationMethod(message = "Values and Format are not of same width.")
	public boolean isAllSameLength() {
		final int expected = format.size();
		return Arrays.stream(values).mapToInt(a -> a.length).allMatch(v -> expected == v);
	}

	@JsonIgnore
	@ValidationMethod(message = "Must use one Id.")
	public boolean isOnlyOneId() {
		return format.stream().filter(IdColumn.class::isInstance).count() == 1;
	}

	//TODO implement this

	//	@ValidationMethod(message = "Wrong usage of Dates.")
	//	public boolean isDatesAccessorExclusive() {
	//		if (format.contains(FormatColumn.DATE_RANGE)) {
	//			return !(format.contains(FormatColumn.DATE_SET) || format.contains(FormatColumn.START_DATE) || format.contains(FormatColumn.END_DATE));
	//		}
	//
	//		if (format.contains(FormatColumn.DATE_SET)) {
	//			return !(format.contains(FormatColumn.START_DATE) || format.contains(FormatColumn.END_DATE));
	//		}
	//
	//		if (format.stream().filter(FormatColumn.START_DATE::equals).count() > 1) {
	//			return false;
	//		}
	//
	//		if (format.stream().filter(FormatColumn.END_DATE::equals).count() > 1) {
	//			return false;
	//		}
	//
	//		return true;
	//	}
}
