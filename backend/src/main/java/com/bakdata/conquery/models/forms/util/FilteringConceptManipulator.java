package com.bakdata.conquery.models.forms.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Manipulates a given Concept based on the provided blocklisting or
 * allowlisting for {@link ConceptSelectId} and {@link CQTable}s. After this
 * filtering, the defined default values are added if the corresponding list is
 * empty.
 *
 * For allowlisted tables a {@link TableManipulator} can be defined, that
 * applies the filtering on specific tables.
 */
@Getter
@Setter
@ToString
@Builder(builderClassName = "ConceptManipulatorInternalBuilder", builderMethodName = "internalBuilder")
@Deprecated
public class FilteringConceptManipulator implements ConceptManipulator{

	@Builder.Default
	private List<ConceptSelectId> selectBlockList = Collections.emptyList();
	@Builder.Default
	private List<ConceptSelectId> selectAllowList = Collections.emptyList();
	@Builder.Default
	private List<ConceptSelectId> selectDefault = Collections.emptyList();

	@Builder.Default
	private List<ConnectorId> tableBlockList = Collections.emptyList();
	@Builder.Default
	private Map<ConnectorId, TableManipulator> tableAllowList = Collections.emptyMap();
	@Builder.Default
	private List<CQTable> tableDefault = Collections.emptyList();

	private void init() {

		if (!selectBlockList.isEmpty() && !selectAllowList.isEmpty()) {
			throw new IllegalArgumentException("Either select blocklist or allowlist needs to be empty.");
		}

		Set<ConceptSelectId> blockDefaultSelectIntersection = selectBlockList
																	  .stream()
																	  .filter(selectDefault::contains)
																	  .collect(Collectors.toSet());

		if (!blockDefaultSelectIntersection.isEmpty()) {
			throw new IllegalArgumentException(
				String
					.format(
						"The list of default selects intersects with the blocklist. Intersecting Elements:\t%s",
						blockDefaultSelectIntersection.toString()));
		}

		// Check tables
		if (!tableBlockList.isEmpty() && !tableAllowList.isEmpty()) {
			throw new IllegalArgumentException("Either table blocklist or allowlist needs to be empty.");
		}

		Set<ConnectorId> blockDefaultTableIntersection = tableDefault
																 .stream()
																 .map(CQTable::getConnector)
			.map(Connector::getId)
																 .filter(tableBlockList::contains)
																 .collect(Collectors.toSet());

		if (!blockDefaultTableIntersection.isEmpty()) {
			throw new IllegalArgumentException(
				String
					.format(
						"The list of default tables intersects with the blocklist. Intersecting Elements:\t%s",
						blockDefaultTableIntersection.toString()));
		}
	}

	public void consume(CQConcept concept, DatasetRegistry namespaces) {

		List<Select> selects = concept.getSelects();
		if (!selectBlockList.isEmpty()) {
			selects.removeIf(s -> selectBlockList.contains(s.getId()));
		}
		else if (!selectAllowList.isEmpty()) {
			selects.removeIf(s -> !selectAllowList.contains(s.getId()));
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
			if (tableBlockList.contains(table.getConnector().getId())) {
				it.remove();
			}
			if (!tableAllowList.containsKey(table.getConnector().getId())) {
				it.remove();
			}
			else {
				// If table is allowlisted apply a table manipulator if one exist
				TableManipulator tableMan = tableAllowList.get(table.getConnector().getId());
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
