package com.bakdata.conquery.api;

import static com.bakdata.conquery.models.execution.ExecutionState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StoredQueriesProcessorTest {
	private static final MetaStorage STRORAGE = mock(MetaStorage.class);
	private static final StoredQueriesProcessor processor = new StoredQueriesProcessor(mock(DatasetRegistry.class), STRORAGE, new ConqueryConfig());

	private static final Dataset DATASET_0 = new Dataset() {{setName("dataset0");}};
	private static final Dataset DATASET_1 = new Dataset() {{setName("dataset1");}};

	private static final ManagedExecutionId QUERY_ID_0 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000000"));
	private static final ManagedExecutionId QUERY_ID_1 = new ManagedExecutionId(DATASET_1.getId(), UUID.fromString("00000000-0000-0000-0000-000000000001"));
	private static final ManagedExecutionId QUERY_ID_2 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000002"));
	private static final ManagedExecutionId QUERY_ID_3 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000003"));
	private static final ManagedExecutionId QUERY_ID_4 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000004"));
	private static final ManagedExecutionId QUERY_ID_5 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000005"));
	private static final ManagedExecutionId QUERY_ID_6 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000006"));
	private static final ManagedExecutionId QUERY_ID_7 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000007"));
	private static final ManagedExecutionId QUERY_ID_8 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000008"));

	private static int USER_COUNT = 0;
	private static final User[] USERS = new User[] {
		mockUser(List.of(QUERY_ID_0, QUERY_ID_1,QUERY_ID_2, QUERY_ID_4, QUERY_ID_7)),
		mockUser(List.of(QUERY_ID_3, QUERY_ID_4))
	};



	private static final List<ManagedExecution<?>> queries = ImmutableList.of(
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_0, NEW, DATASET_0),            // included
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_1, NEW, DATASET_1),            // not included: wrong dataset
			mockManagedForm(USERS[0], QUERY_ID_2, NEW, DATASET_0),                            // not included: not a ManagedQuery
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_3, NEW, DATASET_0),         // not included: missing permission
			mockManagedConceptQueryFrontEnd(USERS[1], QUERY_ID_4, DONE, DATASET_0),        // included
			mockManagedConceptQueryFrontEnd(USERS[0], QUERY_ID_5, FAILED, DATASET_0),        // not included: wrong state
			mockManagedQuery(new AbsoluteFormQuery(null, null, null, null), USERS[0], QUERY_ID_6, NEW, DATASET_0),                                                    // not included: wrong query structure
			mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_7, DONE, new CQAnd(), DATASET_0),    // included, but secondaryId-Query
			mockManagedSecondaryIdQueryFrontEnd(USERS[1], QUERY_ID_8, DONE, new CQConcept(), DATASET_0)    // not-included, wrong structure

		);

	@BeforeAll
	public static void beforeAll() {
		// setup storage mock
		doAnswer((invocation) -> {
			UserId id = invocation.getArgument(0);
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

		List<ExecutionStatus> infos = processor.getQueriesFiltered(DATASET_0, null, USERS[0], queries).collect(Collectors.toList());

		assertThat(infos)
				.containsExactly(
						makeState(QUERY_ID_0, USERS[0], USERS[0], NEW, "CONCEPT_QUERY", null),
						makeState(QUERY_ID_4, USERS[1], USERS[0], DONE, "CONCEPT_QUERY", null),
						makeState(QUERY_ID_7, USERS[1], USERS[0], DONE, "SECONDARY_ID_QUERY", new SecondaryIdDescriptionId(DATASET_0.getId(),"sid"))
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

	private static ManagedForm mockManagedForm(User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset){
		return new ManagedForm(new ExportForm(), user.getId(), dataset) {
			{
				state = execState;
				creationTime = LocalDateTime.MIN;
				queryId =id.getExecution();
			}
		};
	}

	private static ManagedQuery mockManagedConceptQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, Dataset dataset){
		return mockManagedQuery(new ConceptQuery(new CQAnd()), user, id, execState, dataset);
	}
	private static ManagedQuery mockManagedSecondaryIdQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState, CQElement root, Dataset dataset){
		final SecondaryIdQuery sid = new SecondaryIdQuery();
		sid.setSecondaryId(new SecondaryIdDescription() {{
			setDataset(DATASET_0);
			setName("sid");
		}});
		sid.setRoot(root);

		return mockManagedQuery(sid, user, id, execState, dataset);
	}


	private static ManagedQuery mockManagedQuery(IQuery queryDescription, User user, ManagedExecutionId id, ExecutionState execState, final Dataset dataset){
		return new ManagedQuery(queryDescription, user.getId(), dataset) {
			{
				state = execState;
				creationTime = LocalDateTime.MIN;
				queryId = id.getExecution();
			}
		};
	}

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

		return status;
	}

}
