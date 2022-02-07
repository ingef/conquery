package com.bakdata.conquery.models.config;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
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

/**
 * Configuration class for QueryUpload and IdMapping.
 *
 * Describes how rows are mapped for {@link EntityIdMap}/{@link AdminDatasetProcessor#setIdMapping(java.io.InputStream, com.bakdata.conquery.models.worker.Namespace)}.
 */
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

		if (getLength() == -1 || getPad() == null) {
			return new EntityIdMap.ExternalId(getName(), value);
		}

		String padded = StringUtils.leftPad(value, getLength(), getPad());

		return new EntityIdMap.ExternalId(getName(), padded);

	}

	/**
	 * Name of the Column-Config to be used when resolving with Upload.
	 */
	@NotEmpty
	private String name;

	/**
	 * Map of Localized labels.
	 */
	@NotEmpty
	@Builder.Default
	private Map<String, String> label = Collections.emptyMap();

	/**
	 * Map of Localized description.
	 */
	@Builder.Default
	private Map<String, String> description = Collections.emptyMap();

	/**
	 * Name of column in csv for {@link AdminDatasetProcessor#setIdMapping(java.io.InputStream, com.bakdata.conquery.models.worker.Namespace)}.
	 *
	 * Also Name of output column for {@link FrontendConfig.UploadConfig#getIdResultInfos()}, ergo output csv-Columns.
	 */
	private String field;

	/**
	 * Pad-String when uploading data, this avoids problems with some tools truncating leading zeros or similar.
	 */
	private String pad = null;

	/**
	 * In conjunction with pad, the length of the padded string.
	 */
	@Builder.Default
	private int length = -1;

	/**
	 * Set to true, if the column should be resolvable in upload. This can be used to add supplemental information to an entity, for example it's data-source, which would not be unique among entities.
	 */
	@Builder.Default
	private boolean resolvable = false;

	/**
	 * Set to true, if the Column should be printed to output. This can be used to have resolvable but not printable fields in mapping.
	 */
	@Builder.Default
	private boolean print = true;

	/**
	 * Used in conjunction with {@link com.bakdata.conquery.models.identifiable.mapping.AutoIncrementingPseudomizer}: One column is required to have fillAnon true, which will be filled with pseudomized data.
	 */
	@Builder.Default
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
