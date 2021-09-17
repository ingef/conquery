package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenRealm;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.resources.api.ApiTokenResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.Data;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@CPSType(base = AuthenticationRealmFactory.class, id = "API_TOKEN")
@Data
public class ApiTokenRealmFactory implements AuthenticationRealmFactory {

	@NotNull
	private final Path storeDir;

	@NotNull
	private final XodusConfig apiTokenStoreConfig;

	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		final ApiTokenRealm apiTokenRealm = new ApiTokenRealm(managerNode.getStorage(), storeDir, apiTokenStoreConfig, managerNode.getEnvironment().getValidator(), managerNode.getEnvironment().getObjectMapper());


		JerseyEnvironment environment = managerNode.getEnvironment().jersey();
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(apiTokenRealm).to(ApiTokenRealm.class);
			}
		});
		environment.register(ApiTokenResource.class);

		return apiTokenRealm;
	}
}
