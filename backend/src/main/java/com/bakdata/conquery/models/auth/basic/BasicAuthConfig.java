package com.bakdata.conquery.models.auth.basic;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.AuthorizationConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import lombok.Getter;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.hibernate.validator.constraints.NotEmpty;

@CPSType(base = AuthorizationConfig.class, id = "LOCAL_AUTHENTICATION")
@Getter
public class BasicAuthConfig implements AuthenticationConfig {
	
	@NotEmpty
	private String tokenSecret;
	
	private XodusConfig passwordStoreConfig = new XodusConfig();
	private String storeName = "authenticationStore";
	
	@Override
	public AuthenticatingRealm createRealm(MasterMetaStorage storage) {
		return new BasicAuthRealm(storage, this);
	}
}
