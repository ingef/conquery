package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;

// todo(tm): choose implementation
public interface NamespaceProvider<N extends Namespace> {

	// todo(tm): proper implementation with check
	@SuppressWarnings("unchecked")
	default N provide(Namespace namespace) {
		return (N) namespace;
	}

	// todo(tm): Example for a somewhat more elegant solution than casting: Move DatasetRegistry into ExecutionManager and use datasetid to resolve namespace
	//           instead of passing it as a method parameter
	default N provide(DatasetId id) {
		// return myDatasetRegistry.get(id)
		return null;
	}

}
