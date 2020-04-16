package com.bakdata.conquery.apiv1.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Container type to transport the JWT to the front end in the expected format.
 */
@AllArgsConstructor
@Data
public class JwtWrapper {

	private String access_token;

}
