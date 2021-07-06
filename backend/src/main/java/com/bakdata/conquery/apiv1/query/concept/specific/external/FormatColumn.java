package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@Data
public abstract class FormatColumn {

	@InternalOnly @Setter @Getter
	private int position;

}
