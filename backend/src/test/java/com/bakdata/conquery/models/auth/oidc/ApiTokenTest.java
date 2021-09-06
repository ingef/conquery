package com.bakdata.conquery.models.auth.oidc;

import static com.bakdata.conquery.models.auth.conquerytoken.ApiTokenCreator.*;
import static org.assertj.core.api.Assertions.*;

import com.bakdata.conquery.models.auth.conquerytoken.ApiTokenCreator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Random;

@Slf4j
public class ApiTokenTest {

	@Test
	public void checkToken () {
		final ApiTokenCreator apiTokenCreator = new ApiTokenCreator(new Random(1));

		final String token = apiTokenCreator.createToken();

		log.info("Testing token: {}", token);

		assertThat(token).hasSize(TOKEN_LENGTH + TOKEN_PREFIX.length() + 1);

		assertThat(token).matches(TOKEN_PREFIX + "_" + "[\\w\\d-_]{"+ TOKEN_LENGTH +"}");

		assertThat(token.substring(TOKEN_PREFIX.length()+2)).containsPattern("[a-zA-Z]");
	}
}
