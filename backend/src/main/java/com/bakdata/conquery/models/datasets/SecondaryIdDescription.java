package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used to tag Columns as being of the same kind, this is used in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan} to enable grouping by secondary Columns.
 */
@NoArgsConstructor
@Setter
@Getter
@JsonIgnoreProperties({"searchDisabled", "generateSearchSuffixes", "searchMinSuffixLength"})
public class SecondaryIdDescription extends Labeled<SecondaryIdDescriptionId> implements NamespacedIdentifiable<SecondaryIdDescriptionId> {

	@NsIdRef
	private Dataset dataset;

	private String description;

	/**
	 * Determines whether this column should be used to group events in {@link com.bakdata.conquery.resources.api.QueryResource.EntityPreview} by the frontend.
	 *
	 * This is mostly useful for edge-cases, where certain SecondaryIds are only relevant for {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}.
	 */
	private boolean grouped = true;

	@Override
	public SecondaryIdDescriptionId createId() {
		return new SecondaryIdDescriptionId(dataset.getId(), getName());
	}

	@Override
	public String toString() {
		return "SecondaryIdDescription(id = " + getId() + ", label = " + getLabel() + " )";
	}
}
