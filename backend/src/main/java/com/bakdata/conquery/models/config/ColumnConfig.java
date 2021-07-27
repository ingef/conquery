package com.bakdata.conquery.models.config;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import c10n.C10N;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
@Setter
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
	@NotEmpty
	private Map<String, String> label = Collections.emptyMap();

	/**
	 * Map of Localized description.
	 */
	private Map<String, String> description = Collections.emptyMap();

	@InternalOnly
	private String field;

	@InternalOnly
	private String pad = null;

	@InternalOnly
	private int length = -1;

	@InternalOnly
	private boolean resolvable = false;

	@InternalOnly
	private boolean fillAnon = false;

	@JsonIgnore
	@ValidationMethod(message = "Keys must be valid Locales.")
	public boolean isLabelKeysLocale() {
		return getLabel().keySet().stream().map(Locale::forLanguageTag).noneMatch(Objects::isNull);
	}

	@JsonIgnore
	@ValidationMethod(message = "Keys must be valid Locales.")
	public boolean isDescriptionKeysLocale() {
		return getDescription().keySet().stream().map(Locale::forLanguageTag).noneMatch(Objects::isNull);
	}

}
