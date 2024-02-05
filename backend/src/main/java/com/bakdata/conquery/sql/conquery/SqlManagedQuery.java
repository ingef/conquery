package com.bakdata.conquery.sql.conquery;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.query.EditorQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@CPSType(base = ManagedExecution.class, id = "SQL_QUERY")
public class SqlManagedQuery extends ManagedExecution implements EditorQuery, SingleTableResult {

	private Query query;
	private SqlQuery sqlQuery;
	private SqlExecutionResult result;
	private Long lastResultCount;

	protected SqlManagedQuery(MetaStorage storage) {
		super(storage);
	}

	public SqlManagedQuery(Query query, User owner, Dataset dataset, MetaStorage storage, SqlQuery sqlQuery) {
		super(owner, dataset, storage);
		this.query = query;
		this.sqlQuery = sqlQuery;
	}

	@Override
	protected void doInitExecutable() {
		query.resolve(new QueryResolveContext(getNamespace(), getConfig(), getStorage(), null));
	}

	@Override
	public QueryDescription getSubmitted() {
		return query;
	}

	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		return QueryUtils.makeQueryLabel(query, cfg, getId());
	}

	@Override
	public void cancel() {
		//TODO when async is implemented.
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return query.getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		return result.getTable().stream();
	}

	@Override
	public long resultRowCount() {
		return result.getRowCount();
	}

	@Override
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {
		super.setStatusBase(subject, status);
		enrichStatusBase(status);
	}

	public void finish(final SqlExecutionResult result) {
		this.result = result;
		this.lastResultCount = (long) result.getRowCount();
		super.finish(ExecutionState.DONE);
	}
}
