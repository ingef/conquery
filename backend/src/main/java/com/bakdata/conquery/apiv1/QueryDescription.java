package com.bakdata.conquery.apiv1;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.ExternalIdChecker;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ClassToInstanceMap;
import lombok.NonNull;
import org.apache.shiro.authz.Permission;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface QueryDescription extends Visitable {

	/**
	 * Transforms the submitted query to an {@link ManagedExecution}.
	 * In this step some external dependencies are resolve (such as {@link CQExternal}).
	 * However steps that require add or manipulates queries programmatically based on the submitted query
	 * should be done in an extra init procedure (see {@link ManagedForm#doInitExecutable(DatasetRegistry)}.
	 * These steps are executed right before the execution of the query and not necessary in this creation phase.
	 * 
	 * @param storage Needed by {@link ManagedExecution} for the self update upon completion.
	 * @param datasets
	 * @param userId
	 * @param submittedDataset
	 * @return
	 */
	ManagedExecution<?> toManagedExecution(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset);

	
	Set<ManagedExecutionId> collectRequiredQueries();
	
	/**
	 * Initializes a submitted description using the provided context.
	 * All parameters that are set in this phase must be annotated with {@link InternalOnly}.
	 * @param context Holds information which can be used for the initialize the description of the query to be executed.
	 */
	void resolve(QueryResolveContext context);
	
	/**
	 * Allows the implementation to add visitors that traverse the QueryTree.
	 * All visitors are concatenated so only a single traverse needs to be done.  
	 * @param visitors The structure to which new visitors need to be added.
	 */
	default void addVisitors(@NonNull ClassToInstanceMap<QueryVisitor> visitors) {
		// Register visitors for permission checks
		visitors.putInstance(QueryUtils.NamespacedIdCollector.class, new QueryUtils.NamespacedIdCollector());
		visitors.putInstance(QueryUtils.ExternalIdChecker.class, new QueryUtils.ExternalIdChecker());
	}

	/**
	 * Check implementation specific permissions. Is called after all visitors have been registered and executed.
	 */
	default void collectPermissions(@NonNull ClassToInstanceMap<QueryVisitor> visitors, Collection<Permission> requiredPermissions, DatasetId submittedDataset) {
		NamespacedIdCollector nsIdCollector = QueryUtils.getVisitor(visitors, QueryUtils.NamespacedIdCollector.class);
		ExternalIdChecker externalIdChecker = QueryUtils.getVisitor(visitors, QueryUtils.ExternalIdChecker.class);
		if(nsIdCollector == null) {
			throw new IllegalStateException();
		}
		// Generate DatasetPermissions
		nsIdCollector.getIds().stream()
			.map(NamespacedId::getDataset)
			.distinct()
			.map(dId -> DatasetPermission.onInstance(Ability.READ, dId))
			.collect(Collectors.toCollection(() -> requiredPermissions));
		
		// Generate ConceptPermissions
		QueryUtils.generateConceptReadPermissions(nsIdCollector, requiredPermissions);
		
		// Generate permissions for reused queries
		for (ManagedExecutionId requiredQueryId : collectRequiredQueries()) {
			requiredPermissions.add(QueryPermission.onInstance(Ability.READ, requiredQueryId));
		}
		
		// Check if the query contains parts that require to resolve external IDs. If so the user must have the preserve_id permission on the dataset.
		if(externalIdChecker.resolvesExternalIds()) {
			requiredPermissions.add(DatasetPermission.onInstance(Ability.PRESERVE_ID, submittedDataset));
		}
	}

}
