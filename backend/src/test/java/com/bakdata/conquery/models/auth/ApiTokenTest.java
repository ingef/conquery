package com.bakdata.conquery.models.auth;

import static com.bakdata.conquery.models.auth.apitoken.ApiTokenCreator.*;
import static org.assertj.core.api.Assertions.*;

import com.bakdata.conquery.models.auth.apitoken.ApiToken;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenCreator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.CharArrayBuffer;
import org.junit.jupiter.api.Test;

import java.util.Random;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Slf4j
public class ApiTokenTest {

	@Test
	public void checkToken () {
		final ApiTokenCreator apiTokenCreator = new ApiTokenCreator(new Random(1));

		final @NotNull @NotEmpty CharArrayBuffer buffer = apiTokenCreator.createToken().getToken();

		log.info("Testing token: {}", buffer);

		assertThat(buffer).hasSize(TOKEN_LENGTH + TOKEN_PREFIX.length() + 1);

		assertThat(buffer).matches(TOKEN_PREFIX + "_" + "[\\w\\d_]{"+ TOKEN_LENGTH +"}");

		assertThat(buffer.toString().substring(TOKEN_PREFIX.length()+2)).containsPattern("[a-zA-Z]");
	}
}
