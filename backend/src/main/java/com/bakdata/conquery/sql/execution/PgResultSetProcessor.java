package com.bakdata.conquery.sql.execution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.config.ConqueryConfig;
import org.postgresql.jdbc.PgArray;


public class PgResultSetProcessor extends DefaultResultSetProcessor {


	public PgResultSetProcessor(ConqueryConfig config, SqlCDateSetParser dateSetParser) {
		super(config, dateSetParser);
	}
}
