package com.bakdata.conquery.apiv1.forms.arx;

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
import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.criteria.PrivacyCriterion;
import org.deidentifier.arx.risk.RiskModelPopulationUniqueness;
import org.jetbrains.annotations.TestOnly;

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
	 * @see org.deidentifier.arx.ARXConfiguration#setSuppressionLimit(double)
	 */
	@Getter
	@DecimalMax(value = "1")
	@DecimalMin(value = "0")
	private double suppressionLimit = 0.02;

	/**
	 * The PrivacyModel to use
	 */
	@NotNull
	@Getter
	private PrivacyModel privacyModel;

	@JsonIgnore
	private ManagedQuery queryGroup;

	public enum PrivacyModel {
		/**
		 * Use for testing only
		 */
		K_ANONYMITY_2() {
			@Override
			public PrivacyCriterion getPrivacyCriterion() {
				return new KAnonymity(2);
			}
		},
		K_ANONYMITY_5() {
			@Override
			public PrivacyCriterion getPrivacyCriterion() {
				return new KAnonymity(5);
			}
		},
		K_ANONYMITY_11() {
			@Override
			public PrivacyCriterion getPrivacyCriterion() {
				return new KAnonymity(11);
			}
		},
		PITMAN_1_PERCENT() {
			@Override
			public PrivacyCriterion getPrivacyCriterion() {
				return new PopulationUniqueness(0.001, RiskModelPopulationUniqueness.PopulationUniquenessModel.PITMAN, ARXPopulationModel.create(ARXPopulationModel.Region.EUROPEAN_UNION));
			}
		};

		public abstract PrivacyCriterion getPrivacyCriterion();
	}

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
