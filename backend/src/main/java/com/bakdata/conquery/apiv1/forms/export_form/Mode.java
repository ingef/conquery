package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "value")
@CPSBase
public abstract class Mode implements Visitable {

	@Getter
	@Setter
	@JsonBackReference
	private ExportForm form;

	@JsonIgnore
	public String[] getAdditionalHeader() {
		return ArrayUtils.EMPTY_STRING_ARRAY;
	}
	
	public abstract List<ManagedQuery> createSpecializedQuery(Namespaces namespaces, UserId userId, DatasetId submittedDataset);
}
