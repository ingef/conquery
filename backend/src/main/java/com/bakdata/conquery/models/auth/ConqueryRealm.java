package com.bakdata.conquery.models.auth;

import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthenticatingRealm;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@Getter @Setter
public abstract class ConqueryRealm extends AuthenticatingRealm {
	
	@JsonIgnore
	private AuthorizationStorage storage;
		
	public abstract AuthenticationToken extractToken(ContainerRequestContext request);
	
}
