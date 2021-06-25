package com.bakdata.conquery.util;

import javax.ws.rs.NotFoundException;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.NonNull;

//TODO remove this class
public class ResourceUtil {

	private final DatasetRegistry namespaces;

	public ResourceUtil(DatasetRegistry namespaces) {
		this.namespaces = namespaces;
	}

	public static void throwNotFoundIfNull(@NonNull IId<?> id, Object identifiable) {
		if (identifiable == null) {
			throw new NotFoundException(id.toString());
		}
	}

	public Dataset getDataset(DatasetId id) {
		return namespaces.get(id).getStorage().getDataset();
	}

}
