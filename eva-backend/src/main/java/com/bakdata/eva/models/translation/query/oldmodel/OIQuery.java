package com.bakdata.eva.models.translation.query.oldmodel;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;

@CPSBase
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@Getter
@Setter
public abstract class OIQuery {
	private List<String> usedQueries;

	public abstract com.bakdata.conquery.models.query.IQuery translate(DatasetId dataset);
}

