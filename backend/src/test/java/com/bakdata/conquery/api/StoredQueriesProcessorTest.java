package com.bakdata.conquery.api;

import static com.bakdata.conquery.models.execution.ExecutionState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.StoredQueriesProcessor;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.OverviewExecutionStatus;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.SecondaryIdQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultCSVResource;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.UriBuilder;

public class StoredQueriesProcessorTest {
	private static final MetaStorage STRORAGE = mock(MetaStorage.class);
	private static final StoredQueriesProcessor processor = new StoredQueriesProcessor(mock(DatasetRegistry.class), STRORAGE, new ConqueryConfig());

	private static final Dataset DATASET_0 = new Dataset() {{setName("dataset0");}};
	private static final Dataset DATASET_1 = new Dataset() {{setName("dataset1");}};

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
	public static final UriBuilder URI_BUILDER = UriBuilder.fromPath("http://localhost");

	private static ManagedExecutionId createExecutionId(Dataset dataset0, String s) {
		StringBuilder idBuilder = new StringBuilder("00000000-0000-0000-0000-000000000000");
		idBuilder.replace(idBuilder.length() - s.length(),idBuilder.length(), s);

		return new ManagedExecutionId(dataset0.getId(), UUID.fromString(idBuilder.toString()));
	}

	private static int USER_COUNT = 0;

	private static final User[] USERS = new User[] {
		mockUser(List.of(QUERY_ID_0, QUERY_ID_1,QUERY_ID_2, QUERY_ID_4, QUERY_ID_7, QUERY_ID_9)),
		mockUser(List.of(QUERY_ID_3, QUERY_ID_4))
	};



	private static final List<ManagedExecution<?>> queries = ImmutableList.of(
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_0, NEW),            // included
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_1, NEW),            // not included: wrong dataset
			mockManagedForm(USERS[0], QUERY_ID_2, NEW),                            // not included: not a ManagedQuery
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_3, NEW),         // not included: missing permission
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_4, DONE),        // included
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_5, FAILED),        // not included: wrong state
			mockManagedQuery(new AbsoluteFormQuery(null, null, null, null), USERS[0], QUERY_ID_6, NEW),                                                    // not included: wrong query structure
			mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_7, DONE, new CQAnd(){{setChildren(List.of(new CQConcept()));}}),    // included, but secondaryId-Query
			mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_8, DONE, new CQConcept()),    // not-included, wrong structure
			mockManagedQuery(new ConceptQuery(new CQExternal(new ArrayList<>(), new String[0][0])), USERS[1], QUERY_ID_9, DONE)        // included

		);

	@BeforeAll
	public static void beforeAll() {
		// setup storage mock
		doAnswer((invocation) -> {
			UserId id = (UserId) invocation.getArgument(0);
			for (User user : USERS) {
				if(user.getId().equals(id)) {
					return user;
				}
			}
			return null;
		}).when(STRORAGE).getUser(any());
	}

	@Test
	public void getQueriesFiltered() {

		List<ExecutionStatus> infos = processor.getQueriesFiltered(DATASET_0.getId(), URI_BUILDER, USERS[0], queries).collect(Collectors.toList());

		assertThat(infos)
				.containsExactly(
						makeState(QUERY_ID_0, USERS[0], USERS[0], NEW, "CONCEPT_QUERY", null),
						makeState(QUERY_ID_4, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null),
						makeState(QUERY_ID_7, USERS[1], USERS[0], DONE, "SECONDARY_ID_QUERY", new SecondaryIdDescriptionId(DATASET_0.getId(),"sid")),
						makeState(QUERY_ID_9, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null)

						);
	}

	private static User mockUser(List<ManagedExecutionId> allowedQueryIds) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(new UserId("user" + USER_COUNT++));

		doAnswer((invocation) -> {
			ConqueryPermission perm = (ConqueryPermission) invocation.getArgument(0);
			return allowedQueryIds.contains(ManagedExecutionId.Parser.INSTANCE.parse(perm.getInstances().iterator().next()));
		}).when(user).isPermitted(any(ConqueryPermission.class));
		return user;

	}

	private static ManagedForm mockManagedForm(User user, ManagedExecutionId id, ExecutionState execState){
		return new ManagedForm(new ExportForm(), user.getId(), id.getDataset()) {
			{
				state = execState;
				creationTime = LocalDateTime.MIN;
				queryId =id.getExecution();
			}
		};
	}

	private static ManagedQuery mockManagedConceptQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState){
		return mockManagedQuery(
				new ConceptQuery(
						new CQAnd()
						{{
							// short hand class initializer block to support visiting of CQAnd Children
							setChildren(List.of(new CQConcept()));
						}}
						),
				user,
				id,
				execState);
	}
	private static ManagedQuery mockManagedSecondaryIdQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, CQElement root){
		final SecondaryIdQuery sid = new SecondaryIdQuery();
		sid.setSecondaryId(new SecondaryIdDescription() {{
			setDataset(DATASET_0);
			setName("sid");
		}});
		sid.setRoot(root);

		return mockManagedQuery(sid, user, id, execState);
	}


	private static ManagedQuery mockManagedQuery(IQuery queryDescription, User user, ManagedExecutionId id, ExecutionState execState){
		return new ManagedQuery(queryDescription, user.getId(), id.getDataset()) {
			{
				state = execState;
				creationTime = LocalDateTime.MIN;
				queryId = id.getExecution();
			}
		};
	}

	@SneakyThrows
	private static ExecutionStatus makeState(ManagedExecutionId id, User owner, User callingUser, ExecutionState state, String typeLabel, SecondaryIdDescriptionId secondaryId) {
		OverviewExecutionStatus status = new OverviewExecutionStatus();

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
		status.setSecondaryId(secondaryId); // This is probably not interesting on the overview (only if there is an filter for the search)
		if(state.equals(DONE)) {
			status.setResultUrl(URI_BUILDER.clone()
					.path(ResultCSVResource.class)
					.resolveTemplate(ResourceConstants.DATASET, id.getDataset())
					.path(ResultCSVResource.class, ResultCSVResource.GET_CSV_PATH_METHOD)
					.resolveTemplate(ResourceConstants.QUERY, id.toString())
					.build()
					.toURL());
		}

		return status;
	}

}
