package com.bakdata.conquery.models.datasets.concepts.select.concept;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptElementsAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptValuesAggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.ConceptIdPrinter;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.sql.conversion.model.select.ConceptColumnSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;

/**
 * Select to extract the values used to build a {@link TreeConcept}.
 */
@CPSType(id = "CONCEPT_VALUES", base = Select.class)
@Data
public class ConceptColumnSelect extends UniversalSelect {

	/**
	 * If true, values are returned as resolved {@link ConceptElement#getLabel()} instead of the actual values.
	 */
	private boolean asIds = false;

	@Override
	public Aggregator<?> createAggregator() {
		if (isAsIds()) {
			return new ConceptElementsAggregator(((TreeConcept) getHolder().findConcept()));
		}

		return new ConceptValuesAggregator(((TreeConcept) getHolder().findConcept()));
	}

	@Override
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (isAsIds()) {
			return printerFactory.getListPrinter(new ConceptIdPrinter((TreeConcept) getHolder().findConcept(), printSettings), printSettings);
		}

		return printerFactory.getListPrinter(printerFactory.getStringPrinter(printSettings), printSettings);
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {

		if (isAsIds()) {
			return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.ConceptColumnT(cqConcept.getConceptId())));
		}

		return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.ConceptColumnT(cqConcept.getConceptId())));
	}

	@JsonIgnore
	@ValidationMethod(message = "Holder must be TreeConcept.")
	public boolean isHolderTreeConcept() {
		return getHolder().findConcept() instanceof TreeConcept;
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT<>(ResultType.Primitive.STRING);
	}

	@Override
	public SelectConverter<ConceptColumnSelect> createConverter() {
		//TODO bind Select to converter here
		return new ConceptColumnSelectConverter();
	}
}
