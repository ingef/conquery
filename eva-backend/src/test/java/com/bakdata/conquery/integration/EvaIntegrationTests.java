package com.bakdata.conquery.integration;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.support.ConfigOverride;
import com.bakdata.conquery.util.support.TestAuth;
import com.bakdata.eva.idmapping.IngefIdMappingConfig;
import com.bakdata.eva.models.auth.IngefAuthConfig;

public class EvaIntegrationTests extends IntegrationTests implements ConfigOverride {

	private static final IngefAuthConfig AUTH_CONFIG;
	private static final String [] ADDITIONAL_DATE_FORMATS = {
		"dd.MM.yyyy"
	};
	
	static {
		AUTH_CONFIG = IngefAuthConfig
			.builder()
			.secret(TestAuth.SECRET)
			.initialMandator(new Mandator("999999998", "superMandator"))
			.initialUser(new User("superUser", "superUser"))
			.build();
	}
	
	public EvaIntegrationTests() {
		super("eva-tests/");
	}
	
	@Override
	public void override(ConqueryConfig config) {
		config.setAuthentication(AUTH_CONFIG);
		config.setAdditionalFormats(ADDITIONAL_DATE_FORMATS);
		config.setIdMapping(new IngefIdMappingConfig());
    }

	@Override
	@TestFactory @Tag(TestTags.INTEGRATION_JSON)
	public List<DynamicNode> jsonTests() throws IOException {
		return super.jsonTests();
	}
}
