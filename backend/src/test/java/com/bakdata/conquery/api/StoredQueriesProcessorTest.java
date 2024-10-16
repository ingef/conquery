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
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.config.ArrowResultProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.CsvResultProvider;
import com.bakdata.conquery.models.config.ExcelResultProvider;
import com.bakdata.conquery.models.config.ParquetResultProvider;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.extensions.MetaStorageExtension;
import com.bakdata.conquery.util.extensions.UserExtension;
import com.google.common.collect.ImmutableList;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.validation.Validators;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StoredQueriesProcessorTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig();
	public static final UriBuilder URI_BUILDER = UriBuilder.fromPath("http://localhost");
	public static final IndexService INDEX_SERVICE = new IndexService(CONFIG.getCsv().createCsvParserSettings(), "empty");
	private static final Environment ENVIRONMENT = new Environment("StoredQueriesProcessorTest");
	@RegisterExtension
	private static final MetaStorageExtension STORAGE_EXTENTION = new MetaStorageExtension(ENVIRONMENT.metrics());
	public static final MetaStorage STORAGE = STORAGE_EXTENTION.getMetaStorage();
	@RegisterExtension
	private static final UserExtension USER_0_EXTENSIONS = new UserExtension(STORAGE, "0");
	@RegisterExtension
	private static final UserExtension USER_1_EXTENSIONS = new UserExtension(STORAGE, "1");
	private static final User[] USERS = new User[]{
			USER_0_EXTENSIONS.getUser(),
			USER_1_EXTENSIONS.getUser()
	};

	private static final Validator VALIDATOR = Validators.newValidator();
	public static final InternalMapperFactory INTERNAL_MAPPER_FACTORY = new InternalMapperFactory(CONFIG, VALIDATOR);
	private static final DatasetRegistry<DistributedNamespace>
			DATASET_REGISTRY =
			new DatasetRegistry<>(
					0,
					CONFIG,
					INTERNAL_MAPPER_FACTORY,
					new ClusterNamespaceHandler(new ClusterState(), CONFIG, INTERNAL_MAPPER_FACTORY),
					INDEX_SERVICE
			);
	private static final QueryProcessor processor = new QueryProcessor(DATASET_REGISTRY, STORAGE, CONFIG, VALIDATOR);
	private static final ExcelResultProvider EXCEL_RESULT_PROVIDER = new ExcelResultProvider();
	private static final CsvResultProvider CSV_RESULT_PROVIDER = new CsvResultProvider();
	private static final ArrowResultProvider ARROW_RESULT_PROVIDER = new ArrowResultProvider();
	private static final ParquetResultProvider PARQUET_RESULT_PROVIDER = new ParquetResultProvider();
	private static final Dataset DATASET_0 = new Dataset() {{
		setName("dataset0");
	}};
	private static final Dataset DATASET_1 = new Dataset() {{
		setName("dataset1");
	}};
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
	private static List<ManagedExecution> QUERIES;

	@BeforeAll
	public static void beforeAll() throws IOException {
		new AuthorizationController(STORAGE, CONFIG, new Environment(StoredQueriesProcessorTest.class.getSimpleName()), null);

		DATASET_REGISTRY.createNamespace(DATASET_0, STORAGE, ENVIRONMENT);
		DATASET_REGISTRY.createNamespace(DATASET_1, STORAGE, ENVIRONMENT);


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
		for (ManagedExecutionId id : List.of(QUERY_ID_4, QUERY_ID_7, QUERY_ID_9, QUERY_ID_10)) {

			user0.addPermission(ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR, id));
		}


		QUERIES = ImmutableList.of(
				mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_0, NEW, DATASET_0, 100L),
				// included
				mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_1, NEW, DATASET_1, 100L),
				// not included: wrong dataset
				mockManagedForm(USERS[0], QUERY_ID_2, NEW, DATASET_0),
				// not included: not a ManagedQuery
				mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_3, NEW, DATASET_0, 100L),
				// not included: missing permission
				mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_4, DONE, DATASET_0, 100L),
				// included
				mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_5, FAILED, DATASET_0, 100L),
				// not included: wrong state
				mockManagedQuery(new AbsoluteFormQuery(null, null, null, null), USERS[0], QUERY_ID_6, NEW, DATASET_0, 100L),
				// not included: wrong query structure
				mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_7, DONE, new CQAnd() {{
					setChildren(List.of(new CQConcept()));
				}}, DATASET_0),
				// included, but secondaryId-Query
				mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_8, DONE, new CQConcept(), DATASET_0),
				// not-included, wrong structure
				mockManagedQuery(new ConceptQuery(new CQExternal(new ArrayList<>(), new String[0][0], false)), USERS[1], QUERY_ID_9, DONE, DATASET_0, 100L),
				// included
				mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_10, DONE, DATASET_0, 2_000_000L)
				// included, but no result url for xlsx (result has too many rows)

		);
	}

	private static void setState(ExecutionState execState, ManagedExecutionId id) {
		if (execState != NEW) {
			DistributedExecutionManager.DistributedState state = new DistributedExecutionManager.DistributedState();
			state.setState(execState);
			state.getExecutingLock().countDown();

			DATASET_REGISTRY.get(id.getDataset()).getExecutionManager().addState(id, state);
		}
	}

	private static ManagedExecutionId createExecutionId(Dataset dataset0, String s) {
		StringBuilder idBuilder = new StringBuilder("00000000-0000-0000-0000-000000000000");
		idBuilder.replace(idBuilder.length() - s.length(), idBuilder.length(), s);

		return new ManagedExecutionId(dataset0.getId(), UUID.fromString(idBuilder.toString()));
	}

	private static ManagedQuery mockManagedConceptQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, Dataset dataset, long resultCount) {
		return mockManagedQuery(
				new ConceptQuery(
						new CQAnd() {{
							// short hand class initializer block to support visiting of CQAnd Children
							setChildren(List.of(new CQConcept()));
						}}
				),
				user,
				id,
				execState, dataset, resultCount
		);
	}

	private static ManagedForm<?> mockManagedForm(User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset) {
		return new ManagedInternalForm<>(new ExportForm(), user.getId(), dataset.getId(), STORAGE, DATASET_REGISTRY) {
			{
				setState(execState, id);
				setCreationTime(LocalDateTime.MIN);
				setQueryId(id.getExecution());
			}
		};
	}

	private static ManagedQuery mockManagedQuery(
			Query queryDescription,
			User user,
			ManagedExecutionId id,
			ExecutionState execState,
			final Dataset dataset,
			final long resultCount) {
		ManagedQuery managedQuery = new ManagedQuery(queryDescription, user.getId(), dataset.getId(), STORAGE, DATASET_REGISTRY) {
			{
				setCreationTime(LocalDateTime.MIN);
				setQueryId(id.getExecution());
				setLastResultCount(resultCount);
				setConfig(CONFIG);
			}

			@Override
			public List<ResultInfo> getResultInfos() {
				// With method is mocked because the ExcelResultProvider needs some info to check dimensions,
				// but actually resolving the query here requires much more setup
				return Collections.emptyList();
			}
		};
		setState(execState, managedQuery.getId());
		return managedQuery;
	}

	private static ManagedQuery mockManagedSecondaryIdQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, CQElement root, Dataset dataset) {
		final SecondaryIdQuery sIdQ = new SecondaryIdQuery();
		SecondaryIdDescription sId = new SecondaryIdDescription() {{
			setDataset(dataset.getId());
			setName("sid");
		}};
		sIdQ.setSecondaryId(sId.getId());
		sIdQ.setRoot(root);

		return mockManagedQuery(sIdQ, user, id, execState, dataset, 100L);
	}

	@Test
	public void getQueriesFiltered() {

		List<ExecutionStatus> infos = processor.getQueriesFiltered(DATASET_0.getId(), URI_BUILDER, USERS[0], QUERIES.stream(), true)
											   .collect(Collectors.toList());

		assertThat(infos)
				.containsExactly(
						makeState(QUERY_ID_0, USERS[0], USERS[0], NEW, "CONCEPT_QUERY", null, 100L),
						makeState(QUERY_ID_4, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null, 100L),
						makeState(QUERY_ID_7, USERS[1], USERS[0], DONE, "SECONDARY_ID_QUERY", new SecondaryIdDescriptionId(DATASET_0.getId(), "sid"), 100L),
						makeState(QUERY_ID_9, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null, 100L),
						makeState(QUERY_ID_10, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null, 2_000_000L)

				);
	}

	@SneakyThrows
	private static ExecutionStatus makeState(
			ManagedExecutionId id,
			User owner,
			User callingUser,
			ExecutionState state,
			String typeLabel,
			SecondaryIdDescriptionId secondaryId,
			Long resultCount) {
		OverviewExecutionStatus status = new OverviewExecutionStatus();

		final ManagedQuery execMock = new ManagedQuery(null, owner.getId(), DATASET_0.getId(), STORAGE, DATASET_REGISTRY) {
			{
				setQueryId(id.getExecution());
				setLastResultCount(resultCount);
				setConfig(CONFIG);
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
		status.setSecondaryId(secondaryId); // This is probably not interesting on the overview (only if there is a filter for the search)
		if (state.equals(DONE)) {
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
