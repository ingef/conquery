package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.arx.ArxExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.deidentifier.arx.ARXAnonymizer;

/**
 * Form that performs an anonymization using the ARX library on the result of the given execution.
 *
 * @see ArxExecution which intercepts the result of the execution and processes it using ARX.
 */
@CPSType(id = "ARX_FORM", base = QueryDescription.class)
@Setter
public class ArxForm extends Form {


	@NotNull
	@JsonProperty("queryGroup")
	private ManagedExecutionId queryGroupId;

	/**
	 * @see org.deidentifier.arx.criteria.KAnonymity#KAnonymity(int)
	 */
	@Getter
	@Min(2)
	@JsonProperty("kAnonymityParam")
	private int kAnonymityParam = 2;

	/**
	 * @see org.deidentifier.arx.ARXConfiguration#setSuppressionLimit(double)
	 */
	@Getter
	@DecimalMax(value = "1")
	@DecimalMin(value = "0")
	private double suppressionLimit = 0.02;


	/**
	 * @see ARXAnonymizer#setMaximumSnapshotSizeDataset(double)
	 */
	@Getter
	@DecimalMax(value = "1")
	@DecimalMin(value = "0", inclusive = false)
	private double maximumSnapshotSizeDataset = 0.2;

	/**
	 * @see ARXAnonymizer#setMaximumSnapshotSizeSnapshot(double)
	 */
	@Getter
	@DecimalMax(value = "1")
	@DecimalMin(value = "0", inclusive = false)
	private double maximumSnapshotSizeSnapshot = 0.2;

	/**
	 * @see ARXAnonymizer#setHistorySize(int)
	 */
	@Getter
	@Min(1)
	private int historySize = 200;

	@JsonIgnore
	private ManagedQuery queryGroup;

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {
		return Map.of(ConqueryConstants.SINGLE_RESULT_TABLE_NAME, List.of(queryGroup));
	}

	@Override
	@JsonIgnore
	public String getLocalizedTypeLabel() {
		return "ARX Anonymization";
	}

	@Override
	public ManagedExecution<?> toManagedExecution(User user, Dataset submittedDataset) {
		return new ArxExecution(this, user, submittedDataset);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroupId);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup = (ManagedQuery) context.getDatasetRegistry().getMetaRegistry().resolve(queryGroupId);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
