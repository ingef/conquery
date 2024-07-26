package com.bakdata.conquery.api;

import static com.bakdata.conquery.models.execution.ExecutionState.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.OverviewExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.config.*;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.ExternalId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.SerialisationObjectsUtil;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.extentions.MetaStorageExtention;
import com.bakdata.conquery.util.extentions.UserExtension;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.univocity.parsers.csv.CsvParserSettings;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.validation.Validators;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


public class StoredQueriesProcessorTest {

	private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

	@RegisterExtension
	private static final MetaStorageExtention STORAGE_EXTENTION = new MetaStorageExtention(METRIC_REGISTRY);
	@RegisterExtension
	private static final UserExtension USER_0_EXTENSIONS = new UserExtension(STORAGE_EXTENTION.getMetaStorage(), "0");
	@RegisterExtension
	private static final UserExtension USER_1_EXTENSIONS = new UserExtension(STORAGE_EXTENTION.getMetaStorage(), "1");

	private static final Validator VALIDATOR = Validators.newValidator();
	public static final ConqueryConfig CONFIG = new ConqueryConfig(){{ setStorage(new NonPersistentStoreFactory());}};

	private static final MetaStorage STORAGE = STORAGE_EXTENTION.getMetaStorage();


	private static final DatasetRegistry<DistributedNamespace> datasetRegistry = new DatasetRegistry<>(0, CONFIG, new InternalObjectMapperCreator(CONFIG, VALIDATOR), new ClusterNamespaceHandler(new ClusterState(), CONFIG), new IndexService(new CsvParserSettings(), null));
	private static final QueryProcessor processor = new QueryProcessor(datasetRegistry, STORAGE, CONFIG, VALIDATOR);

	private static final Dataset DATASET_0 = new Dataset("dataset0");
	private static final Dataset DATASET_1 = new Dataset("dataset1");

	private static ManagedExecutionId QUERY_ID_0;
	private static ManagedExecutionId QUERY_ID_1;
	private static ManagedExecutionId QUERY_ID_2;
	private static ManagedExecutionId QUERY_ID_3;
	private static ManagedExecutionId QUERY_ID_4;
	private static ManagedExecutionId QUERY_ID_5;
	private static ManagedExecutionId QUERY_ID_6;
	private static ManagedExecutionId QUERY_ID_7;
	private static ManagedExecutionId QUERY_ID_8;
	private static ManagedExecutionId QUERY_ID_9;
	private static ManagedExecutionId QUERY_ID_10;

	public static final UriBuilder URI_BUILDER = UriBuilder.fromPath("http://localhost");

	private static final ExcelResultProvider EXCEL_RESULT_PROVIDER = new ExcelResultProvider();
	private static final CsvResultProvider CSV_RESULT_PROVIDER = new CsvResultProvider();
	private static final ArrowResultProvider ARROW_RESULT_PROVIDER = new ArrowResultProvider();
	private static final ParquetResultProvider PARQUET_RESULT_PROVIDER = new ParquetResultProvider();

	private static ManagedExecutionId createExecutionId(Dataset dataset0, String s) {
		StringBuilder idBuilder = new StringBuilder("00000000-0000-0000-0000-000000000000");
		idBuilder.replace(idBuilder.length() - s.length(), idBuilder.length(), s);

		return new ManagedExecutionId(dataset0.getId(), UUID.fromString(idBuilder.toString()));
	}


	private static final User[] USERS = new User[]{
			USER_0_EXTENSIONS.getUser(),
			USER_1_EXTENSIONS.getUser()
	};

	private static List<ManagedExecution> QUERIES;

