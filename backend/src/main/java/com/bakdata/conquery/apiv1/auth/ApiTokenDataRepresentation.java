package com.bakdata.conquery.apiv1.auth;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenData;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenRealm;
import com.bakdata.conquery.models.auth.apitoken.Scopes;
import com.bakdata.conquery.models.auth.entities.User;
import io.dropwizard.validation.ValidationMethod;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * @implNote We don't use fluent accessors here, because that does not work well with Jackson
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ApiTokenDataRepresentation {

	@NotNull
	protected String name;
	@NotNull
	protected LocalDate expirationDate;
	@NotEmpty
	protected Set<Scopes> scopes;

	@ValidationMethod
	boolean isNotExpired() {
		final LocalDate now = LocalDate.now();
		return expirationDate.isAfter(now) || expirationDate.isEqual(now);
	}

	/**
	 * Container that is send with an incoming request.
	 */
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Request extends ApiTokenDataRepresentation {

		public ApiTokenData toInternalRepresentation(User user, ApiTokenRealm.ApiTokenHash hash, MetaStorage storage) {
			return new ApiTokenData(
					UUID.randomUUID(),
					hash,
					name,
					user.getId(),
					LocalDate.now(),
					expirationDate,
					scopes,
					storage
			);
		}
	}

	/**
	 * Container that is send with an outgoing response.
	 */
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Response extends ApiTokenDataRepresentation {

		private UUID id;
		private LocalDate lastUsed;
		private LocalDate creationDate;

	}
}
