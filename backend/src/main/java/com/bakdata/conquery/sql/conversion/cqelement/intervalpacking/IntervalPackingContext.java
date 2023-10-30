package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import lombok.Value;
import org.jooq.Field;

@Value
public class IntervalPackingContext implements Context {

	String nodeLabel;
	Field<Object> primaryColumn;
	ColumnDateRange validityDate;
	QueryStep predecessor;
	IntervalPackingTables intervalPackingTables;
	List<ExplicitSelect> carryThroughSelects;
	NameGenerator nameGenerator;

	public IntervalPackingContext(
			String conceptLabel,
			Field<Object> primaryColumn,
			ColumnDateRange validityDate,
			ConceptTables conceptTables,
			NameGenerator nameGenerator
	) {
		this.nodeLabel = conceptLabel;
		this.primaryColumn = primaryColumn;
		this.validityDate = validityDate;
		this.predecessor = null; // we don't need a predecessor because the interval packing steps will be joined with the other concept steps
		this.intervalPackingTables = IntervalPackingTables.forConcept(conceptLabel, conceptTables, nameGenerator);
		this.carryThroughSelects = Collections.emptyList();
		this.nameGenerator = nameGenerator;
	}

	/**
	 * @param nodeLabel            A unique CTE label which will be suffixed with the interval packing CTE names.
	 * @param predeceasingStep        The predeceasing step containing the validity date which should be interval-packed.
	 * @param carryThroughSelects    The selects you want to carry through all interval packing steps. They won't get touched besides qualifying.
	 */
	public IntervalPackingContext(
			String nodeLabel,
			QueryStep predeceasingStep,
			List<ExplicitSelect> carryThroughSelects,
			NameGenerator nameGenerator
	) {
		this.nodeLabel = nodeLabel;
		this.primaryColumn = predeceasingStep.getSelects().getPrimaryColumn();
		this.validityDate = predeceasingStep.getSelects().getValidityDate().get();
		this.predecessor = predeceasingStep;
		this.carryThroughSelects = carryThroughSelects;
		this.intervalPackingTables = IntervalPackingTables.forGenericQueryStep(nodeLabel, predeceasingStep, nameGenerator);
		this.nameGenerator = nameGenerator;
	}

	@CheckForNull
	public QueryStep getPredecessor() {
		return predecessor;
	}

}
