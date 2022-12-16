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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This class represents a concept filter as it is presented to the front end.
 */
@SuperBuilder
@Getter
@Setter
@ToString
@NoArgsConstructor
public abstract class FrontendFilterConfiguration {

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
	private List<FrontendValue> options = Collections.emptyList();

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
	 * Special class for the fields of the frontend representation of a {@link com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter}. These
	 * fields don't have an id compared to {@link Top}.
	 * <p>
	 * The reason to have this class are:
	 * <ul>
	 *     <li>No validation error for group fields</li>
	 *     <li>A clearer separation between the serialized top-level object {@link Top} and possible nested {@link Nested}s. This way, an {@link Top} can not be nested (which is not supported by the frontend)</li>
	 * </ul>
	 */
	@SuperBuilder
	@Getter
	@Setter
	@ToString(callSuper = true)
	@NoArgsConstructor
	public static class Nested extends FrontendFilterConfiguration {

	}

	/**
	 * The top-level object for filter configurations that are processed by the frontend.
	 * <p>
	 * It allows to have nested {@link Nested}s for complex {@link com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter}.
	 */
	@SuperBuilder
	@Getter
	@Setter
	@ToString(callSuper = true)
	@NoArgsConstructor
	public static class Top extends FrontendFilterConfiguration {

		@NotNull
		private FilterId id;

		/**
		 * When {@link com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter} is used, the sub-fields of this filter.
		 */
		private Map<String, @Valid Nested> filters;
	}
}