	@BeforeAll
	public static void beforeAll() throws IOException {
		new AuthorizationController(STORAGE, CONFIG, new Environment(StoredQueriesProcessorTest.class.getSimpleName()), null);

		MetricRegistry metricRegistry = new MetricRegistry();
		DistributedNamespace namespace0 = datasetRegistry.createNamespace(DATASET_0, STORAGE, metricRegistry);
		DistributedNamespace namespace1 = datasetRegistry.createNamespace(DATASET_1, STORAGE, metricRegistry);

		NamespaceStorage namespaceStorage0 = namespace0.getStorage();
		NamespaceStorage namespaceStorage1 = namespace1.getStorage();

		EntityIdMap entityIdMap = new EntityIdMap();
		String idColumnName = CONFIG.getIdColumns().getIds().get(0).getName();
		entityIdMap.addInputMapping("0", new ExternalId(idColumnName, "0"));
		namespaceStorage0.updateIdMapping(entityIdMap);

		Concept<?> CONCEPT_0 = SerialisationObjectsUtil.createConcept(DATASET_0, namespaceStorage0);
		Concept<?> CONCEPT_1 = SerialisationObjectsUtil.createConcept(DATASET_1, namespaceStorage1);

		CONCEPT_0.setNsIdResolver(namespaceStorage0);
		namespaceStorage0.updateConcept(CONCEPT_0);
		CONCEPT_1.setNsIdResolver(namespaceStorage1);
		namespaceStorage1.updateConcept(CONCEPT_1);


		SecondaryIdDescription secondaryIdDescription0 = namespace0.getStorage().getSecondaryId(new SecondaryIdDescriptionId(DATASET_0.getId(), "sid"));


		QUERY_ID_0 = createExecutionId(DATASET_0, "0");
		QUERY_ID_1 = createExecutionId(DATASET_1, "1");
		QUERY_ID_2 = createExecutionId(DATASET_0, "2");
		QUERY_ID_3 = createExecutionId(DATASET_0, "3");
		QUERY_ID_4 = createExecutionId(DATASET_0, "4");
		QUERY_ID_5 = createExecutionId(DATASET_0, "5");
		QUERY_ID_6 = createExecutionId(DATASET_0, "6");
		QUERY_ID_7 = createExecutionId(DATASET_0, "7");
		QUERY_ID_8 = createExecutionId(DATASET_0, "8");
		QUERY_ID_9 = createExecutionId(DATASET_0, "9");
		QUERY_ID_10 = createExecutionId(DATASET_0, "10");

		User user0 = USER_0_EXTENSIONS.getUser();
		for (ManagedExecutionId id : List.of(QUERY_ID_0, QUERY_ID_1, QUERY_ID_2, QUERY_ID_4, QUERY_ID_7, QUERY_ID_9, QUERY_ID_10)) {

			user0.addPermission(ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR,id));
		}

