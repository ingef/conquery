package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

@Data
public class ApiTokenData {
	private final String name;
	private final UserId userId;
	private final LocalDate creationDate;
	private final LocalDate expirationDate;
	private final Set<Scopes> scopes;


	@JacksonInject(useInput = OptBoolean.FALSE)
	@NonNull
	@EqualsAndHashCode.Exclude
	private MetaStorage storage;

	/**
	 * Dynamic information about the token
	 */
	@Data
	public static class MetaData{
		private LocalDate lastUsed;
	}
}
