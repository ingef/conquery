package com.bakdata.conquery.util.support;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.AuthConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.eva.models.auth.IngefAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@Getter
public class TestEva extends TestConquery{
	
	@Override
	protected ConqueryConfig getConfig() throws Exception {
		ConqueryConfig cfg = super.getConfig();
		ObjectMapper mapper = Jackson.MAPPER.copy();
		String authStr = String.format("{\n" + 
				"		\"type\" : \"INGEF\",\n" + 
				"		\"secret\": \"%s\",\n" + 
				"		\"initialMandator\": {\n" + 
				"			\"label\": \"superMandator\",\n" + 
				"			\"name\": \"superMandator\",\n" + 
				"			\"principals\": \"mandator.999999998\"\n" + 
				"		}, \n" + 
				"		\"initialUser\": {\n" + 
				"			\"name\": \"superUser\",\n" + 
				"			\"label\": \"superUser\",\n" + 
				"			\"principals\": \"user.superUser\"\n" + 
				"		}\n" + 
				"	}", TestAuth.SECRET);
		AuthConfig authConfig = mapper.readValue(authStr , IngefAuthConfig.class);
		cfg.setAuthentication(authConfig);
		return cfg;
	}
}
