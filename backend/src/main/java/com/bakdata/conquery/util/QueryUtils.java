package com.bakdata.conquery.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import c10n.C10N;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.NamespacedIdentifiableHolding;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.google.common.base.Strings;
import com.google.common.collect.ClassToInstanceMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class QueryUtils {

	private static final int MAX_CONCEPT_LABEL_CONCAT_LENGTH = 70;
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

	public static String createDefaultMultiLabel(List<CQElement> elements, String delimiter, Locale locale) {
			return elements.stream().map(elt -> elt.getUserOrDefaultLabel(locale)).collect(Collectors.joining(delimiter));
	}

	public static String createTotalDefaultMultiLabel(List<CQElement> elements, String delimiter, Locale locale) {
		return elements.stream().map(elt -> elt.defaultLabel(locale)).collect(Collectors.joining(delimiter));
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
			if (element instanceof CQExternal) {
				elements.add((CQElement) element);
			}
		}

		public boolean resolvesExternalIds() {
			return !elements.isEmpty();
		}
	}

	/**
	 * Test if this query is only reusing a different query (ie not combining it with other elements, or changing its secondaryId)
	 */
	public static class OnlyReusingChecker implements QueryVisitor {

		private CQReusedQuery reusedQuery = null;
		private boolean containsOthersElements = false;

		@Override
		public void accept(Visitable element) {
			if(containsOthersElements){
				return;
			}

			if (element instanceof CQReusedQuery) {
				// We would have to reason way too much about the sent query so we just reexecute it.
				containsOthersElements = containsOthersElements || ((CQReusedQuery) element).isExcludeFromSecondaryId();

				if (reusedQuery == null) {
					reusedQuery = (CQReusedQuery) element;
				}
				else {
					containsOthersElements = true;
				}
				return;
			}

			if (!(element instanceof CQAnd || element instanceof CQOr || element instanceof QueryDescription)) {
				containsOthersElements = true;
			}
		}

		public Optional<ManagedExecutionId> getOnlyReused() {
			if (containsOthersElements || reusedQuery == null) {
				return Optional.empty();
			}

			return Optional.of(reusedQuery.getQueryId());
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
	 * Collects all {@link NamespacedIdentifiable} provided by a user from a
	 * {@link Visitable}.
	 */
	public static class NamespacedIdentifiableCollector implements QueryVisitor {

		@Getter
		private final Set<NamespacedIdentifiable<?>> identifiables = new HashSet<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof NamespacedIdentifiableHolding idHolder) {
				idHolder.collectNamespacedObjects(identifiables);
			}
		}
	}

	/**
	 * Collects all {@link NamespacedId} references provided by a user from a
	 * {@link Visitable}.
	 */
	public static class AvailableSecondaryIdCollector implements QueryVisitor {

		@Getter
		private final Set<SecondaryIdDescription> ids = new HashSet<>();

		@Override
		public void accept(Visitable element) {
			if(element instanceof final CQConcept cqConcept){

				// Excluded Concepts are not available
				if(cqConcept.isExcludeFromSecondaryId()){
					return;
				}

				for (Connector connector : cqConcept.getConcept().getConnectors()) {
					for (Column column : connector.getTable().getColumns()) {
						if(column.getSecondaryId() == null){
							continue;
						}

						ids.add(column.getSecondaryId());
					}
				}
			}
		}
	}
	
	public static void generateConceptReadPermissions(@NonNull QueryUtils.NamespacedIdentifiableCollector idCollector, @NonNull Collection<ConqueryPermission> collectPermissions){
		idCollector.getIdentifiables().stream()
				   .filter(id -> id instanceof ConceptElement)
				   .map(ConceptElement.class::cast)
				   .map(ConceptElement::getConcept)
				   .map(cId -> cId.createPermission(Ability.READ.asSet()))
				   .distinct()
				   .collect(Collectors.toCollection(() -> collectPermissions));
	}



	public static QueryExecutionContext determineDateAggregatorForContext(QueryExecutionContext ctx, Supplier<Optional<Aggregator<CDateSet>>> altValidityDateAggregator) {
		if (ctx.getQueryDateAggregator().isPresent()) {
			return ctx;
		}
		return ctx.withQueryDateAggregator(altValidityDateAggregator.get());
	}

	public static String makeQueryLabel(final Visitable query, PrintSettings cfg, ManagedExecutionId id) {
		final StringBuilder sb = new StringBuilder();

		final Map<Class<? extends Visitable>, List<Visitable>> sortedContents =
				Visitable.stream(query)
						 .collect(Collectors.groupingBy(Visitable::getClass));

		int sbStartSize = sb.length();

		// Check for CQExternal
		List<Visitable> externals = sortedContents.getOrDefault(CQExternal.class, Collections.emptyList());
		if (!externals.isEmpty()) {
			if (!sb.isEmpty()) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).external());
		}

		// Check for CQReused
		if (sortedContents.containsKey(CQReusedQuery.class)) {
			if (!sb.isEmpty()) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).reused());
		}


		// Check for CQConcept
		if (sortedContents.containsKey(CQConcept.class)) {
			if (!sb.isEmpty()) {
				sb.append(" ");
			}
			// Track length of text we are appending for concepts.
			final AtomicInteger length = new AtomicInteger();

			sortedContents.get(CQConcept.class)
						  .stream()
						  .map(CQConcept.class::cast)

						  .map(c -> makeLabelWithRootAndChild(c, cfg))
						  .filter(Predicate.not(Strings::isNullOrEmpty))
						  .distinct()

						  .takeWhile(elem -> length.addAndGet(elem.length()) < MAX_CONCEPT_LABEL_CONCAT_LENGTH)
						  .forEach(label -> sb.append(label).append(" "));

			// Last entry will output one Space that we don't want
			if (!sb.isEmpty()) {
				sb.deleteCharAt(sb.length() - 1);
			}

			// If not all Concept could be included in the name, point that out
			if (length.get() > MAX_CONCEPT_LABEL_CONCAT_LENGTH) {
				sb.append(" ").append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).furtherConcepts());
			}
		}


		// Fallback to id if nothing could be extracted from the query description
		if (sbStartSize == sb.length()) {
			sb.append(id.getExecution());
		}

		return sb.toString();
	}


	private static String makeLabelWithRootAndChild(CQConcept cqConcept, PrintSettings cfg) {
		String label = cqConcept.getUserOrDefaultLabel(cfg.getLocale());
		if (label == null) {
			label = cqConcept.getConcept().getLabel();
		}

		// Concat everything with dashes
		return label.replace(" ", "-");
	}
}
