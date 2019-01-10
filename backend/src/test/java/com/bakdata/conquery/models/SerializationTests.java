package com.bakdata.conquery.models;

import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public class SerializationTests {

	@Test
	public void dataset() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("dataset");
		
		SerializationTestUtil.testSerialization(dataset, Dataset.class);
	}
	
	@Test
	public void mandator() throws IOException, JSONException{
		SinglePrincipalCollection principals = new SinglePrincipalCollection(new MandatorId("company"));
		Mandator mandator = new Mandator(principals);
		mandator.setLabel("company");
		
		SerializationTestUtil.testSerialization(mandator, Mandator.class);
		
	}
	
	@Test
	public void user() throws IOException, JSONException{
		SinglePrincipalCollection principals = new SinglePrincipalCollection(new UserId("user"));
		User user = new User(principals);
		user.setLabel("user");
		
		SerializationTestUtil.testSerialization(user, User.class);
		
	}
	
	@Test
	public void datasetPermission() throws IOException, JSONException{
		DatasetPermission permission = new DatasetPermission(new UserId("user"), EnumSet.of(Ability.READ), new DatasetId("dataset"));
		
		SerializationTestUtil.testSerialization(permission, DatasetPermission.class);
		
	}
	
	@Test
	public void queryPermission() throws IOException, JSONException{
		QueryPermission permission = new QueryPermission(new UserId("user"), EnumSet.of(Ability.READ), new ManagedQueryId(new DatasetId("dataset"), UUID.randomUUID()));

		SerializationTestUtil.testSerialization(permission, QueryPermission.class);
		
	}
}
