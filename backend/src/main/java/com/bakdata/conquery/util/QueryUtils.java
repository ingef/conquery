package com.bakdata.conquery.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConceptPermission;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQExternalResolved;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;
import com.google.common.collect.ClassToInstanceMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;

@Slf4j
@UtilityClass
public class QueryUtils {
	
	/**
	 * Provides a starting operator for consumer chains, that does nothing.
	 */
	public static <T> Consumer<T> getNoOpEntryPoint() {
		return (whatever) -> {};
	}
	
	/**
	 * Gets the specified visitor from the map. If none was found an exception is raised.
	 */
	public static <T extends QueryVisitor> T getVisitor(ClassToInstanceMap<QueryVisitor> visitors, Class<T> clazz){
		return Objects.requireNonNull(visitors.getInstance(clazz),String.format("Among the visitor that traversed the query no %s could be found", clazz));
	}

	/**
	 * Checks if the query requires to resolve external ids.
	 *
	 * @return True if a {@link CQExternal} is found.
	 */
	public static class ExternalIdChecker implements QueryVisitor {

		private final List<CQElement> elements = new ArrayList<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQExternal || element instanceof CQExternalResolved) {
				elements.add((CQElement) element);
			}
		}

		public boolean resolvesExternalIds() {
			return elements.size() > 0;
		}
	}

	/**
	 * Find first and only directly ReusedQuery in the queries tree, and return its
	 * Id. ie.: arbirtary CQAnd/CQOr with only them or then a ReusedQuery.
	 *
	 * @return Null if not only a single {@link CQReusedQuery} was found beside
	 * {@link CQAnd} / {@link CQOr}.
	 */
	public static class SingleReusedChecker implements QueryVisitor {

		final List<CQReusedQuery> reusedElements = new ArrayList<>();
		private boolean containsOthersElements = false;

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQReusedQuery) {
				reusedElements.add((CQReusedQuery) element);
			}
			else if (element instanceof CQAnd || element instanceof CQOr) {
				// Ignore these elements
			}
			else {
				containsOthersElements = true;
			}
		}

		public ManagedExecutionId getOnlyReused() {
			return (reusedElements.size() == 1 && !containsOthersElements) ? reusedElements.get(0).getQuery() : null;
		}
	}


	/**
	 * Find all ReusedQuery in the queries tree, and return their Ids.
	 *
	 */
	public static class AllReusedFinder implements QueryVisitor {

		@Getter
		final List<CQReusedQuery> reusedElements = new ArrayList<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQReusedQuery) {
				reusedElements.add((CQReusedQuery) element);
			}
		}

	}

	/**
	 * Collects all {@link NamespacedId} references provided by a user from a
	 * {@link Visitable}.
	 */
	public static class NamespacedIdCollector implements QueryVisitor {

		@Getter
		private Set<NamespacedId> ids = new HashSet<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof NamespacedIdHolding) {
				NamespacedIdHolding idHolder = (NamespacedIdHolding) element;
				ids.addAll(idHolder.collectNamespacedIds());
			}
		}
	}
	
	public static void generateConceptReadPermissions(@NonNull NamespacedIdCollector idCollector, @NonNull Collection<Permission> collectPermissions){
		idCollector.getIds().stream()
			.filter(id -> ConceptElementId.class.isAssignableFrom(id.getClass()))
			.map(ConceptElementId.class::cast)
			.map(ConceptElementId::findConcept)
			.map(cId -> ConceptPermission.onInstance(Ability.READ, cId))
			.map(Permission.class::cast)
			.distinct()
			.collect(Collectors.toCollection(() -> collectPermissions));
	}

	/**
	 * Patches the given {@link Identifiable} by checking if the user holds the necessary Permission for that operation.
	 * Hence the patched instance must have a corresponding {@link Permission}-type.
	 * Tagging and Labeling only alters the state of the instance while sharing also alters the state of {@link Group}s.
	 * @param <INST>	Type of the instance that is patched
	 * @param storage	Storage that persists the instance and also auth information.
	 * @param user		The user on whose behalf the patch is executed
	 * @param instance	The instance to patch
	 * @param patch		The patch that is applied to the instance
	 * @param permissionCreator	A function that produces a {@link Permission} that targets the given instance (e.g QueryPermission, FormConfigPermission).
	 */
	public static <INST extends Taggable & Shareable & Labelable & Identifiable<?>> void patchIdentifialble(MasterMetaStorage storage, User user, INST instance, QueryPatch patch, Function<Ability,ConqueryPermission> permissionCreator) {
		
		Consumer<QueryPatch> patchConsumerChain = QueryUtils.getNoOpEntryPoint();
		
		if(patch.getTags() != null && user.isPermitted(permissionCreator.apply(Ability.TAG))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.tagger());
		}
		if(patch.getLabel() != null && user.isPermitted(permissionCreator.apply(Ability.LABEL))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.labeler());
		}
		if(patch.getShared() != null && user.isPermitted(permissionCreator.apply(Ability.SHARE))) {
			List<Group> groups;
			if(patch.getGroups() != null) {
				groups = patch.getGroups().stream().map(id -> storage.getGroup(id)).collect(Collectors.toList());
			}
			else {				
				groups = AuthorizationHelper.getGroupsOf(user, storage);
			}
			for(Group group : groups) {
				patchConsumerChain = patchConsumerChain.andThen(instance.sharer(storage, user, group, (c) -> permissionCreator.apply(Ability.READ)));

			}
		}
		patchConsumerChain.accept(patch);
	}
	
	/**
	 * (Un)Shares a query with a specific group. Set or unsets the shared flag.
	 * Does persist this change made to the {@link Shareable}. 
	 */
	public static <S extends Identifiable<?> & Shareable, O extends PermissionOwner<? extends IId<O>>> void shareWithOther(
		MasterMetaStorage storage,
		User user,
		S shareable,
		Function<S, ConqueryPermission> instancePermissionCreator,
		O other,
		boolean shared) {
		
		ConqueryPermission sharePermission = instancePermissionCreator.apply(shareable);
		if (shared) {
			other.addPermission(storage, sharePermission);
			log.trace("User {} shares query {}. Adding permission {} to group {}.", user, shareable, shareable.getId(), sharePermission, other);
		}
		else {
			other.removePermission(storage, sharePermission);
			log.trace("User {} unshares query {}. Removing permission {} from group {}.", user, shareable, shareable.getId(), sharePermission, other);
		}
		shareable.setShared(shared);
	}
}
