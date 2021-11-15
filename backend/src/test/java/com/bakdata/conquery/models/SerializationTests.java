package com.bakdata.conquery.models;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.MeProcessor;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.primitive.ShortArrayStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeCompound;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMapSerialisationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class SerializationTests {

	private final static MetaStorage STORAGE = new NonPersistentStoreFactory().createMetaStorage();

	@Test
	public void dataset() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("dataset");

		SerializationTestUtil
				.forType(Dataset.class)
				.test(dataset);
	}

	@Test
	public void passwordCredential() throws IOException, JSONException {
		PasswordCredential credential = new PasswordCredential(new String("testPassword").toCharArray());

		SerializationTestUtil
				.forType(PasswordCredential.class)
				.test(credential);
	}

	@Test
	public void role() throws IOException, JSONException {
		Role mandator = new Role("company", "company", STORAGE);

		SerializationTestUtil
				.forType(Role.class)
				.injectables(STORAGE)
				.test(mandator);
	}

	/*
	 * Only way to add permission without a storage.
	 */
	@Test
	public void user() throws IOException, JSONException {
		User user = new User("user", "user", STORAGE);
		user.addPermission(DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		user.addPermission(ExecutionPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		Role role = new Role("company", "company", STORAGE);
		user.addRole(role);

		CentralRegistry registry = new CentralRegistry();
		registry.register(role);

		SerializationTestUtil
				.forType(User.class)
				.registry(registry)
				.injectables(STORAGE)
				.test(user);
	}

	@Test
	public void group() throws IOException, JSONException {
		Group group = new Group("group", "group", STORAGE);
		group.addPermission(DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		group.addPermission(ExecutionPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		group.addRole(new Role("company", "company", STORAGE));

		Role role = new Role("company", "company", STORAGE);
		group.addRole(role);
		User user = new User("userName", "userLabel", STORAGE);
		group.addMember(user);

		CentralRegistry registry = new CentralRegistry();
		registry.register(role);
		registry.register(user);

		SerializationTestUtil
				.forType(Group.class)
				.injectables(STORAGE)
				.registry(registry)
				.test(group);
	}


	@Test
	public void bucketCompoundDateRange() throws JSONException, IOException {
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");

		Table table = new Table();

		Column startCol = new Column();
		startCol.setName("startCol");
		startCol.setType(MajorTypeId.DATE);
		startCol.setTable(table);

		Column endCol = new Column();
		endCol.setLabel("endLabel");
		endCol.setName("endCol");
		endCol.setType(MajorTypeId.DATE);
		endCol.setTable(table);


		Column compoundCol = new Column();
		compoundCol.setName("compoundCol");
		compoundCol.setType(MajorTypeId.DATE_RANGE);
		compoundCol.setTable(table);

		table.setColumns(new Column[]{startCol, endCol, compoundCol});
		table.setDataset(dataset);
		table.setName("tableName");


		Import imp = new Import(table);
		imp.setName("importTest");

		DateRangeTypeCompound compoundStore = new DateRangeTypeCompound(startCol.getName(), endCol.getName(), BitSetStore.create(4));

		ColumnStore startStore = new IntegerDateStore(new ShortArrayStore(new short[]{1, 2, 3, 4}, Short.MIN_VALUE));
		ColumnStore endStore = new IntegerDateStore(new ShortArrayStore(new short[]{5, 6, 7, 8}, Short.MIN_VALUE));

		Bucket bucket = new Bucket(0, 1, 4, new ColumnStore[]{startStore, endStore, compoundStore}, Collections.emptySet(), new int[0], new int[0], imp);

		compoundStore.setParent(bucket);


		CentralRegistry registry = new CentralRegistry();

		registry.register(dataset);
		registry.register(startCol);
		registry.register(endCol);
		registry.register(compoundCol);
		registry.register(table);
		registry.register(imp);
		registry.register(bucket);


		final Validator validator = Validators.newValidator();

		SerializationTestUtil
				.forType(Bucket.class)
				.registry(registry)
				.injectables(new Injectable() {
					@Override
					public MutableInjectableValues inject(MutableInjectableValues values) {
						return values.add(Validator.class, validator);
					}
				})
				.test(bucket);

	}


	@Test
	public void table() throws JSONException, IOException {
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");

		Table table = new Table();

		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setType(MajorTypeId.STRING);
		column.setTable(table);


		table.setColumns(new Column[]{column});
		table.setDataset(dataset);
		table.setLabel("tableLabel");
		table.setName("tableName");


		CentralRegistry registry = new CentralRegistry();

		registry.register(dataset);
		registry.register(table);
		registry.register(column);

		final Validator validator = Validators.newValidator();

		SerializationTestUtil
				.forType(Table.class)
				.registry(registry)
				.injectables(new Injectable() {
					@Override
					public MutableInjectableValues inject(MutableInjectableValues values) {
						return values.add(Validator.class, validator);
					}
				})
				.test(table);
	}

	@Test
	public void treeConcept() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");

		TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setLabel("conceptLabel");
		concept.setName("conceptName");

		Table table = new Table();

		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setType(MajorTypeId.STRING);
		column.setTable(table);

		Column dateColumn = new Column();
		dateColumn.setLabel("colLabel2");
		dateColumn.setName("colName2");
		dateColumn.setType(MajorTypeId.DATE);
		dateColumn.setTable(table);


		table.setColumns(new Column[]{column, dateColumn});
		table.setDataset(dataset);
		table.setLabel("tableLabel");
		table.setName("tableName");

		column.setTable(table);

		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setLabel("connLabel");
		connector.setName("connName");
		connector.setColumn(column);

		concept.setConnectors(List.of(connector));

		ValidityDate valDate = new ValidityDate();
		valDate.setColumn(dateColumn);
		valDate.setConnector(connector);
		valDate.setLabel("valLabel");
		valDate.setName("valName");
		connector.setValidityDates(List.of(valDate));

		CentralRegistry registry = new CentralRegistry();

		registry.register(dataset);
		registry.register(concept);
		registry.register(column);
		registry.register(dateColumn);
		registry.register(table);
		registry.register(connector);
		registry.register(valDate);

		final Validator validator = Validators.newValidator();
		concept.setValidator(validator);

		SerializationTestUtil
				.forType(TreeConcept.class)
				.registry(registry)
				.injectables(new Injectable() {
					@Override
					public MutableInjectableValues inject(MutableInjectableValues values) {
						return values.add(Validator.class, validator);
					}
				})
				.test(concept);
	}

	@Test
	public void persistentIdMap() throws JSONException, IOException {
		SerializationTestUtil.forType(EntityIdMap.class)
							 .test(IdMapSerialisationTest.createTestPersistentMap());

	}

	@Test
	public void formConfig() throws JSONException, IOException {
		final CentralRegistry registry = new CentralRegistry();

		final Dataset dataset = new Dataset("test-dataset");

		registry.register(dataset);

		ExportForm form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		mode.setForm(form);
		mode.setFeatures(List.of(new CQConcept()));

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setDataset(dataset);

		SerializationTestUtil
				.forType(FormConfig.class)
				.registry(registry)
				.test(formConfig);
	}

	@Test
	public void managedQuery() throws JSONException, IOException {

		final CentralRegistry registry = new CentralRegistry();

		final Dataset dataset = new Dataset("test-dataset");

		final User user = new User("test-user", "test-user", STORAGE);

		registry.register(dataset);
		registry.register(user);

		ManagedQuery execution = new ManagedQuery(null, user, dataset);
		execution.setTags(new String[]{"test-tag"});

		SerializationTestUtil.forType(ManagedExecution.class)
							 .registry(registry)
							 .test(execution);
	}

	@Test
	public void cqConcept() throws JSONException, IOException {

		final Dataset dataset = new Dataset();
		dataset.setName("dataset");

		final TreeConcept concept = new TreeConcept();
		concept.setName("concept");
		concept.setDataset(dataset);

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		concept.setConnectors(List.of(connector));

		final CQConcept cqConcept = new CQConcept();
		cqConcept.setElements(List.of(concept));
		cqConcept.setLabel("Label");

		final CQTable cqTable = new CQTable();
		cqTable.setConnector(connector);
		cqTable.setFilters(List.of());
		cqTable.setConcept(cqConcept);

		cqConcept.setTables(List.of(cqTable));

		final CentralRegistry registry = new CentralRegistry();
		registry.register(dataset);
		registry.register(concept);
		registry.register(connector);

		SerializationTestUtil
				.forType(CQConcept.class)
				.registry(registry)
				.test(cqConcept);
	}

	@Test
	public void executionCreationPlanError() throws JSONException, IOException {
		ConqueryError error = new ConqueryError.ExecutionCreationPlanError();

		SerializationTestUtil
				.forType(ConqueryError.class)
				.test(error);
	}

	@Test
	public void executionCreationResolveError() throws JSONException, IOException {
		ConqueryError error = new ConqueryError.ExecutionCreationResolveError(new DatasetId("test"));

		SerializationTestUtil
				.forType(ConqueryError.class)
				.test(error);
	}


	@Test
	public void executionQueryJobError() throws JSONException, IOException {
		log.info("Beware, this test will print an ERROR message.");
		ConqueryError error = new ConqueryError.ExecutionJobErrorWrapper(new Entity(5), new ConqueryError.UnknownError(null));

		SerializationTestUtil
				.forType(ConqueryError.class)
				.test(error);
	}

	@Test
	public void meInformation() throws IOException, JSONException {
		User user = new User("name", "labe", STORAGE);

		MeProcessor.FEMeInformation info = MeProcessor.FEMeInformation.builder()
																	  .userName(user.getLabel())
																	  .hideLogoutButton(false)
																	  .groups(List.of(new IdLabel<>(new GroupId("test_group"), "test_group_label")))
																	  .datasetAbilities(Map.of(new DatasetId("testdataset"), new MeProcessor.FEDatasetAbility(true)))
																	  .build();

		SerializationTestUtil
				.forType(MeProcessor.FEMeInformation.class)
				.test(info);
	}

	@Test
	public void testFormQuery() throws IOException, JSONException {
		CQConcept concept = new CQConcept();
		final TreeConcept testConcept = new TreeConcept();
		Dataset dataset = new Dataset();
		dataset.setName("testDataset");
		testConcept.setDataset(dataset);
		testConcept.setName("concept");
		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(testConcept);
		connector.setName("connector1");

		testConcept.setConnectors(List.of(connector));

		concept.setElements(Collections.singletonList(testConcept));
		CQTable[] tables = {new CQTable()};
		connector.setTable(new Table());
		tables[0].setConnector(connector);
		tables[0].setConcept(concept);
		concept.setTables(Arrays.asList(tables));
		ConceptQuery subQuery = new ConceptQuery(concept);


		CQOr features = new CQOr();
		features.setChildren(Collections.singletonList(concept));


		AbsoluteFormQuery query = new AbsoluteFormQuery(
				subQuery,
				CDateRange.exactly(LocalDate.now()).toSimpleRange(),
				ArrayConceptQuery.createFromFeatures(Collections.singletonList(features)),
				List.of(
						ExportForm.ResolutionAndAlignment.of(Resolution.COMPLETE, Alignment.NO_ALIGN),
						ExportForm.ResolutionAndAlignment.of(Resolution.QUARTERS, Alignment.QUARTER)
				)
		);

		CentralRegistry centralRegistry = new CentralRegistry();
		centralRegistry.register(dataset);
		centralRegistry.register(testConcept);
		centralRegistry.register(connector);

		SerializationTestUtil.forType(AbsoluteFormQuery.class).registry(centralRegistry).test(query);
	}

}
