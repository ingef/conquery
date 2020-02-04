package com.bakdata.conquery.models.auth.web.api;

import lombok.Data;

@Data
public class UsernamePasswordToken {
	private String user;
	private char[] password;
}
