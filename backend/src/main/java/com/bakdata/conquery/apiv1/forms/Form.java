package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ClassToInstanceMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * API representation of a form query.
 */
public abstract class Form implements QueryDescription {

	/**
	 * Raw form config (basically the raw format of this form), that is used by the backend at the moment to
	 * create a {@link com.bakdata.conquery.models.forms.configs.FormConfig} upon start of this form (see {@link ManagedForm#start()}).
	 */
	@Nullable
	@Getter
	@Setter
	private JsonNode values;

	@JsonIgnore
	public String getFormType() {
		return this.getClass().getAnnotation(CPSType.class).id();
	}

	public abstract Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset, MetaStorage storage);


	@Override
	public void authorize(Subject subject, Dataset submittedDataset, @NonNull ClassToInstanceMap<QueryVisitor> visitors, MetaStorage storage) {
		QueryDescription.super.authorize(subject, submittedDataset, visitors, storage);
		// Check if subject is allowed to create this form
		subject.authorize(FormScanner.FRONTEND_FORM_CONFIGS.get(getFormType()), Ability.CREATE);
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
