package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.databind.JsonNode;

public class StoredQueriesProcessor {
        
        private final Namespaces namespaces;

        public StoredQueriesProcessor(Namespaces namespaces) {
                this.namespaces = namespaces;
        }
        
	public List<SQStatus> getAllQueries(Dataset dataset, URLBuilder fromRequest) {
                // TODO Auto-generated method stub
		return null;
	}

	public void deleteQuery(Dataset dataset, ManagedQuery query) {
		// TODO Auto-generated method stub
		
	}

	public SQStatus patchQuery(Dataset dataset, UUID uuid, JsonNode patch) {
		// TODO Auto-generated method stub
		return null;
	}

	public SQStatus getQueryWithSource(Dataset dataset, UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}

}
