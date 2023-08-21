package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.filter.FilterConverterService;
import com.bakdata.conquery.sql.conversion.select.SelectConverterService;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private final List<ConceptQueryStep> querySteps;

	public CQConceptConverter(FilterConverterService filterConverterService, SelectConverterService selectConverterService) {
		this.querySteps = List.of(
				new PreprocessingQueryStep(),
				new DateRestrictionQueryStep(),
				new EventSelectStep(selectConverterService),
				new EventFilterQueryStep(filterConverterService),
				new FinalConceptQueryStep()
		);
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept node, ConversionContext context) {

		if (node.getTables().size() > 1) {
			throw new UnsupportedOperationException("Can't handle concepts with multiple tables for now.");
		}

		StepContext stepContext = StepContext.builder()
											 .context(context)
											 .node(node)
											 .table(node.getTables().get(0))
											 .conceptLabel(this.getConceptLabel(node, context))
											 .sqlFunctions(context.getSqlDialect().getFunction())
											 .build();

		for (ConceptQueryStep queryStep : this.querySteps) {
			Optional<QueryStep> convert = queryStep.convert(stepContext);
			if (convert.isPresent()) {
				stepContext = stepContext.toBuilder()
										 .previous(convert.get())
										 .previousSelects((ConceptSelects) convert.get().getQualifiedSelects())
										 .build();
			}
		}

		return context.withQueryStep(stepContext.getPrevious());
	}

	private String getConceptLabel(CQConcept node, ConversionContext context) {
		// only relevant for debugging purposes as it will be part of the generated SQL query
			// we prefix each cte name of a concept with an incrementing counter to prevent naming collisions if the same concept is selected multiple times
			return "%s_%s".formatted(
					context.getQueryStepCounter(),
					node.getUserOrDefaultLabel(Locale.ENGLISH)
						.toLowerCase()
						.replace(' ', '_')
						.replaceAll("\\s", "_")
			);
	}


}
