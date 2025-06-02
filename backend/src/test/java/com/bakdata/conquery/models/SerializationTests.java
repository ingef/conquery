package com.bakdata.conquery.models;

import static com.bakdata.conquery.util.SerialisationObjectsUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.MeProcessor;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.io.AbstractSerializationTest;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.external.form.FormBackendVersion;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.WorkerStorageImpl;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.primitive.ShortArrayStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.CompoundDateRangeStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.IdMapSerialisationTest;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.dropwizard.jersey.validation.Validators;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.RecursiveComparisonAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


@Slf4j
public class SerializationTests extends AbstractSerializationTest {

	public static Stream<Range<Integer>> rangeData() {
		final int SEED = 7;
		Random random = new Random(SEED);
		return Stream
				.generate(() -> {
					int first = random.nextInt();
					int second = random.nextInt();

					if (first < second) {
						return Range.of(first, second);
					}
					return Range.of(second, first);
				})
				.filter(Range::isOrdered)
				.flatMap(range -> Stream.of(
						range,
						Range.exactly(range.getMin()),
						Range.atMost(range.getMin()),
						Range.atLeast(range.getMin())
				))
				.filter(Range::isOrdered)
				.limit(100);
	}

	@Test
	public void dataset() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("dataset");
		dataset.setLabel("Dataset");
		dataset.setStorageProvider(getDatasetRegistry());

