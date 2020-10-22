package com.bakdata.conquery.api;

import static com.bakdata.conquery.models.execution.ExecutionState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.StoredQueriesProcessor;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StoredQueriesProcessorTest {
	private static final MetaStorage STRORAGE = mock(MetaStorage.class);
	private static final StoredQueriesProcessor processor = new StoredQueriesProcessor(mock(DatasetRegistry.class), STRORAGE);
	
	private static final Dataset DATASET_0 = new Dataset() {{setName("dataset0");}};
	private static final Dataset DATASET_1 = new Dataset() {{setName("dataset1");}};
	private static final ManagedExecutionId QUERY_ID_0 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000000"));
	private static final ManagedExecutionId QUERY_ID_1 = new ManagedExecutionId(DATASET_1.getId(), UUID.fromString("00000000-0000-0000-0000-000000000001"));
	private static final ManagedExecutionId QUERY_ID_2 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000002"));
	private static final ManagedExecutionId QUERY_ID_3 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000003"));
	private static final ManagedExecutionId QUERY_ID_4 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000004"));
	private static final ManagedExecutionId QUERY_ID_5 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000005"));
	private static final ManagedExecutionId QUERY_ID_6 = new ManagedExecutionId(DATASET_0.getId(), UUID.fromString("00000000-0000-0000-0000-000000000006"));
	
	private static int USER_COUNT = 0;
	private static final User[] USERS = new User[] {
		mockUser(List.of(QUERY_ID_0, QUERY_ID_1,QUERY_ID_2, QUERY_ID_4)),
		mockUser(List.of(QUERY_ID_3, QUERY_ID_4))
	};
	

	
	private static final List<ManagedExecution<?>> queries = ImmutableList.of(
		mockManagedQueryFrontEnd(USERS[0], QUERY_ID_0, NEW),		// included
		mockManagedQueryFrontEnd(USERS[0], QUERY_ID_1, NEW),		// not included: wrong dataset
		mockManagedForm(USERS[0], QUERY_ID_2, NEW),				// not included: not a ManagedQuery
		mockManagedQueryFrontEnd(USERS[1], QUERY_ID_3, NEW),		// not included: missing permission
		mockManagedQueryFrontEnd(USERS[1], QUERY_ID_4, DONE),	// included
		mockManagedQueryFrontEnd(USERS[0], QUERY_ID_5, FAILED), 	// not included: wrong state
		mockManagedQuery(new AbsoluteFormQuery(null, null, null, null), USERS[0], QUERY_ID_6, NEW) 	// not included: wrong query structure
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
		
		Stream<ExecutionStatus> infos = processor.getQueriesFiltered(DATASET_0.getId(), null, USERS[0], queries);
		
		assertThat(infos).containsExactly(
			makeState(QUERY_ID_0, USERS[0], USERS[0], NEW),
			makeState(QUERY_ID_4, USERS[1], USERS[0], DONE)
			//new ExecutionStatus(new String[0], QUERY_ID_0.getExecution().toString(), true, LocalDateTime.MIN.atZone(ZoneId.systemDefault()), null, USERS[0].getId(), null, false, true, false, QUERY_ID_0, ExecutionState.NEW, null, null, null, null, false, null, null)
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
	
	private static ManagedQuery mockManagedQueryFrontEnd(User user, ManagedExecutionId id, ExecutionState execState){
		return mockManagedQuery(new ConceptQuery(null), user, id, execState);
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
	
	private static ExecutionStatus makeState(ManagedExecutionId id, User owner, User callingUser, ExecutionState state) {
		return new ExecutionStatus(new String[0], id.getExecution().toString(), true, LocalDateTime.MIN.atZone(ZoneId.systemDefault()), null, owner.getId(), null, false, owner.equals(callingUser), false, id, state, null, null, null, null, false, null, null);
	}

}
