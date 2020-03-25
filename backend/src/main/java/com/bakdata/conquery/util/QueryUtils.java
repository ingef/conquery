package com.bakdata.conquery.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConceptPermission;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
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
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.shiro.authz.Permission;

@UtilityClass
public class QueryUtils {

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
			.filter(id -> ConceptId.class.isAssignableFrom(id.getClass()))
			.map(ConceptId.class::cast)
			.map(cId -> ConceptPermission.onInstance(Ability.READ, cId))
			.map(Permission.class::cast)
			.distinct()
			.collect(Collectors.toCollection(() -> collectPermissions));
	}
}
