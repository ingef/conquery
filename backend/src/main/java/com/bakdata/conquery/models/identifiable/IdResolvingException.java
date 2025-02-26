package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.error.ErrorMessages;
import com.bakdata.conquery.models.identifiable.ids.Id;

@CPSType(base = ConqueryError.class, id = "CQ_ID_RESOLVE_ERROR")
public class IdResolvingException extends ConqueryError {

	private final Id<?> id;

    public IdResolvingException(Id<?> id) {
		this.id = id;
    }

    public IdResolvingException(Id<?> id, Throwable cause) {
        super(ConqueryError.asConqueryError(cause));
		this.id = id;
    }

	@Override
	public String getMessageTemplate(ErrorMessages errorMessages) {
		return errorMessages.idUnresolvable(id);
	}
}
