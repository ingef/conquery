package com.bakdata.conquery.sql.conquery;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.util.QueryUtils;

public class SqlManagedQuery extends ManagedExecution implements SingleTableResult {
	private final Query query;

	protected SqlManagedQuery(MetaStorage storage) {
		super(storage);
		throw new UnsupportedOperationException("SqlManagedQuery should not be deserialized");
	}

	public SqlManagedQuery(Query query, User owner, Dataset dataset, MetaStorage storage) {
		super(owner, dataset, storage);
		this.query = query;
	}

	@Override
	protected void doInitExecutable() {

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
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions() {
		return null;
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return null;
	}

	@Override
	public Stream<EntityResult> streamResults() {
		return null;
	}

	@Override
	public long resultRowCount() {
		return 0;
	}
}
