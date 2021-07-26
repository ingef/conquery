package com.bakdata.conquery.models.config;

import java.util.Collections;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor @Setter
@Getter
public class ColumnConfig {


	public EntityIdMap.ExternalId read(String value) {
		if (!isResolvable()) {
			return null;
		}

		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		if (getLength() == -1) {
			return new EntityIdMap.ExternalId(new String[]{getName(), value});
		}

		String padded = StringUtils.leftPad(value, getLength(), getPad());

		return new EntityIdMap.ExternalId(new String[]{getName(), padded});

	}


	@NotEmpty
	private String name;

	/**
	 * Map of Localized labels.
	 */

	private Map<String, String> label = Collections.emptyMap();

	/**
	 * Map of Localized description.
	 */
	private Map<String, String> description = Collections.emptyMap();


	private String field;

	private String pad = null;


	private int length = -1;

	private boolean resolvable = false;


	private boolean fillAnon = false;

}
