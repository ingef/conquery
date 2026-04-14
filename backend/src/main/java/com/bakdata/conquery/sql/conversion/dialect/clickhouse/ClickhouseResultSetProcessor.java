package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.sql.execution.DefaultResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import com.clickhouse.data.Tuple;

public class ClickhouseResultSetProcessor extends DefaultResultSetProcessor {


	public ClickhouseResultSetProcessor(ConqueryConfig config, SqlCDateSetParser sqlCDateSetParser) {
		super(config, sqlCDateSetParser);
	}

	@Override
	public List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException {
		Array array = resultSet.getArray(columnIndex);

		if (array == null) {
			return Collections.emptyList();
		}

		return ((ClickhouseCDateSetParser) this.sqlCDateSetParser).toEpochDayRangeList((Object[]) array.getArray());
	}
}
