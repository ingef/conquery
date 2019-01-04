package com.bakdata.conquery.external.auth.ingef;

import javax.validation.constraints.NotNull;

import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthConfig;

import lombok.Getter;

@CPSType(base=AuthConfig.class, id="INGEF")
@Getter
public class IngefAuthConfig extends AuthConfig {
	@NotNull
	private String secret;

	@Override
	public AuthorizingRealm getRealm(MasterMetaStorage storage) {
		return new IngefRealm(storage, secret);
	}
}
