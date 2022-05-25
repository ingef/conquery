package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * Marker interface for {@link Id}s that are loaded via Namespaced CentralRegistry (see {@link com.bakdata.conquery.models.worker.IdResolveContext#findRegistry(DatasetId)}, as opposed to MetaRegistry (see {@link IdResolveContext#getMetaRegistry()}).
 */
public interface NamespacedId {

	@JsonIgnore
	DatasetId getDataset();

	default String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IdUtil.JOIN_CHAR);
	}
}