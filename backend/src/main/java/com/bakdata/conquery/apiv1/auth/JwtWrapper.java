package com.bakdata.conquery.apiv1.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class JwtWrapper {

	private String access_token;

}
