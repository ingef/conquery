package com.bakdata.conquery.models.config;

import java.util.Collections;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.google.common.base.Strings;
import io.dropwizard.logback.shaded.checkerframework.checker.nullness.qual.Nullable;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class ColumnConfig {

	@Data
	@Builder
	public static class Mapping {
		private final String field;

		@Builder.Default
		private final String pad = null;
		@Builder.Default
		private final int length = -1;

		@Builder.Default
		private final boolean resolvable = true;

		@Builder.Default
		private final boolean fillAnon = false;

	}

	public EntityIdMap.ExternalId read(String value) {
		if(!mapping.isResolvable()){
			return null; //TODO throw Exception?
		}

		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		if (getMapping().getLength() == -1) {
			return new EntityIdMap.ExternalId(new String[]{getName(), value});
		}

		String padded = StringUtils.leftPad(value, getMapping().getLength(), getMapping().getPad());

		return new EntityIdMap.ExternalId(new String[]{getName(), padded});

	}


	@NotEmpty
	private final String name;

	/**
	 * Map of Localized labels.
	 */
	@Builder.Default
	private final Map<String, String> label = Collections.emptyMap();

	/**
	 * Map of Localized description.
	 */
	@Builder.Default
	private final Map<String, String> description = Collections.emptyMap();

	@Nullable
	private final Mapping mapping;

}
