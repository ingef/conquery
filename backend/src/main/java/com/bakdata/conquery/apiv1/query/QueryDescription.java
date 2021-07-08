package com.bakdata.conquery.apiv1.query;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.ExternalIdChecker;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ClassToInstanceMap;
import lombok.NonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface QueryDescription extends Visitable {

	/**
	 * Transforms the submitted query to an {@link ManagedExecution}.
	 * In this step some external dependencies are resolve (such as {@link CQExternal}).
	 * However steps that require add or manipulates queries programmatically based on the submitted query
	 * should be done in an extra init procedure (see {@link ManagedForm#doInitExecutable(DatasetRegistry, ConqueryConfig)}.
	 * These steps are executed right before the execution of the query and not necessary in this creation phase.
	 *
	 * @param user
	 * @param submittedDataset
	 * @return
	 */
	ManagedExecution<?> toManagedExecution(User user, Dataset submittedDataset);

	
	Set<ManagedExecution<?>> collectRequiredQueries();
	
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
		visitors.putInstance(NamespacedIdentifiableCollector.class, new NamespacedIdentifiableCollector());
		visitors.putInstance(QueryUtils.ExternalIdChecker.class, new QueryUtils.ExternalIdChecker());
	}

	/**
	 * Check implementation specific permissions. Is called after all visitors have been registered and executed.
	 */
	default void authorize(User user, Dataset submittedDataset, @NonNull ClassToInstanceMap<QueryVisitor> visitors) {
		NamespacedIdentifiableCollector nsIdCollector = QueryUtils.getVisitor(visitors, NamespacedIdentifiableCollector.class);
		ExternalIdChecker externalIdChecker = QueryUtils.getVisitor(visitors, QueryUtils.ExternalIdChecker.class);
		if(nsIdCollector == null) {
			throw new IllegalStateException();
		}
		// Generate DatasetPermissions
		final Set<Dataset> datasets = nsIdCollector.getIdentifiables().stream()
												  .map(NamespacedIdentifiable::getDataset)
												  .collect(Collectors.toSet());

		user.authorize(datasets, Ability.READ);

		// Generate ConceptPermissions
		final Set<Concept> concepts = nsIdCollector.getIdentifiables().stream()
													  .filter(ConceptElement.class::isInstance)
													  .map(ConceptElement.class::cast)
													  .map(ConceptElement::getConcept)
													  .collect(Collectors.toSet());

		user.authorize(concepts, Ability.READ);

		user.authorize(collectRequiredQueries(), Ability.READ);
		
		// Check if the query contains parts that require to resolve external IDs. If so the user must have the preserve_id permission on the dataset.
		if(externalIdChecker.resolvesExternalIds()) {
			user.authorize(submittedDataset, Ability.PRESERVE_ID);
		}
	}

}
