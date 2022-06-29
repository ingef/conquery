package com.bakdata.conquery.apiv1.frontend;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a concept filter as it is presented to the front end.
 */
@Data
@Builder
public class FEFilter {

	@NotNull
	private FilterId id;
	/**
	 * User readable name of the Filter.
	 */
	@NotEmpty
	private String label;
	/**
	 * Kind of filter: Communicates to the frontend which UI element to use and what values are valid.
	 */
	@NotEmpty
	private String type;
	/**
	 * Used as display unit for enumerations etc in UI elements.
	 */
	private String unit;

	/**
	 * Displayed on hover for filters.
	 */
	private String tooltip;
	@Builder.Default
	private List<FEValue> options = Collections.emptyList();

	/**
	 * min value for range filters.
	 */
	private Integer min;
	/**
	 * max value for range filters.
	 */
	private Integer max;

	/**
	 * TODO Is this used by the frontend?
	 */
	private FilterTemplate template;
	private String pattern;
	/**
	 * If true, enables users to use drag and drop files into the filter element (usually for {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}).
	 */
	private Boolean allowDropFile;
	/**
	 * If true, user can manually insert their input. At the moment only true for SelectFilter without any enabled backing searches.
	 */
	private Boolean creatable;
	/**
	 * If set, default value used for the filter by the frontend.
	 */
	@Nullable
	private Object defaultValue;

	/**
	 * When {@link com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter} is used, the sub-fields of this filter.
	 */
	private Map<String, @Valid FEFilter> filters;
}