		SerializationTestUtil
				.forType(Dataset.class)
				.objectMappers(getManagerInternalMapper(), getShardInternalMapper())
				.customizingAssertion(assertion -> assertion.ignoringFields("storageProvider"))
				.test(dataset);
	}

	@Test
	public void passwordCredential() throws IOException, JSONException {
		PasswordCredential credential = new PasswordCredential("testPassword");

		SerializationTestUtil
				.forType(PasswordCredential.class)
				.objectMappers(getManagerInternalMapper())
				.test(credential);
	}

	@Test
	public void role() throws IOException, JSONException {
		Role mandator = new Role("company", "company", getMetaStorage());

		SerializationTestUtil
				.forType(Role.class)
				.objectMappers(getManagerInternalMapper(), getApiMapper())
				.test(mandator);
	}

	/*
	 * Only way to add permission without a storage.
	 */
	@Test
	public void user() throws IOException, JSONException {
		User user = new User("user", "user", getMetaStorage());
		user.setMetaStorage(getMetaStorage());
		user.addPermission(DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		user.addPermission(ExecutionPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		Role role = new Role("company", "company", getMetaStorage());

		getMetaStorage().addRole(role);

		user.addRole(role.getId());

		SerializationTestUtil
				.forType(User.class)
				.objectMappers(getManagerInternalMapper(), getApiMapper())
				.injectables(getMetaStorage())
				.test(user);
	}

	@Test
	public void group() throws IOException, JSONException {
		Group group = new Group("group", "group", getMetaStorage());
		group.addPermission(DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		group.addPermission(ExecutionPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));

		Role role = new Role("company", "company", getMetaStorage());
		RoleId roleId = role.getId();

		group.addRole(roleId);

		group.addRole(roleId);
		User user = new User("userName", "userLabel", getMetaStorage());
		group.addMember(user.getId());

		final MetaStorage metaStorage = getMetaStorage();
		metaStorage.addRole(role);
		metaStorage.addUser(user);

		SerializationTestUtil
				.forType(Group.class)
				.objectMappers(getManagerInternalMapper(), getApiMapper())
				.test(group);
	}

	@Test
	@Tag("OBJECT_2_INT_MAP") // Bucket uses Object2IntMap
	public void bucketCompoundDateRange() throws JSONException, IOException {

		Dataset dataset = new Dataset("datasetName");
		dataset.setStorageProvider(getShardNamespacedStorageProvider());

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
		table.setDataset(dataset.getId());
		table.setName("tableName");

		Import imp = new Import("importTest", table.getId());

		CompoundDateRangeStore compoundStore =
				new CompoundDateRangeStore(startCol.getName(), endCol.getName(), new BitSetStore(BitSet.valueOf(new byte[]{0b1000}), new BitSet(), 4));
		//0b1000 is a binary representation of 8 so that the 4th is set to make sure that BitSet length is 4.

		ColumnStore startStore = new IntegerDateStore(new ShortArrayStore(new short[]{1, 2, 3, 4}, Short.MIN_VALUE));
		ColumnStore endStore = new IntegerDateStore(new ShortArrayStore(new short[]{5, 6, 7, 8}, Short.MIN_VALUE));

		Bucket bucket =
				new Bucket(0, Object2IntMaps.singleton("0", 0), Object2IntMaps.singleton("0", 4), 4, imp.getId(), new ColumnStore[]{startStore, endStore, compoundStore});

		compoundStore.setParent(bucket);

		getWorkerStorage().updateDataset(dataset);
		getWorkerStorage().addTable(table);
		getWorkerStorage().addImport(imp);
		getWorkerStorage().addBucket(bucket);

		final Validator validator = Validators.newValidator();

		SerializationTestUtil
				.forType(Bucket.class)
				.objectMappers(getShardInternalMapper())
				.injectables(values -> values.add(Validator.class, validator)
											 .add(NamespacedStorageProvider.class, getShardNamespacedStorageProvider()))
				.test(bucket);

	}

	@Test
	public void table() throws JSONException, IOException {
		{
			// Manager
			Dataset dataset = createDataset(getNamespaceStorage(), getDatasetRegistry());

			Table table = getTable(dataset);

			table.init();

			SerializationTestUtil
					.forType(Table.class)
					.objectMappers(getNamespaceInternalMapper(), getApiMapper())
					.test(table);
		}

		{
			// Shard
			Dataset dataset = createDataset(getWorkerStorage(), getShardNamespacedStorageProvider());

			Table table = getTable(dataset);

			getWorkerStorage().addTable(table);

			SerializationTestUtil
					.forType(Table.class)
					.objectMappers(getShardInternalMapper())
					.test(table);
		}
	}

	private static @NotNull Table getTable(Dataset dataset) {
		Table table = new Table();

		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setType(MajorTypeId.STRING);
		column.setTable(table);


		table.setColumns(new Column[]{column});
		table.setDataset(dataset.getId());
		table.setLabel("tableLabel");
		table.setName("tableName");
		return table;
	}

	@Test
	public void filterValueMoneyRange() throws JSONException, IOException {
		FilterValue.CQMoneyRangeFilter filterValue =
				new FilterValue.CQMoneyRangeFilter(FilterId.Parser.INSTANCE.parse("dataset.concept.connector.filter"), new Range.LongRange(2000L, 30000L));

		filterValue.setConfig(getConfig());

		SerializationTestUtil
				.forType(FilterValue.class)
				.objectMappers(getNamespaceInternalMapper(), getApiMapper())
				.test(filterValue);
	}

	@Test
	public void treeConcept() throws IOException, JSONException {
		{
			// Manager
			final Dataset dataset = createDataset(getNamespaceStorage(), getDatasetRegistry());
			TreeConcept concept = createConcept(dataset, getNamespaceStorage());

			SerializationTestUtil
					.forType(Concept.class)
					.objectMappers(getNamespaceInternalMapper(), getApiMapper())
					.injectables(dataset, getDatasetRegistry())
					.test(concept);
		}

		{
			// Shard
			final Dataset dataset = createDataset(getWorkerStorage(), getShardNamespacedStorageProvider());
			TreeConcept concept = createConcept(dataset, getWorkerStorage());

			SerializationTestUtil
					.forType(Concept.class)
					.objectMappers(getShardInternalMapper())
					.test(concept);

		}
	}

	@Test
	public void persistentIdMap() throws JSONException, IOException {
		EntityIdMap persistentMap = IdMapSerialisationTest.createTestPersistentMap(getNamespaceStorage());

		SerializationTestUtil.forType(EntityIdMap.class)
							 .objectMappers(getNamespaceInternalMapper(), getApiMapper())
							 .test(persistentMap);

	}

	@Test
	public void formConfig() throws JSONException, IOException {

		final Dataset dataset = createDataset(getNamespaceStorage(), getDatasetRegistry());

		ExportForm form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		mode.setForm(form);

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setDataset(dataset.getId());

		SerializationTestUtil
				.forType(FormConfig.class)
				.objectMappers(getManagerInternalMapper(), getApiMapper())
				.test(formConfig);
	}

	@Test
	public void managedQuery() throws JSONException, IOException {

		final Dataset dataset = new Dataset("test-dataset");

		final User user = new User("test-user", "test-user", getMetaStorage());

		getNamespaceStorage().updateDataset(dataset);
		getMetaStorage().updateUser(user);

		ManagedQuery execution = new ManagedQuery(null, user.getId(), dataset.getId(), getMetaStorage(), getDatasetRegistry(), getConfig());
		execution.setTags(new String[]{"test-tag"});

		// Trigger UUID creation
		execution.getId();

		SerializationTestUtil.forType(ManagedExecution.class)
							 .objectMappers(getManagerInternalMapper(), getApiMapper())
							 .injectables(getMetaStorage())
							 .test(execution);
	}

	@Test
	public void testExportForm() throws JSONException, IOException {

		final Dataset dataset = createDataset(getNamespaceStorage(), getDatasetRegistry());

		final ExportForm exportForm = createExportForm(dataset, getNamespaceStorage());

		SerializationTestUtil.forType(QueryDescription.class)
							 .objectMappers(getManagerInternalMapper(), getApiMapper())
							 .checkHashCode()
							 .test(exportForm);
	}

	@Test
	public void managedForm() throws JSONException, IOException {

		final Dataset dataset = createDataset(getNamespaceStorage(), getDatasetRegistry());

		final User user = createUser(getMetaStorage());
		final ExportForm exportForm = createExportForm(dataset, getNamespaceStorage());

		ManagedInternalForm<ExportForm> execution =
				new ManagedInternalForm<>(exportForm, user.getId(), dataset.getId(), getMetaStorage(), getDatasetRegistry(), getConfig());
		execution.setTags(new String[]{"test-tag"});

		// Trigger UUID creation
		execution.getId();

		SerializationTestUtil.forType(ManagedExecution.class)
							 .objectMappers(getManagerInternalMapper(), getApiMapper())
							 .injectables(getDatasetRegistry())
							 .test(execution);
	}

	@Test
	public void testExternalExecution() throws IOException, JSONException {

		final String subType = "test-type";
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode node = new ObjectNode(factory,
										 Map.of(
												 "some-other-member", new TextNode("some-other-value")
										 )
		);

		ExternalForm form = new ExternalForm(node, subType);
		final Dataset dataset = createDataset(getNamespaceStorage(), getDatasetRegistry());
		final User user = createUser(getMetaStorage());

		final ExternalExecution execution = new ExternalExecution(form, user.getId(), dataset.getId(), getMetaStorage(), getDatasetRegistry(), getConfig());

		// Trigger UUID creation
		execution.getId();

		SerializationTestUtil.forType(ManagedExecution.class)
							 .objectMappers(getManagerInternalMapper())
							 .injectables(getDatasetRegistry())
							 .test(execution);

	}

	@Test
	public void cqConcept() throws JSONException, IOException {
		{
			// Manager
			final CQConcept cqConcept = createCqConcept(getNamespaceStorage(), getDatasetRegistry());

			SerializationTestUtil
					.forType(CQConcept.class)
					.objectMappers(getManagerInternalMapper(), getApiMapper())
					.test(cqConcept);
		}

		{
			// Shard
			final CQConcept cqConcept = createCqConcept(getWorkerStorage(), getShardNamespacedStorageProvider());

			SerializationTestUtil
					.forType(CQConcept.class)
					.objectMappers(getShardInternalMapper())
					.test(cqConcept);
		}
	}

	@NotNull
	private static CQConcept createCqConcept(NamespacedStorage namespaceStorage, NamespacedStorageProvider storageProvider) {
		Dataset dataset = createDataset(namespaceStorage, storageProvider);

		final TreeConcept concept = new TreeConcept();
		concept.setName("concept");
		concept.setDataset(dataset.getId());

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setName("connector");
		concept.setConnectors(List.of(connector));

		final CQConcept cqConcept = new CQConcept();
		cqConcept.setElements(List.of(concept.getId()));
		cqConcept.setLabel("Label");

		final CQTable cqTable = new CQTable();
		cqTable.setConnector(connector.getId());
		cqTable.setFilters(List.of());
		cqTable.setConcept(cqConcept);

		cqConcept.setTables(List.of(cqTable));
		namespaceStorage.updateConcept(concept);

		return cqConcept;
	}

	@Test
	public void executionCreationPlanError() throws JSONException, IOException {

		I18n.init();

		ConqueryError error = new ConqueryError.ExecutionProcessingError();

		SerializationTestUtil
				.forType(ConqueryError.class)
				.objectMappers(getManagerInternalMapper(), getShardInternalMapper(), getApiMapper())
				.test(error);
	}

	@Test
	public void executionCreationResolveError() throws JSONException, IOException {
		ConqueryError error = new ConqueryError.ExecutionCreationResolveError(new DatasetId("test"));

		SerializationTestUtil
				.forType(ConqueryError.class)
				.objectMappers(getManagerInternalMapper(), getShardInternalMapper(), getApiMapper())
				.test(error);
	}

	@Test
	public void executionQueryJobError() throws JSONException, IOException {
		log.info("Beware, this test will print an ERROR message.");
		ConqueryError error = new ConqueryError.ExecutionJobErrorWrapper(new Entity("5"), new ConqueryError.UnknownError(null));

		SerializationTestUtil
				.forType(ConqueryError.class)
				.objectMappers(getManagerInternalMapper(), getShardInternalMapper(), getApiMapper())
				.test(error);
	}

	@Test
	public void meInformation() throws IOException, JSONException {
		User user = new User("name", "labe", getMetaStorage());

		MeProcessor.FrontendMeInformation info = MeProcessor.FrontendMeInformation.builder()
																				  .userName(user.getLabel())
																				  .hideLogoutButton(false)
																				  .groups(List.of(new IdLabel<>(new GroupId("test_group"), "test_group_label")))
																				  .datasetAbilities(Map.of(new DatasetId("testdataset"),
																										   new MeProcessor.FrontendDatasetAbility(true, true, true)
																				  ))
																				  .build();

		SerializationTestUtil
				.forType(MeProcessor.FrontendMeInformation.class)
				.objectMappers(getManagerInternalMapper(), getApiMapper())
				.test(info);
	}

	@Test
	public void testFormQuery() throws IOException, JSONException {
		CQConcept concept = new CQConcept();
		final TreeConcept testConcept = new TreeConcept();
		Dataset dataset = new Dataset("testDataset");
		dataset.setStorageProvider(getDatasetRegistry());

		testConcept.setDataset(dataset.getId());
		testConcept.setName("concept");

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(testConcept);
		connector.setName("connector1");

		testConcept.setConnectors(List.of(connector));

		concept.setElements(Collections.singletonList(testConcept.getId()));
		CQTable[] tables = {new CQTable()};
		Table table = new Table();
		table.setDataset(dataset.getId());
		connector.setTable(table.getId());
		tables[0].setConnector(connector.getId());
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

		getNamespaceStorage().updateDataset(dataset);
		getNamespaceStorage().updateConcept(testConcept);

		WorkerStorageImpl workerStorage = getWorkerStorage();
		workerStorage.updateDataset(dataset);
		workerStorage.updateConcept(testConcept);

		SerializationTestUtil
				.forType(AbsoluteFormQuery.class)
				.objectMappers(getManagerInternalMapper(), getShardInternalMapper(), getApiMapper())
				.test(query);
	}

	@Test
	public void cBlock() throws IOException, JSONException {

		final Dataset dataset = new Dataset();
		dataset.setStorageProvider(getShardNamespacedStorageProvider());
		dataset.setName("dataset");

		final TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset.getId());
		concept.setName("concept");

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("connector");

		connector.setConcept(concept);
		concept.setConnectors(List.of(connector));

		final Table table = new Table();
		table.setName("table");
		table.setDataset(dataset.getId());

		final Import imp = new Import("import", table.getId());

		getWorkerStorage().updateDataset(dataset);
		getWorkerStorage().addTable(table);
		getWorkerStorage().updateConcept(concept);
		getWorkerStorage().addImport(imp);

		final Bucket bucket = new Bucket(0, Object2IntMaps.emptyMap(), Object2IntMaps.emptyMap(), 0, imp.getId(), new ColumnStore[0]);

		getWorkerStorage().addBucket(bucket);

		final CBlock cBlock = new CBlock(bucket.getId(), connector.getId(), Collections.emptyMap(), Collections.emptyMap(), new int[0][]);

		SerializationTestUtil.forType(CBlock.class)
							 .objectMappers(getShardInternalMapper())
							 .test(cBlock);
	}

	@Test
	public void testBiMapSerialization() throws JSONException, IOException {
		BiMap<String, String> map = HashBiMap.create();
		map.put("a", "1");
		map.put("b", "2");
		SerializationTestUtil
				.forType(new TypeReference<BiMap<String, String>>() {
				})
				.objectMappers(getApiMapper(), getManagerInternalMapper())
				.test(map);
	}

	@Test
	public void testNonStrictNumbers() throws JSONException, IOException {
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper()).test(Double.NaN, null);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper()).test(Double.NEGATIVE_INFINITY, null);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper()).test(Double.POSITIVE_INFINITY, null);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper()).test(Double.MAX_VALUE);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper()).test(Double.MIN_VALUE);
		SerializationTestUtil
				.forType(EntityResult.class)
				.objectMappers(getApiMapper(), getManagerInternalMapper())
				.test(
						new MultilineEntityResult("4", List.of(
								new Object[]{0, 1, 2},
								new Object[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}
						)
						),
						new MultilineEntityResult("4", List.of(
								new Object[]{0, 1, 2},
								new Object[]{null, null, null}
						)
						)
				);
	}

	@ParameterizedTest
	@MethodSource("rangeData")
	public void test(Range<Integer> range) throws IOException, JSONException {
		SerializationTestUtil
				.forType(new TypeReference<Range<Integer>>() {
				})
				.objectMappers(getApiMapper(), getManagerInternalMapper(), getShardInternalMapper())
				.test(range);
	}

	@Test
	public void locale() throws JSONException, IOException {
		SerializationTestUtil.forType(Locale.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper())
							 .test(Locale.GERMANY);
	}

	@Test
	public void localeArray() throws JSONException, IOException {
		SerializationTestUtil.forType(Locale[].class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper())
							 .test(new Locale[]{Locale.GERMANY, Locale.ROOT, Locale.ENGLISH, Locale.US, Locale.UK});
	}

	/**
	 * Tests if the type id prefix for external forms is correctly removed before a form is forwarded to a form backend.
	 */
	@Test
	public void externalForm() throws IOException, JSONException {

		final String externalFormString = """
				{
					"type": "EXTERNAL_FORM@SOME_SUB_TYPE",
					"title": "Test Form",
					"members": {
						"val1": [0,1,2,3],
						"val2": "hello"
					}
				}
				""";

		ExternalForm externalForm = getApiMapper().readerFor(QueryDescription.class).readValue(externalFormString);
		SerializationTestUtil.forType(QueryDescription.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper())
							 .test(externalForm);
	}

	/**
	 * Extra nesting test because {@link ExternalForm} uses a custom deserializer.
	 */
	@Test
	public void externalFormArray() throws IOException, JSONException {

		final String externalFormString = """
				{
					"type": "EXTERNAL_FORM@SOME_SUB_TYPE",
					"title": "Test Form",
					"members": {
						"val1": [0,1,2,3],
						"val2": "hello"
					}
				}
				""";

		final String externalFormString2 = """
				{
					"type": "EXTERNAL_FORM@SOME_SUB_TYPE2",
					"title": "Test Form",
					"members": {
						"val1": [0,1,2,3],
						"val2": "hello"
					}
				}
				""";

		ExternalForm externalForm = getApiMapper().readerFor(QueryDescription.class).readValue(externalFormString);
		ExternalForm externalForm2 = getApiMapper().readerFor(QueryDescription.class).readValue(externalFormString2);
		SerializationTestUtil.forType(QueryDescription[].class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper())
							 .test(new QueryDescription[]{externalForm, externalForm2});
	}


	/**
	 * Tests if the type id prefix for external forms is correctly removed before a form is forwarded to a form backend.
	 */
	@Test
	public void externalFormToFormBackend() throws JsonProcessingException {

		final String externalFormString = """
				{
					"type": "EXTERNAL_FORM@SOME_SUB_TYPE",
					"title": "Test Form",
					"members": {
						"val1": [0,1,2,3],
						"val2": "hello"
					}
				}
				""";

		ExternalForm externalForm = getApiMapper().readerFor(QueryDescription.class).readValue(externalFormString);

		final ObjectMapper objectMapper = FormBackendConfig.configureObjectMapper(getApiMapper().copy());
		final String actual = objectMapper.writer().writeValueAsString(externalForm);

		final String expected = """
				{
					"type":"SOME_SUB_TYPE",
					"title":"Test Form",
					"members":{
						"val1":[0,1,2,3],
						"val2":"hello"
					}
				}
				""".replaceAll("[\\n\\t]", "");
		assertThat(actual).as("Result of mixin for form backend").isEqualTo(expected);
	}

	@Test
	@Tag("OBJECT_2_INT_MAP")
	public void object2IntEmpty() throws JSONException, IOException {
		Object2IntMap<String> empty = Object2IntMaps.emptyMap();

		SerializationTestUtil.forType(new TypeReference<Object2IntMap<String>>() {
							 })
							 .objectMappers(getApiMapper(), getShardInternalMapper(), getManagerInternalMapper())
							 .customizingAssertion(RecursiveComparisonAssert::ignoringCollectionOrder)
							 .test(empty);

	}

	@Test
	@Tag("OBJECT_2_INT_MAP")
	public void object2IntString() throws JSONException, IOException {
		Object2IntMap<String> map = new Object2IntOpenHashMap<>();

		map.put("zero", 0);
		map.put("one", 1);
		map.put("two", 2);
		SerializationTestUtil.forType(new TypeReference<Object2IntMap<String>>() {
							 })
							 .objectMappers(getApiMapper(), getShardInternalMapper(), getManagerInternalMapper())
							 .customizingAssertion(RecursiveComparisonAssert::ignoringCollectionOrder)
							 .test(map);

	}

	@Test
	@Tag("OBJECT_2_INT_MAP")
	public void arrayObject2Int() throws JSONException, IOException {
		Object2IntMap<String>[] map = new Object2IntOpenHashMap[]{
				new Object2IntOpenHashMap<>() {{
					put("zero", 0);
				}},
				new Object2IntOpenHashMap<>() {{
					put("zero", 0);
					put("one", 1);
				}},
				new Object2IntOpenHashMap<>() {{
					put("zero", 0);
					put("one", 1);
					put("two", 2);
				}}
		};
		SerializationTestUtil.forArrayType(new TypeReference<Object2IntMap<String>>() {
							 }).objectMappers(getApiMapper(), getShardInternalMapper(), getManagerInternalMapper())
							 .customizingAssertion(RecursiveComparisonAssert::ignoringCollectionOrder)
							 .test(map);

	}

	@Test
	public void formBackendVersion() throws JSONException, IOException {
		final FormBackendVersion version = new FormBackendVersion("3.45.45-g85ut85u43t8", ZonedDateTime.parse("2007-12-03T10:15:30+00:00"));

		SerializationTestUtil.forType(FormBackendVersion.class)
							 .objectMappers(getApiMapper(), getManagerInternalMapper())
							 .test(version);
	}

}
