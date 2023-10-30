package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.FlagFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.FlagCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class FlagFilterConverter implements FilterConverter<String[], FlagFilter> {

	@Override
	public ConceptFilter convert(FlagFilter flagFilter, FilterContext<String[]> context) {

		ConceptTables conceptTables = context.getConceptTables();

		// only columns that match the selected flags are required for us to apply the filter
		String[] selectedFlags = context.getValue();
		List<Column> requiredColumns = getRequiredColumnsForSelectedFlags(flagFilter, selectedFlags);

		String rootTable = conceptTables.getPredecessorTableName(ConceptCteStep.PREPROCESSING);
		List<SqlSelect> rootSelects =
				flagFilter.getRequiredColumns().stream()
						  .filter(requiredColumns::contains)
						  .map(column -> new ExtractingSqlSelect<>(rootTable, column.getName(), Boolean.class))
						  .collect(Collectors.toList());

		List<Field<Boolean>> flagFields =
				rootSelects.stream()
						   .map(sqlSelect -> conceptTables.<Boolean>qualifyOnPredecessor(ConceptCteStep.EVENT_FILTER, sqlSelect.aliased()))
						   .collect(Collectors.toList());
		FlagCondition flagCondition = new FlagCondition(flagFields);

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(rootSelects)
						  .build(),
				Filters.builder()
					   .event(List.of(flagCondition))
					   .build()
		);
	}

	private List<Column> getRequiredColumnsForSelectedFlags(FlagFilter flagFilter, String[] selectedFlags) {
		ArrayList<Column> requiredColumns = new ArrayList<>();
		for (String selectedFlag : selectedFlags) {
			Column column = flagFilter.getFlags().get(selectedFlag);
			requiredColumns.add(column);
		}
		return requiredColumns;
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.EVENT_FILTER);
	}

	@Override
	public Class<? extends FlagFilter> getConversionClass() {
		return FlagFilter.class;
	}

}
