package com.bakdata.conquery.apiv1.auth;

import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Container class that holds user ({@link UserId} as a {@link String}) and
 * password for request of a JWT. This is used by the Resources that are
 * registered by the {@link LocalAuthenticationRealm}.
 */
@Data
public class UsernamePasswordToken {

	@NotEmpty
	private String user;
	@NotEmpty
	private char[] password;
}
