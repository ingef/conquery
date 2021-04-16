package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationToken;

@SuppressWarnings("serial")
@Getter
@RequiredArgsConstructor
public class DevelopmentToken implements AuthenticationToken {
	//TODO migrate to user?
	private final UserId principal;
	private final String credentials;

}