		User user1 = USER_1_EXTENSIONS.getUser();
		for (ManagedExecutionId id : List.of(QUERY_ID_3, QUERY_ID_4)) {
			user1.addPermission(ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR,id));
		}

		CQConcept cqConcept = new CQConcept();
		cqConcept.setElements(List.of(CONCEPT_0.getId()));
		CQTable cqTable = new CQTable();
		cqTable.setConnector(CONCEPT_0.getConnectors().get(0).getId());
		cqConcept.setTables(List.of(cqTable));

		final String[][] externalValues= new String[][]{
				{idColumnName},
				{"0"}
		};
		QUERIES= ImmutableList.of(
				mockManagedConceptQueryFrontEnd(user0, QUERY_ID_0, NEW, CONCEPT_0, 100L),            // included
				mockManagedConceptQueryFrontEnd(user0, QUERY_ID_1, NEW, CONCEPT_1, 100L),            // not included: wrong dataset
				mockManagedForm(user0, QUERY_ID_2, NEW, DATASET_0),                            // not included: not a ManagedQuery
				mockManagedConceptQueryFrontEnd(user1, QUERY_ID_3, NEW, CONCEPT_0, 100L),         // not included: missing permission
				mockManagedConceptQueryFrontEnd(user1, QUERY_ID_4, DONE, CONCEPT_0, 100L),        // included
				mockManagedConceptQueryFrontEnd(user0, QUERY_ID_5, FAILED, CONCEPT_0, 100L),        // not included: wrong state
				mockManagedQuery(new AbsoluteFormQuery(null, null, null, null), user1, QUERY_ID_6, NEW, DATASET_0, 100L),                                                    // not included: wrong query structure
				mockManagedSecondaryIdQueryFrontEnd(user1, QUERY_ID_7, DONE, new CQAnd() {{
					setChildren(List.of(cqConcept));
				}}, secondaryIdDescription0),    // included, but secondaryId-Query
				mockManagedSecondaryIdQueryFrontEnd(user1, QUERY_ID_8, DONE, cqConcept, secondaryIdDescription0),    // not-included, wrong structure
				mockManagedQuery(new ConceptQuery(new CQExternal(List.of(idColumnName), externalValues, false)), user1, QUERY_ID_9, DONE, DATASET_0, 100L),        // included
				mockManagedConceptQueryFrontEnd(user1, QUERY_ID_10, DONE, CONCEPT_0, 2_000_000L)        // included, but no result url for xlsx (result has too many rows)

		);
	}


	@Test
	public void getQueriesFiltered() {

		List<ExecutionStatus> infos = processor.getQueriesFiltered(DATASET_0, URI_BUILDER, USERS[0], QUERIES.stream(), true)
											   .collect(Collectors.toList());

		assertThat(infos)
				.containsExactly(
						makeState(QUERY_ID_0, USERS[0], USERS[0], NEW, "CONCEPT_QUERY", null, 100L, true),
						makeState(QUERY_ID_4, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null, 100L, true),
						makeState(QUERY_ID_7, USERS[1], USERS[0], DONE, "SECONDARY_ID_QUERY", new SecondaryIdDescriptionId(DATASET_0.getId(), "sid"), 100L, true),
						makeState(QUERY_ID_9, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null, 100L, false),
						makeState(QUERY_ID_10, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null, 2_000_000L, true)

				);
	}

	private static ManagedForm mockManagedForm(User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset){
		return new ManagedInternalForm(new ExportForm(), user.getId(), dataset.getId()) {
			{
				setState(execState);
				setCreationTime(LocalDateTime.MIN);
				setQueryId(id.getExecution());
			}
		};
	}

	private static ManagedQuery mockManagedConceptQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, Concept<?> concept, long resultCount) {
		return mockManagedQuery(
				new ConceptQuery(
						new CQAnd() {{
							// short hand class initializer block to support visiting of CQAnd Children
							CQConcept cqConcept = new CQConcept();
							cqConcept.setElements(List.of(concept.getId()));
							setChildren(List.of(cqConcept));
						}}
				),
				user,
				id,
				execState, concept.getDataset().resolve(), resultCount
		);
	}
	private static ManagedQuery mockManagedSecondaryIdQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, CQElement root, SecondaryIdDescription secondaryIdDescription){
		final SecondaryIdQuery sid = new SecondaryIdQuery();

		sid.setSecondaryId(secondaryIdDescription.getId());
		sid.setRoot(root);

		return mockManagedQuery(sid, user, id, execState, secondaryIdDescription.getDataset().resolve(), 100L);
	}


	private static ManagedQuery mockManagedQuery(Query queryDescription, User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset, final long resultCount) {
		return new ManagedQuery(queryDescription, user.getId(), dataset.getId()) {
			{
				setState(execState);
				setCreationTime(LocalDateTime.MIN);
				setQueryId(id.getExecution());
				setLastResultCount(resultCount);
				setLabel(id.getExecution().toString());
			}

			@Override
			public List<ResultInfo> getResultInfos() {
				// With method is mocked because the ExcelResultProvider needs some info to check dimensions,
				// but actually resolving the query here requires much more setup
				return Collections.emptyList();
			}
		};
	}

	@SneakyThrows
	private static ExecutionStatus makeState(ManagedExecutionId id, User owner, User callingUser, ExecutionState state, String typeLabel, SecondaryIdDescriptionId secondaryId, Long resultCount, boolean containsDates) {
		OverviewExecutionStatus status = new OverviewExecutionStatus();

		final ManagedQuery execMock = new ManagedQuery(null, owner.getId(), DATASET_0.getId()) {
			{
				setQueryId(id.getExecution());
				setLastResultCount(resultCount);
			}

			@Override
			public List<ResultInfo> getResultInfos() {
				return Collections.emptyList();
			}
		};

		status.setTags(new String[0]);
		status.setLabel(id.getExecution().toString());
		status.setPristineLabel(true);
		status.setCreatedAt(LocalDateTime.MIN.atZone(ZoneId.systemDefault()));
		status.setOwner(owner.getId());
		status.setOwnerName(owner.getLabel());
		status.setShared(false);
		status.setOwn(owner.equals(callingUser));
		status.setId(id);
		status.setStatus(state);
		status.setQueryType(typeLabel);
		status.setNumberOfResults(resultCount);
		status.setSecondaryId(secondaryId); // This is probably not interesting on the overview (only if there is an filter for the search)
		status.setContainsDates(containsDates);
		if(state.equals(DONE)) {
			List<ResultAsset> resultUrls = new ArrayList<>();
			resultUrls.addAll(EXCEL_RESULT_PROVIDER.generateResultURLs(execMock, URI_BUILDER.clone(), true));
			resultUrls.addAll(CSV_RESULT_PROVIDER.generateResultURLs(execMock, URI_BUILDER.clone(), true));
			resultUrls.addAll(ARROW_RESULT_PROVIDER.generateResultURLs(execMock, URI_BUILDER.clone(), true));
			resultUrls.addAll(PARQUET_RESULT_PROVIDER.generateResultURLs(execMock, URI_BUILDER.clone(), true));
			status.setResultUrls(resultUrls);
		}

		return status;
	}

}
