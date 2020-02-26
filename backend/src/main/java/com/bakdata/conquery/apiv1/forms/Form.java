package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * API representation of a form query.
 */
@Getter
@Setter
public abstract class Form implements SubmittedQuery {

	public abstract Map<String, List<ManagedQuery>> createSubQueries(Namespaces namespaces, UserId userId, DatasetId submittedDataset);

	/**
	 * Executed upon Execution initialization.
	 * E.g. Manipulate or add concepts to the form.
	 * @param namespaces
	 */
	public void init(Namespaces namespaces) {
		// Do nothing if not necessary
	};
	
	public ManagedForm toManagedExecution(MasterMetaStorage storage, Namespaces namespaces, UserId userId, DatasetId submittedDataset) {
		return new ManagedForm(storage, this, userId, submittedDataset);
	}
	
	@Override
	public void checkPermissions(@NonNull User user) {
		SubmittedQuery.super.checkPermissions(user);
		user.checkPermission(FormPermission.onInstance(Ability.CREATE, this.getClass()));
	}
}
