package com.bakdata.conquery.apiv1.auth;

import lombok.Data;

@Data
public class UsernamePasswordToken {
	private String user;
	private char[] password;
}
