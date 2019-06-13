package com.bakdata.eva.models.auth;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PrincipalIndex {
	EMAIL(0),
	MANDATOR(1),
	NAME(2);
	
	public final int IDX;
}
