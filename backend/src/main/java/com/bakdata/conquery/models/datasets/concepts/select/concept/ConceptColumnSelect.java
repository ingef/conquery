package com.bakdata.conquery.models.datasets.concepts.select.concept;

import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptColumnsAggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;
import lombok.ToString;

/**
 * Select collecting the concept-Elements for a whole concept.
 *
 * When prettyPrint is true, the label of the Concept Element is used. Else, the ConceptElementId is returned.
 */
@CPSType(id = "CONCEPT_VALUES", base = Select.class)
@ToString
public class ConceptColumnSelect extends Select {

	@JsonBackReference
	@JsonIgnore
	@Setter
	private TreeConcept concept;

	@Override
	public Aggregator<?> createAggregator() {
		return new ConceptColumnsAggregator(concept);
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {
		return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.ConceptColumnT(concept)));
	}


	/**
	 * rawValue is expected to be an Integer, expressing a localId for {@link TreeConcept#getElementByLocalId(int)}.
	 *
	 * If {@link PrintSettings#isPrettyPrint()} is true, {@link ConceptElement#getLabel()} is used to print.
	 * If {@link PrintSettings#isPrettyPrint()} is false, {@link ConceptElement#getId()} ()} is used to print.
 	 */
	public static String printValue(Concept concept, Object rawValue, PrintSettings printSettings) {

		if (rawValue == null) {
			return null;
		}

		if (!(concept instanceof TreeConcept)) {
			return Objects.toString(rawValue);
		}

		final TreeConcept tree = (TreeConcept) concept;

		int localId = (int) rawValue;

		final ConceptTreeNode<?> node = tree.getElementByLocalId(localId);

		if (!printSettings.isPrettyPrint()) {
			return node.getId().toStringWithoutDataset();
		}

		return node.getName();
	}
}
