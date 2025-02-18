package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.error.ErrorMessages;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO as {@link com.bakdata.conquery.models.error.ConqueryError} ?
 */
@CPSType(base = ConqueryError.class, id = "CQ_ID_RESOLVE_ERROR")
@Data
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
public class IdResolvingException extends ConqueryError {

	private String missingId;

	public IdResolvingException(Id<?> id) {
		this.missingId = id.toString();
	}

	public IdResolvingException(Id<?> id, Throwable cause) {
		super(ConqueryError.asConqueryError(cause));
		this.missingId = id.toString();
	}

	@Override
	public String getMessageTemplate(ErrorMessages errorMessages) {
		return errorMessages.idUnresolvable(missingId);
	}
}
