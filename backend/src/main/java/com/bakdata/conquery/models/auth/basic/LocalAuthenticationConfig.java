package com.bakdata.conquery.models.auth.basic;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.config.XodusConfig;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

@CPSType(base = AuthenticationConfig.class, id = "LOCAL_AUTHENTICATION")
@Getter
public class LocalAuthenticationConfig implements AuthenticationConfig {
	
	@NotEmpty
	private String tokenSecret;
	
	@NotNull
	private XodusConfig passwordStoreConfig = new XodusConfig();
	@NotEmpty
	private String storeName = "authenticationStore";
	
	@Override
	public ConqueryAuthenticationRealm createRealm(MasterMetaStorage storage) {
		return new LocalAuthenticationRealm(storage, this);
	}
}
