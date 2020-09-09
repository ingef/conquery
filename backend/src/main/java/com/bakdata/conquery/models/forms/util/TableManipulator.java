package com.bakdata.conquery.models.forms.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Manipulates a given Table based on the provided blacklisting or whitelisting
 * for {@link ConnectorSelectId}s. After this filtering, the defined default
 * values are added if the corresponding list is empty.
 */
@Getter
@Setter
@Builder(builderClassName = "TableManipulatorInternalBuilder", builderMethodName = "internalBuilder")
public class TableManipulator {

	@Builder.Default
	private List<ConnectorSelectId> selectBlacklist = Collections.emptyList();
	@Builder.Default
	private List<ConnectorSelectId> selectWhitelist = Collections.emptyList();
	@Builder.Default
	private List<ConnectorSelectId> selectDefault = Collections.emptyList();

	private void init() {

		if (!selectBlacklist.isEmpty() && !selectWhitelist.isEmpty()) {
			throw new IllegalArgumentException("Either blacklist or whitelist needs to be empty.");
		}

		Set<ConnectorSelectId> blackDefaultIntersection = selectBlacklist
			.stream()
			.distinct()
			.filter(selectDefault::contains)
			.collect(Collectors.toSet());
		if (!blackDefaultIntersection.isEmpty()) {
			throw new IllegalArgumentException(
				String
					.format(
						"The list of default selects intersects with the blacklist. Intersecting Elements:\t",
						blackDefaultIntersection.toString()));
		}

	}

	public void consume(CQTable table, DatasetRegistry namespaces) {

		List<Select> selects = table.getSelects();
		if (!selectBlacklist.isEmpty()) {
			selects.removeIf(s -> selectBlacklist.contains(s.getId()));
		}
		else if (!selectWhitelist.isEmpty()) {
			selects.removeIf(s -> !selectWhitelist.contains(s.getId()));
		}

		// Add default selects if none is present anymore
		if (selects.isEmpty()) {
			table.setSelects(selectDefault.stream().map(namespaces::resolve).collect(Collectors.toList()));
		}
	}

	public static TableManipulatorBuilder builder() {
		return new TableManipulatorBuilder();
	}

	public static class TableManipulatorBuilder extends TableManipulatorInternalBuilder {

		public TableManipulatorBuilder() {
			super();
		}

		@Override
		public TableManipulator build() {
			// The builder is called indirectly using this method to ensure its members are
			// properly set by ensuring that the init funciton is called.
			TableManipulator tm = super.build();
			tm.init();
			return tm;
		}
	}

}
