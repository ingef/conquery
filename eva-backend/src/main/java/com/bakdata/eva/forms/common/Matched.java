package com.bakdata.eva.forms.common;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.eva.forms.common.ColumnDescriptor.MatchingType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@CPSType(id="MATCHED", base=CQElement.class)
public class Matched extends CQOr {

	@NotNull
	private MatchingType matchingType = MatchingType.NONE;
	
}
