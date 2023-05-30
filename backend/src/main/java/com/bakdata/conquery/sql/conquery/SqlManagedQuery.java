package com.bakdata.conquery.sql.conquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.sql.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.util.QueryUtils;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@CPSType(base = ManagedExecution.class, id = "SQL_QUERY")
public class SqlManagedQuery extends ManagedExecution implements SingleTableResult {
	private Query query;
	private SqlQuery sqlQuery;
	@Setter
	private SqlExecutionResult result;

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
		Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");
		List<ColumnDescriptor> columnDescriptions = new ArrayList<>();

		final Locale locale = I18n.LOCALE.get();

		PrintSettings settings = new PrintSettings(true, locale, getNamespace(), getConfig(), null);

		UniqueNamer uniqNamer = new UniqueNamer(settings);

		// First add the id columns to the descriptor list. The are the first columns
		for (ResultInfo header : getConfig().getIdColumns().getIdResultInfos()) {
			columnDescriptions.add(ColumnDescriptor.builder()
												   .label(uniqNamer.getUniqueName(header))
												   .type(header.getType().typeInfo())
												   .semantics(header.getSemantics())
												   .build());
		}

		final UniqueNamer collector = new UniqueNamer(settings);
		getResultInfos().forEach(info -> columnDescriptions.add(info.asColumnDescriptor(settings, collector)));
		return columnDescriptions;
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return query.getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		return result.getTable().stream().map(Function.identity());
	}

	@Override
	public long resultRowCount() {
		return result.getRowCount();
	}
}
