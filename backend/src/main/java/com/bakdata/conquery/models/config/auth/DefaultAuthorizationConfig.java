package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@CPSType(base = AuthorizationConfig.class, id = "DEFAULT")
@Getter
public class DefaultAuthorizationConfig implements AuthorizationConfig {

	@NotEmpty 
	@Valid
	private List<ProtoUser> initialUsers;

	@NotEmpty
	private List<String> overviewScope;
}
