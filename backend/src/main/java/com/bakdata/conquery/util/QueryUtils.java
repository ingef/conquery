package com.bakdata.conquery.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.NamespacedIdentifiableHolding;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.google.common.base.Strings;
import com.google.common.collect.MoreCollectors;
import lombok.Getter;
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
	public static <T extends QueryVisitor> T getVisitor(List<QueryVisitor> visitors, Class<T> clazz) {
		return (T) visitors.stream().filter(clazz::isInstance).collect(MoreCollectors.onlyElement());
	}

	/**
	 * Create label for children preferring the children's user label if provided.
	 */
	public static String createUserMultiLabel(List<CQElement> elements, String delimiter, String postfix, Locale locale) {
		return elements.stream().map(elt -> elt.userLabel(locale)).collect(Collectors.joining(delimiter,"", postfix));
	}
	/**
	 * Create label for children the children's default label ignoring user provided label.
	 */
	public static String createDefaultMultiLabel(List<CQElement> elements, String delimiter, String postfix, Locale locale) {
		return elements.stream().map(elt -> elt.defaultLabel(locale)).collect(Collectors.joining(delimiter, "", postfix));
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
		String label = cqConcept.userLabel(cfg.getLocale());

		if (label == null) {
			label = cqConcept.getConcept().getLabel();
		}

		// Concat everything with dashes
		return label.replace(" ", "-");
	}
	
	/**
	 * Checks if the query requires to resolve external ids.
	 */
	public static class ExternalIdChecker implements QueryVisitor {

		private final List<CQElement> elements = new ArrayList<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQExternal) {
				elements.add((CQElement) element);
			}
		}

		/***
		 * @return True if a {@link CQExternal} is found.
		 */
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
				containsOthersElements = ((CQReusedQuery) element).isExcludeFromSecondaryId();

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
	@Getter
	public static class AvailableSecondaryIdCollector implements QueryVisitor {

		private final Set<SecondaryIdDescriptionId> ids = new HashSet<>();

		@Override
		public void accept(Visitable element) {
			if(element instanceof final CQConcept cqConcept){

				// Excluded Concepts are not available
				if(cqConcept.isExcludeFromSecondaryId()){
					return;
				}

				for (Connector connector : cqConcept.getConcept().getConnectors()) {
					for (Column column : connector.getResolvedTable().getColumns()) {
						if(column.getSecondaryId() == null){
							continue;
						}

						ids.add(column.getSecondaryId());
					}
				}
			}
		}
	}
}
