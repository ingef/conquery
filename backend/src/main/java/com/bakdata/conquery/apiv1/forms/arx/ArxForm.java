package com.bakdata.conquery.apiv1.forms.arx;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.ws.rs.InternalServerErrorException;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.arx.ARXConfig;
import com.bakdata.conquery.models.forms.arx.ArxExecution;
import com.bakdata.conquery.models.forms.arx.models.PrivacyModel;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.deidentifier.arx.criteria.PrivacyCriterion;

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
	@NotNull
	private BigDecimal suppressionLimit = new BigDecimal("0.02");

	/**
	 * The PrivacyModel to use
	 */
	@NotNull
	@Getter
	private String privacyModel;

	@JsonIgnore
	@Getter
	private PrivacyCriterion resolvedPrivacyModel;

	@JsonIgnore
	private ManagedQuery queryGroup;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@EqualsAndHashCode.Exclude
	private ConqueryConfig config;

	public ArxForm(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	@ValidationMethod(message = "The referenced privacy model is not configured")
	@JsonIgnore
	public boolean isPrivacyModelSupported() {

		final ARXConfig
				arxConfig =
				config.getPluginConfig(ARXConfig.class).orElseThrow(() -> new InternalServerErrorException("ARX module not configured"));

		return arxConfig.getPrivacyModels().get(privacyModel) != null;
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
	public ManagedExecution toManagedExecution(User user, Dataset submittedDataset) {
		return new ArxExecution(this, user, submittedDataset, getStorage());
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroupId);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup = (ManagedQuery) context.getDatasetRegistry().getMetaRegistry().resolve(queryGroupId);
		final ARXConfig
				arxConfig =
				config.getPluginConfig(ARXConfig.class).orElseThrow(ConqueryError.ExecutionCreationErrorUnspecified::new);

		final PrivacyModel configuredPrivacyModel = arxConfig.getPrivacyModels().get(privacyModel);

		resolvedPrivacyModel = configuredPrivacyModel.getPrivacyCriterion();
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
