package com.bakdata.conquery.apiv1.forms.export_form;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "value")
@CPSBase
@EqualsAndHashCode
public abstract class Mode implements Visitable {

	@Getter
	@Setter
	@JsonBackReference
	@EqualsAndHashCode.Exclude
	private ExportForm form;

	public abstract void resolve(QueryResolveContext context);

	public abstract Query createSpecializedQuery();
}
