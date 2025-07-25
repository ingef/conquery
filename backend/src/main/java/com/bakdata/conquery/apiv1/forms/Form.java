package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * API representation of a form query.
 */
@EqualsAndHashCode
public abstract class Form implements QueryDescription {


	/**
	 * Raw form config (basically the raw format of this form), that is used by the backend at the moment to
	 * create a {@link com.bakdata.conquery.models.forms.configs.FormConfig} upon start of this form (see {@link ManagedExecution#start()}).
	 */
	@Nullable
	public abstract JsonNode getValues();

	@JsonIgnore
	public String getFormType() {
		return this.getClass().getAnnotation(CPSType.class).id();
	}



	@Override
	public void authorize(Subject subject, DatasetId submittedDataset, @NonNull List<QueryVisitor> visitors, MetaStorage storage) {
		QueryDescription.super.authorize(subject, submittedDataset, visitors, storage);
		// Check if subject is allowed to create this form
		final FormType formType = FormScanner.resolveFormType(getFormType());

		if (formType == null) {
			throw new ConqueryError.ExecutionCreationErrorUnspecified();
		}

		subject.authorize(formType, Ability.CREATE);
	}


	/** 
	 * Is called in context of a request to generate a default label.
	 * If localization is needed use:<br/>
	 * <code>
	 * Locale preferredLocale = I18n.LOCALE.get();
	 * </code>
	 */
	@JsonIgnore
	public abstract String getLocalizedTypeLabel();
}
