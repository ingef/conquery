package com.bakdata.conquery.models.forms.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Manipulates a given Concept based on the provided blacklisting or
 * whitelisting for {@link ConceptSelectId} and {@link CQTable}s. After this
 * filtering, the defined default values are added if the corresponding list is
 * empty.
 *
 * For whitelisted tables a {@link TableManipulator} can be defined, that
 * applies the filtering on specific tables.
 */
@Getter
@Setter
@ToString
@Builder(builderClassName = "ConceptManipulatorInternalBuilder", builderMethodName = "internalBuilder")
public class FilteringConceptManipulator implements ConceptManipulator{

	@Builder.Default
	private List<ConceptSelectId> selectBlacklist = Collections.emptyList();
	@Builder.Default
	private List<ConceptSelectId> selectWhitelist = Collections.emptyList();
	@Builder.Default
	private List<ConceptSelectId> selectDefault = Collections.emptyList();

	@Builder.Default
	private List<ConnectorId> tableBlacklist = Collections.emptyList();
	@Builder.Default
	private Map<ConnectorId, TableManipulator> tableWhitelist = Collections.emptyMap();
	@Builder.Default
	private List<CQTable> tableDefault = Collections.emptyList();

	private void init() {

		if (!selectBlacklist.isEmpty() && !selectWhitelist.isEmpty()) {
			throw new IllegalArgumentException("Either select blacklist or whitelist needs to be empty.");
		}

		Set<ConceptSelectId> blackDefaultSelectIntersection = selectBlacklist
			.stream()
			.distinct()
			.filter(selectDefault::contains)
			.collect(Collectors.toSet());
		if (!blackDefaultSelectIntersection.isEmpty()) {
			throw new IllegalArgumentException(
				String
					.format(
						"The list of default selects intersects with the blacklist. Intersecting Elements:\t",
						blackDefaultSelectIntersection.toString()));
		}

		// Check tables
		if (!tableBlacklist.isEmpty() && !tableWhitelist.isEmpty()) {
			throw new IllegalArgumentException("Either table blacklist or whitelist needs to be empty.");
		}

		Set<ConnectorId> blackDefaultTableIntersection = tableDefault
			.stream()
			.map(CQTable::getId)
			.distinct()
			.filter(tableBlacklist::contains)
			.collect(Collectors.toSet());
		if (!blackDefaultTableIntersection.isEmpty()) {
			throw new IllegalArgumentException(
				String
					.format(
						"The list of default tables intersects with the blacklist. Intersecting Elements:\t",
						blackDefaultTableIntersection.toString()));
		}
	}

	public void consume(CQConcept concept, Namespaces namespaces) {

		List<Select> selects = concept.getSelects();
		if (!selectBlacklist.isEmpty()) {
			selects.removeIf(s -> selectBlacklist.contains(s.getId()));
		}
		else if (!selectWhitelist.isEmpty()) {
			selects.removeIf(s -> !selectWhitelist.contains(s.getId()));
		}

		// Add default selects if none is present anymore
		if (selects.isEmpty()) {
			concept.setSelects(selectDefault.stream().map(namespaces::resolve).collect(Collectors.toList()));
		}

		// Handle tables
		List<CQTable> tables = concept.getTables();
		Iterator<CQTable> it = tables.iterator();
		while(it.hasNext()) {
			CQTable table = it.next();
			if (tableBlacklist.contains(table.getId())) {
				it.remove();
			}
			if (!tableWhitelist.containsKey(table.getId())) {
				it.remove();
			}
			else {
				// If table is whitelisted apply a table manipulator if one exist
				TableManipulator tableMan = tableWhitelist.get(table.getId());
				if (tableMan != null) {
					tableMan.consume(table, namespaces);
				}
			}
		}
		if(tables.isEmpty()) {
			throw new IllegalStateException(String.format("After filtering the tables of concept %s, no table was left in the concept. ConceptManipulator: %s", concept, this.toString()));
		}
	}

	public static ConceptManipulatorBuilder builder() {
		return new ConceptManipulatorBuilder();
	}

	public static class ConceptManipulatorBuilder extends ConceptManipulatorInternalBuilder {

		public ConceptManipulatorBuilder() {
			super();
		}

		@Override
		public FilteringConceptManipulator build() {
			FilteringConceptManipulator foo = super.build();
			foo.init();
			return foo;
		}
	}

}
