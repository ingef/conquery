package com.bakdata.eva.models.translation.query.oldmodel.concept;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@Getter @Setter
@NoArgsConstructor
public abstract class QElement {

	@JsonBackReference
	private QGroup group;

	private boolean excludeTimestamps = false;

	public abstract CQElement translate(DatasetId dataset);
}
