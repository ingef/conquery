package com.bakdata.conquery.util;

import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UtilityClass
public class Mocks {



    public static ExecutionManager<?> mockExecutionManager(List<EntityResult> results) {
        ExecutionManager<?> executionManager = mock(ExecutionManager.class);

        when(executionManager.streamQueryResults(any())).thenReturn(results.stream());
        return executionManager;
    }
}
