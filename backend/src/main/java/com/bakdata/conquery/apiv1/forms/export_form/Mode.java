package com.bakdata.conquery.apiv1.forms.export_form;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "value")
@CPSBase
public abstract class Mode implements Visitable {

	@Getter
	@Setter
	@JsonBackReference
	private ExportForm form;

	public abstract void resolve(QueryResolveContext context);
	
	public abstract Query createSpecializedQuery(DatasetRegistry datasets, User user, Dataset submittedDataset);
}
