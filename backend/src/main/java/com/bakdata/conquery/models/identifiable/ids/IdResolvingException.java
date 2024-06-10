package com.bakdata.conquery.models.identifiable.ids;

/**
 * TODO as {@link com.bakdata.conquery.models.error.ConqueryError} ?
 */
public class IdResolvingException extends IllegalArgumentException {

    public static final String MESSAGE = "Unable to resolve id %s to an instance";

    public IdResolvingException(Id<?> id) {
        super(MESSAGE.formatted(id));
    }

    public IdResolvingException(Id<?> id, Throwable cause) {
        super(MESSAGE.formatted(id), cause);
    }
}
