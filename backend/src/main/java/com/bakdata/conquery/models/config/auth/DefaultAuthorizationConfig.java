package com.bakdata.conquery.models.config.auth;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;

@CPSType(base = AuthorizationConfig.class, id = "DEFAULT")
@Getter
public class DefaultAuthorizationConfig implements AuthorizationConfig {

	@NotEmpty 
	@Valid
	private List<ProtoUser> initialUsers;

	@NotEmpty
	private List<String> overviewScope;

}
