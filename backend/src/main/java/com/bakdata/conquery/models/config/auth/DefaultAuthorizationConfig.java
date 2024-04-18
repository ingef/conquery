package com.bakdata.conquery.models.config.auth;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.apiv1.auth.ProtoRole;
import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@CPSType(base = AuthorizationConfig.class, id = "DEFAULT")
@Data
public class DefaultAuthorizationConfig implements AuthorizationConfig {

	@Valid
	private List<ProtoRole> initialRoles = List.of(ProtoRole.builder()
															.name("admin")
															.permissions(Set.of(AdminPermission.DOMAIN))
															.build());

	@Valid
	private List<ProtoUser> initialUsers = Collections.emptyList();

	@NotEmpty
	private List<String> overviewScope = List.of(DatasetPermission.DOMAIN, AdminPermission.DOMAIN, SuperPermission.DOMAIN);

	@ValidationMethod(message = "No initial entities defined. Access will not be possible")
	public boolean isInitialAccessPossible() {
		return !(initialRoles.isEmpty() && initialUsers.isEmpty());
	}
}
