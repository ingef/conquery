package com.bakdata.conquery.models.auth.web.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class JwtWrapper {

	private String access_token;

}
