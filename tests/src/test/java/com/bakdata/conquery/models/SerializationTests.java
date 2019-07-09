package com.bakdata.conquery.models;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.SerializationTestUtil;

public class SerializationTests {

	@Test
	public void dataset() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("dataset");
		
		SerializationTestUtil
			.forType(Dataset.class)
			.test(dataset);
	}
	
	@Test
	public void mandator() throws IOException, JSONException{
		Mandator mandator = new Mandator("company", "company");
		
		SerializationTestUtil
			.forType(Mandator.class)
			.test(mandator);
	}
	
	@Test
	public void user() throws IOException, JSONException{
		User user = new User("user", "user");
		
		SerializationTestUtil
			.forType(User.class)
			.test(user);
	}
	
	@Test
	public void datasetPermission() throws IOException, JSONException{
		DatasetPermission permission = new DatasetPermission(new UserId("user"), Ability.READ.asSet(), new DatasetId("dataset"));
		
		SerializationTestUtil
			.forType(DatasetPermission.class)
			.test(permission);
	}
	
	@Test
	public void queryPermission() throws IOException, JSONException{
		QueryPermission permission = new QueryPermission(new UserId("user"), Ability.READ.asSet(), new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID()));

		SerializationTestUtil
			.forType(QueryPermission.class)
			.test(permission);
	}
}
