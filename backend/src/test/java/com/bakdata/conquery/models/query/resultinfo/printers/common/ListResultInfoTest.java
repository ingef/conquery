package com.bakdata.conquery.models.query.resultinfo.printers.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.FixedLabelResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import org.junit.jupiter.api.Test;

class ListResultInfoTest {

	@Test
	void readsValuesAsOrderedList() throws Exception {
		FixedLabelResultInfo elementInfo = new FixedLabelResultInfo(ResultType.Primitive.INTEGER, Set.of()) {
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return "values";
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getInteger;
			}
		};
		ListResultInfo listInfo = new ListResultInfo(elementInfo);
		ResultSetProcessor resultSetProcessor = mock(ResultSetProcessor.class);
		ResultSet resultSet = mock(ResultSet.class);
		List<Integer> expected = List.of(4, 3);
		when(resultSetProcessor.getOrderedList(resultSet, 2, ResultType.Primitive.INTEGER)).thenReturn(expected);

		Object actual = listInfo.createReader(resultSetProcessor).read(resultSet, 2);

		assertThat(actual).isEqualTo(expected);
		verify(resultSetProcessor).getOrderedList(resultSet, 2, ResultType.Primitive.INTEGER);
	}
}
