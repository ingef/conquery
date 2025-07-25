package com.bakdata.conquery.apiv1.query;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.ExternalIdChecker;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface QueryDescription extends Visitable {

	/**
	 * Transforms the submitted query to an {@link ManagedExecution}.
	 * In this step some external dependencies are resolve (such as {@link CQExternal}).
	 * However, steps that require add or manipulates queries programmatically based on the submitted query
	 * should be done in an extra init procedure (see {@link ManagedExecution#doInitExecutable(Namespace)}.
	 * These steps are executed right before the execution of the query and not necessary in this creation phase.
	 *
	 * @param user
	 * @param submittedDataset
	 * @param storage
	 * @param config
	 * @return
	 */
	ManagedExecution toManagedExecution(UserId user, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config);

	/**
	 * Initializes a submitted description using the provided context.
	 * All parameters that are set in this phase must be annotated with {@link com.bakdata.conquery.io.jackson.View.InternalCommunication}.
	 * @param context Holds information which can be used for the initialize the description of the query to be executed.
	 */
	void resolve(QueryResolveContext context);
	
	/**
	 * Allows the implementation to add visitors that traverse the QueryTree.
	 * All visitors are concatenated so only a single traverse needs to be done.
	 * @param visitors The structure to which new visitors need to be added.
	 */
	default void addVisitors(@NonNull List<QueryVisitor> visitors) {
		// Register visitors for permission checks
		visitors.add(new QueryUtils.ExternalIdChecker());
	}
	
	/**
	 * Check implementation specific permissions. Is called after all visitors have been registered and executed.
	 */
	default void authorize(Subject subject, DatasetId submittedDataset, List<QueryVisitor> visitors, MetaStorage storage) {
		authorizeQuery(this, subject, submittedDataset, visitors, storage);
	}

	static void authorizeQuery(QueryDescription queryDescription, Subject subject, DatasetId submittedDataset, List<QueryVisitor> visitors, MetaStorage storage) {
		NamespacedIdentifiableCollector nsIdCollector = QueryUtils.getVisitor(visitors, NamespacedIdentifiableCollector.class);
		ExternalIdChecker externalIdChecker = QueryUtils.getVisitor(visitors, ExternalIdChecker.class);

		// Generate DatasetPermissions
		final Set<DatasetId> datasets = nsIdCollector.getIdentifiables().stream()
												   .map(NamespacedIdentifiable::getDataset)
												   .collect(Collectors.toSet());

		subject.authorize(datasets, Ability.READ);

		// Generate ConceptPermissions
		final Set<Concept<?>> concepts = nsIdCollector.getIdentifiables().stream()
												   .filter(ConceptElement.class::isInstance)
												   .map(ConceptElement.class::cast)
												   .<Concept<?>>map(ConceptElement::getConcept)
												   .collect(Collectors.toSet());

		subject.authorize(concepts, Ability.READ);

		// Check reused query permissions
		final Set<ManagedExecution> collectedExecutions = queryDescription.collectRequiredQueries().stream()
																		  .map(storage::getExecution)
																		  .filter(Objects::nonNull)
																		  .collect(Collectors.toSet());

		subject.authorize(collectedExecutions, Ability.READ);

		// Check if the query contains parts that require to resolve external IDs. If so the subject must have the preserve_id permission on the dataset.
		if (externalIdChecker.resolvesExternalIds()) {
			subject.authorize(submittedDataset, Ability.PRESERVE_ID);
		}
	}

	Set<ManagedExecutionId> collectRequiredQueries();

	default RequiredEntities collectRequiredEntities(QueryExecutionContext context){
		return new RequiredEntities(context.getBucketManager().getEntities());
	}
}
