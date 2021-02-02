package com.bakdata.conquery.apiv1.forms;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import lombok.NonNull;
import org.apache.shiro.authz.Permission;

/**
 * API representation of a form query.
 */
public interface Form extends QueryDescription {
	
	@JsonIgnore
	default String getFormType() {
		return this.getClass().getAnnotation(CPSType.class).id();
	}

	public abstract Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset);
	
	@Override
	public default ManagedForm toManagedExecution(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset) {
		return new ManagedForm(this, userId, submittedDataset);
	}
		
	@Override
	public default void collectPermissions(@NonNull ClassToInstanceMap<QueryVisitor> visitors, Collection<Permission> requiredPermissions, DatasetId submittedDataset) {
		QueryDescription.super.collectPermissions(visitors, requiredPermissions, submittedDataset);
		// Check if user is allowed to create this form
		requiredPermissions.add(FormPermission.onInstance(Ability.CREATE, getFormType()));
	}

	/**
	 * Utility function for forms that usually have at least one query as a prerequisite.
	 */
	public static IQuery resolvePrerequisite(QueryResolveContext context, ManagedExecutionId prerequisiteId) {
		// Resolve the prerequisite
		ManagedExecution<?> prerequisiteExe = context.getDatasetRegistry().getMetaStorage().getExecution(prerequisiteId);
		if(!(prerequisiteExe instanceof ManagedQuery)) {
			throw new IllegalArgumentException("The prerequisite query must be of type " + ManagedQuery.class.getName());
		}
		IQuery query = ((ManagedQuery)prerequisiteExe).getQuery();
		query.resolve(context);
		return query;
	}


	/** 
	 * Is called in context of a request to generate a default label.
	 * If localization is needed use:<br/>
	 * <code>
	 * Locale preferredLocale = I18n.LOCALE.get();
	 * </code>
	 */
	@JsonIgnore
	abstract public String getLocalizedTypeLabel();
}
