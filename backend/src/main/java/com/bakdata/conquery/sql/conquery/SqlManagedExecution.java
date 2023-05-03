package com.bakdata.conquery.sql.conquery;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;

public class SqlManagedExecution extends ManagedExecution {

	protected SqlManagedExecution(MetaStorage storage) {
		super(storage);
	}

	public SqlManagedExecution(User owner, Dataset dataset, MetaStorage storage) {
		super(owner, dataset, storage);
	}

	@Override
	protected void doInitExecutable() {

	}
	@Override
	public QueryDescription getSubmitted() {
		return null;
	}

	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		return null;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {

	}
}
