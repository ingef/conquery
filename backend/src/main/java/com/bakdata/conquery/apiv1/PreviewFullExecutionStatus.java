package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants
public class PreviewFullExecutionStatus extends FullExecutionStatus {
	private ManagedExecutionId infocard; //TODO Uri would be even better

}
