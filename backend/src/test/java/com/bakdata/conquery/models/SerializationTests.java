package com.bakdata.conquery.models;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.types.MajorTypeId;

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
		DatasetPermission permission = new DatasetPermission(Ability.READ.asSet(), new DatasetId("dataset"));
		
		SerializationTestUtil
			.forType(DatasetPermission.class)
			.test(permission);
	}
	
	@Test
	public void queryPermission() throws IOException, JSONException{
		QueryPermission permission = new QueryPermission(Ability.READ.asSet(), new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID()));

		SerializationTestUtil
			.forType(QueryPermission.class)
			.test(permission);
	}
	

	@Test
	public void treeConcept() throws IOException, JSONException{
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");
		
		TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset.getId());
		concept.setLabel("conceptLabel");
		concept.setName("conceptName");
		
		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setPosition(2);
		column.setType(MajorTypeId.DATE);
		
		Table table = new Table();
		table.setColumns(new Column[]{column});
		table.setDataset(dataset);
		table.setLabel("tableLabel");
		table.setName("tableName");
		table.setPrimaryColumn(column);
		table.setTags(Set.of("tag"));
		
		column.setTable(table);
		
		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setLabel("connLabel");
		connector.setName("connName");
		connector.setColumn(column);
		
		concept.setConnectors(List.of(connector));
		
		ValidityDate valDate = new ValidityDate();
		valDate.setColumn(column);
		valDate.setConnector(connector);
		valDate.setLabel("valLabel");
		valDate.setName("valName");
		connector.setValidityDates(List.of(valDate));
		
		CentralRegistry registry = new CentralRegistry();
		
		registry.register(dataset);
		registry.register(concept);
		registry.register(column);
		registry.register(table);
		registry.register(connector);
		registry.register(valDate);
		
		SerializationTestUtil
			.forType(TreeConcept.class)
			.registry(registry)
			.test(concept);
	}
}
