package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.databind.JsonNode;

public class Processor {

	public SQStatus getQueryWithSource(User user, Dataset dataset, UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	public SQStatus patchQuery(User user, Dataset dataset, UUID uuid, JsonNode patch) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<SQStatus> getAllQueries(User user, Dataset dataset, Object fromRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteQuery(User user, Dataset dataset, UUID uuid) {
		// TODO Auto-generated method stub
		
	}

}
