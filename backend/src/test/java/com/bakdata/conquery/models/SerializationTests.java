package com.bakdata.conquery.models;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMapSerialisationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
	public void passwordCredential() throws IOException, JSONException{
		PasswordCredential credential = new PasswordCredential(new String("testPassword").toCharArray());
		
		SerializationTestUtil
			.forType(PasswordCredential.class)
			.test(credential);
	}
	
	@Test
	public void mandator() throws IOException, JSONException{
		Role mandator = new Role("company", "company");
		
		SerializationTestUtil
			.forType(Role.class)
			.test(mandator);
	}
	
	/*
	 * Only way to add permission without a storage.
	 */
	@Test
	public void user() throws IOException, JSONException{
		MasterMetaStorage storage = mock(MasterMetaStorage.class);
		User user = new User("user", "user");
		user.addPermission(storage, DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		user
			.addPermission(
				storage,
				QueryPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		Role role = new Role("company", "company");
		user.addRole(storage, role);

		CentralRegistry registry = new CentralRegistry();
		registry.register(role);
		
		SerializationTestUtil
			.forType(User.class)
			.registry(registry)
			.test(user);
	}
	
	@Test
	public void group() throws IOException, JSONException {
		MasterMetaStorage storage = mock(MasterMetaStorage.class);
		Group group = new Group("group", "group");
		group.addPermission(storage, DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		group
			.addPermission(
				storage,
				QueryPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		group.addRole(storage, new Role("company", "company"));

		Role role = new Role("company", "company");
		group.addRole(storage, role);
		User user = new User("userName", "userLabel");
		group.addMember(storage, user);

		CentralRegistry registry = new CentralRegistry();
		registry.register(role);
		registry.register(user);

		SerializationTestUtil.forType(Group.class).registry(registry).test(group);
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

	@Test
	public void persistentIdMap() throws JSONException, IOException {
		SerializationTestUtil.forType(PersistentIdMap.class)
			.test(IdMapSerialisationTest.createTestPersistentMap());

	}
	
	@Test
	public void formConfig() throws JSONException, IOException {

		ExportForm form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		mode.setForm(form);
		mode.setFeatures(List.of(new CQConcept()));

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		
		SerializationTestUtil
			.forType(FormConfig.class)
			.test(formConfig);
	}
	
	@Test
	public void managedQuery() throws JSONException, IOException {
		
		ManagedQuery execution = new ManagedQuery(null, new UserId("test-user"), new DatasetId("test-dataset"));
		execution.setTags(new String[] {"test-tag"});
		
		SerializationTestUtil
			.forType(ManagedExecution.class)
			.test(execution);
	}
}
