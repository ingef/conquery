package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

public interface EntityResolver {

	/**
	 * Helper method to try and resolve entities in values using the specified format.
	 */
	ResolveStatistic resolveEntities(
			@NotEmpty String[][] values,
			@NotEmpty List<String> format,
			EntityIdMap mapping,
			IdColumnConfig idColumnConfig,
			@NotNull DateReader dateReader,
			boolean onlySingles
	);

	@Data
	class ResolveStatistic {

		@JsonIgnore
		private final Map<String, CDateSet> resolved;

		/**
		 * Entity -> Column -> Values
		 */
		@JsonIgnore
		private final Map<String, Map<String, List<String>>> extra;

		private final List<String[]> unreadableDate;
		private final List<String[]> unresolvedId;

		public static ResolveStatistic forEmptyReaders(String[][] values) {
			return new ResolveStatistic(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), List.of(values));
		}

	}

}
