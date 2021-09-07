package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Data;

import java.time.LocalDate;
import java.util.Collection;

@Data
public class ApiTokenData {
	private final String name;
	private final UserId userId;
	private final Collection<ConqueryPermission> permissions;
	private final LocalDate creationDate;
	private final LocalDate expirationDate;

	/**
	 * Dynamic information about the token
	 */
	@Data
	public static class MetaData{
		private LocalDate lastUsed;
	}
}
