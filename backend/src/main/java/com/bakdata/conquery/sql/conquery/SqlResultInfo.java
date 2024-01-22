package com.bakdata.conquery.sql.conquery;

import java.util.Set;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;

/**
 * Temporary result info that sets all {@link ResultType} to {@link com.bakdata.conquery.models.types.ResultType.StringT}.
 */
public class SqlResultInfo extends ResultInfo {
	private final ResultInfo delegate;

	public SqlResultInfo(ResultInfo delegate) {
		this.delegate = delegate;
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return delegate.userColumnName(printSettings);
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return delegate.defaultColumnName(printSettings);
	}

	@Override
	public ResultType getType() {
		return ResultType.StringT.getINSTANCE();
	}

	@Override
	public Set<SemanticType> getSemantics() {
		return delegate.getSemantics();
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}
}
