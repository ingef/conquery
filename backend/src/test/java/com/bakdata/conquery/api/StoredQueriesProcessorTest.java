package com.bakdata.conquery.api;

import static com.bakdata.conquery.models.execution.ExecutionState.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

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
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.config.ArrowResultProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.CsvResultProvider;
import com.bakdata.conquery.models.config.ExcelResultProvider;
import com.bakdata.conquery.models.config.ParquetResultProvider;
import com.bakdata.conquery.models.config.auth.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class StoredQueriesProcessorTest {
	private static final MetaStorage STORAGE = new NonPersistentStoreFactory().createMetaStorage();
	// Marked Unused, but does inject itself.
	public static final AuthorizationController AUTHORIZATION_CONTROLLER = new AuthorizationController(STORAGE, new DevelopmentAuthorizationConfig());

	public static final ConqueryConfig CONFIG = new ConqueryConfig();
	private static final DatasetRegistry<DistributedNamespace> datasetRegistry = new DatasetRegistry<>(0, CONFIG, null, null);
	private static final QueryProcessor processor = new QueryProcessor(datasetRegistry, STORAGE, CONFIG);

	private static final Dataset DATASET_0 = new Dataset() {{
		setName("dataset0");
	}};
	private static final Dataset DATASET_1 = new Dataset() {{
		setName("dataset1");
	}};

	private static final ManagedExecutionId QUERY_ID_0 = createExecutionId(DATASET_0, "0");
	private static final ManagedExecutionId QUERY_ID_1 = createExecutionId(DATASET_1, "1");
	private static final ManagedExecutionId QUERY_ID_2 = createExecutionId(DATASET_0, "2");
	private static final ManagedExecutionId QUERY_ID_3 = createExecutionId(DATASET_0, "3");
	private static final ManagedExecutionId QUERY_ID_4 = createExecutionId(DATASET_0, "4");
	private static final ManagedExecutionId QUERY_ID_5 = createExecutionId(DATASET_0, "5");
	private static final ManagedExecutionId QUERY_ID_6 = createExecutionId(DATASET_0, "6");
	private static final ManagedExecutionId QUERY_ID_7 = createExecutionId(DATASET_0, "7");
	private static final ManagedExecutionId QUERY_ID_8 = createExecutionId(DATASET_0, "8");
	private static final ManagedExecutionId QUERY_ID_9 = createExecutionId(DATASET_0, "9");
	private static final ManagedExecutionId QUERY_ID_10 = createExecutionId(DATASET_0, "10");
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
			mockUser(0, List.of(QUERY_ID_0, QUERY_ID_1, QUERY_ID_2, QUERY_ID_4, QUERY_ID_7, QUERY_ID_9, QUERY_ID_10)),
			mockUser(1, List.of(QUERY_ID_3, QUERY_ID_4))
	};

	private static final List<ManagedExecution> queries = ImmutableList.of(
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_0, NEW, DATASET_0, 100L),            // included
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_1, NEW, DATASET_1, 100L),            // not included: wrong dataset
			mockManagedForm(USERS[0], QUERY_ID_2, NEW, DATASET_0),                            // not included: not a ManagedQuery
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_3, NEW, DATASET_0, 100L),         // not included: missing permission
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_4, DONE, DATASET_0, 100L),        // included
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_5, FAILED, DATASET_0, 100L),        // not included: wrong state
			mockManagedQuery(new AbsoluteFormQuery(null, null, null, null), USERS[0], QUERY_ID_6, NEW, DATASET_0, 100L),                                                    // not included: wrong query structure
			mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_7, DONE, new CQAnd() {{
				setChildren(List.of(new CQConcept()));
			}}, DATASET_0),    // included, but secondaryId-Query
			mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_8, DONE, new CQConcept(), DATASET_0),    // not-included, wrong structure
			mockManagedQuery(new ConceptQuery(new CQExternal(new ArrayList<>(), new String[0][0], false)), USERS[1], QUERY_ID_9, DONE, DATASET_0, 100L),        // included
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_10, DONE, DATASET_0, 2_000_000L)        // included, but no result url for xlsx (result has too many rows)

	);


	@Test
	public void getQueriesFiltered() {

		List<ExecutionStatus> infos = processor.getQueriesFiltered(DATASET_0, URI_BUILDER, USERS[0], queries, true)
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

	private static User mockUser(int id, List<ManagedExecutionId> allowedQueryIds) {
		final User user = new User("user" + id, null, STORAGE);

		STORAGE.addUser(user);

		for (ManagedExecutionId queryId : allowedQueryIds) {
			user.addPermission(ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR,queryId));
		}

		return user;

	}

	private static ManagedForm mockManagedForm(User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset){
		return new ManagedInternalForm(new ExportForm(), user, dataset, STORAGE) {
			{
				setState(execState);
				setCreationTime(LocalDateTime.MIN);
				setQueryId(id.getExecution());
			}
		};
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
	private static ManagedQuery mockManagedSecondaryIdQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, CQElement root, Dataset dataset){
		final SecondaryIdQuery sid = new SecondaryIdQuery();
		sid.setSecondaryId(new SecondaryIdDescription() {{
			setDataset(dataset);
			setName("sid");
		}});
		sid.setRoot(root);

		return mockManagedQuery(sid, user, id, execState, dataset, 100L);
	}


	private static ManagedQuery mockManagedQuery(Query queryDescription, User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset, final long resultCount) {
		return new ManagedQuery(queryDescription, user, dataset, STORAGE) {
			{
				setState(execState);
				setCreationTime(LocalDateTime.MIN);
				setQueryId(id.getExecution());
				setLastResultCount(resultCount);
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
	private static ExecutionStatus makeState(ManagedExecutionId id, User owner, User callingUser, ExecutionState state, String typeLabel, SecondaryIdDescriptionId secondaryId, Long resultCount) {
		OverviewExecutionStatus status = new OverviewExecutionStatus();

		final ManagedQuery execMock = new ManagedQuery(null, owner, DATASET_0, STORAGE) {
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
		status.setShared(false);
		status.setOwn(owner.equals(callingUser));
		status.setId(id);
		status.setStatus(state);
		status.setQueryType(typeLabel);
		status.setNumberOfResults(resultCount);
		status.setSecondaryId(secondaryId); // This is probably not interesting on the overview (only if there is an filter for the search)
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
