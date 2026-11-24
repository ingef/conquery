package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.error.ErrorMessages;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@CPSType(base = ConqueryError.class, id = "CQ_ID_RESOLVE_ERROR")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(onConstructor_ =  @JsonCreator)
public class IdResolvingException extends ConqueryError {

	private final String idClass;
	private final String unresolvableId;

	public IdResolvingException(Id<?, ?> id) {
		this(id, null);
    }

	public IdResolvingException(Id<?, ?> id, Throwable cause) {
        super(ConqueryError.asConqueryError(cause));
		this.idClass = id.getClass().toString();
		this.unresolvableId = id.toString();
    }

	@Override
	public String getMessageTemplate(ErrorMessages errorMessages) {
		return errorMessages.idUnresolvable(unresolvableId, idClass);
	}
}
