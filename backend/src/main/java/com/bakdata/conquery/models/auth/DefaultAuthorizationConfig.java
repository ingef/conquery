package com.bakdata.conquery.models.auth;

import java.util.List;

import javax.validation.Valid;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

@CPSType(base = AuthorizationConfig.class, id = "DEFAULT")
@Getter
public class DefaultAuthorizationConfig implements AuthorizationConfig {

	@NotEmpty 
	@Valid
	private List<ProtoUser> initialUsers;

	@NotEmpty
	private List<String> overviewScope;

}
