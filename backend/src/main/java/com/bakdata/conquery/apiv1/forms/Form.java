package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import lombok.NonNull;

/**
 * API representation of a form query.
 */
public abstract class Form implements QueryDescription {
	
	@JsonIgnore
	public String getFormType() {
		return this.getClass().getAnnotation(CPSType.class).id();
	}

	public abstract Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset);


	@Override
	public void authorize(User user, Dataset submittedDataset, @NonNull ClassToInstanceMap<QueryVisitor> visitors) {
		QueryDescription.super.authorize(user, submittedDataset, visitors);
		// Check if user is allowed to create this form
		user.authorize(FormScanner.FRONTEND_FORM_CONFIGS.get(getFormType()), Ability.CREATE);
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
