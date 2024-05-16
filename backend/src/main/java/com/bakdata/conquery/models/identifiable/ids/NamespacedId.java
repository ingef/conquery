package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * Interface for {@link Id}s that reference objects related to a namespace and reference directly the corresponding {@link com.bakdata.conquery.models.datasets.Dataset}.
 */
public interface NamespacedId {

	@JsonIgnore
	DatasetId getDataset();

	default String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IdUtil.JOIN_CHAR);
	}
}