package com.bakdata.conquery.models.auth.oidc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.net.URI;

import javax.ws.rs.client.Client;

import com.bakdata.conquery.models.auth.OIDCMockServer;
import com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.model.JsonBody;

public class IdpConfigRetrievalTest {

	private static final OIDCMockServer OIDC_MOCK_SERVER = new OIDCMockServer();
	private static final JwtPkceVerifyingRealmFactory REALM_FACTORY = new JwtPkceVerifyingRealmFactory();

	private static final Client CLIENT = new JerseyClientBuilder(new Environment("oidc-test")).build("oidc-test-client");


	@BeforeAll
	static void init() {
		OIDC_MOCK_SERVER.init((server) -> {
			// MOCK JWK Endpoint (1 signing + 1 encryption key)
			server.when(request().withMethod("GET").withPath("/realms/" + OIDCMockServer.REALM_NAME + "/protocol/openid-connect/certs"))
				  .respond(
						  response().withBody(
								  JsonBody.json("{\"keys\":[{\"kid\":\"nW8YlwzTMqqOd6eqL1IXMZfBL7k2rSl8fX_NR80btvM\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"uUdwWKsu_ExHC7-VxS84ZbOxJBUK_DmMYvfbvmufPX5Vv1Rhg2DMB1oy1hrz2cJVA7cbnKpXuHj5DC6BJ2RuO-zaC9bXBmXVte5DiKHTtyljb1jxT2Yt3SwvZSTJG72ycGfDmkmqSM6RNlK5qN9RD7OfPl-JDKPRbJi71WZ-P8aIBL05IiY0DMHreOo3j09v1vVwz7g3ORh5dA17wrLHLXjgi1wGC6J4ZT6EVxXlwIxeU7R1pqLhvfVhZk74GD5qqWJKoF5t3JypirP3tnBf6zSeaIOXvLurSEPP7QqrPPbSfa0m9aHnwvF0o0lt2SHWwRMBUWdyuLUVJv4GU7F45w\",\"e\":\"AQAB\",\"x5c\":[\"MIIClzCCAX8CBgGMH0942TANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTIzMTEzMDA4MTczMVoXDTMzMTEzMDA4MTkxMVowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALlHcFirLvxMRwu/lcUvOGWzsSQVCvw5jGL3275rnz1+Vb9UYYNgzAdaMtYa89nCVQO3G5yqV7h4+QwugSdkbjvs2gvW1wZl1bXuQ4ih07cpY29Y8U9mLd0sL2UkyRu9snBnw5pJqkjOkTZSuajfUQ+znz5fiQyj0WyYu9Vmfj/GiAS9OSImNAzB63jqN49Pb9b1cM+4NzkYeXQNe8Kyxy144ItcBguieGU+hFcV5cCMXlO0daai4b31YWZO+Bg+aqliSqBebdycqYqz97ZwX+s0nmiDl7y7q0hDz+0Kqzz20n2tJvWh58LxdKNJbdkh1sETAVFncri1FSb+BlOxeOcCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAr7u7/VHUo6X2ig4JWAjeNVAcauYYLwnVpZpRKlqfx6GxyNZwGdxJSsfF4FTtfWODkLoOR6VJhQWzhpMiRIwrVIh6Oa9Qz1B8sbiM/AQA2WGI/ueLmcIzQdoOlOIZ6CiQA/P0virwrRu6ov0/ukOnuyLV9yRv08X4BnrfqIfIQTYzUhd0ylkF3l9lrrB1rxlGuWU0CA+jkj2D7JI7/0UPfHmbUzw1w70myHdnYzALnAWzQmHgWzmtMFmF0CW/EIanZ75/V6PMZ8+kOeBJfbJcD4C/fIYWIP174Uhu7xHNMQVtcnjUJ8si9VQgi9xR0kipXuu2UKFbPrMzHkqfNaOPxw==\"],\"x5t\":\"cDwkyVl_hhpJK69DIAxKqs_9vWE\",\"x5t#S256\":\"Y3hdo6JypgQC05Aa8qjkZi4me_JpEmsyTkc8n6Mkrcw\"},{\"kid\":\"ih_BbqHvev8DAqxCKAvIV6JI7A6L9fhwSBeDOJAtlIk\",\"kty\":\"RSA\",\"alg\":\"RSA-OAEP\",\"use\":\"enc\",\"n\":\"zCJQu4bWzdaHW5DJSJhKMKWjc6dNGYMGyPXYat90X4VixyQHrzYnH_0y0f0t88nmMClMrUawWr5oy7haok6IyVVFEhfviTMud0wLFCgNnS-rjSOwOsFk2hljo5NBjJCr-Y-MDv_r5vRdzmp1XKFLuqNplC6lB7JRiHGKZxHq3mXtFf7CtjspKRiPS5L1tKGfkJYdzEKL4jsOC53djWtJESTqvPINER4w0oDFR6cySc3Hrgu41wFov_v6g9lKt6Dj_yp5WrsDnO9yCfyS6Pg5wFWuMP6s0YG3v8e8E8Ss0ngO5ziEEFMolM2K-Orisq2koVDxbvHIipyIV3ZCofFVYQ\",\"e\":\"AQAB\",\"x5c\":[\"MIIClzCCAX8CBgGMH096ETANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTIzMTEzMDA4MTczMVoXDTMzMTEzMDA4MTkxMVowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMwiULuG1s3Wh1uQyUiYSjClo3OnTRmDBsj12GrfdF+FYsckB682Jx/9MtH9LfPJ5jApTK1GsFq+aMu4WqJOiMlVRRIX74kzLndMCxQoDZ0vq40jsDrBZNoZY6OTQYyQq/mPjA7/6+b0Xc5qdVyhS7qjaZQupQeyUYhximcR6t5l7RX+wrY7KSkYj0uS9bShn5CWHcxCi+I7Dgud3Y1rSREk6rzyDREeMNKAxUenMknNx64LuNcBaL/7+oPZSreg4/8qeVq7A5zvcgn8kuj4OcBVrjD+rNGBt7/HvBPErNJ4Duc4hBBTKJTNivjq4rKtpKFQ8W7xyIqciFd2QqHxVWECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAXkltYV9tfpK3STzLe0fyHwsVXhr8whCxBjleTVs3rmeNOjr8vjvB27NVI6jiOZZgj7oGTN9H8p8f4CeG+c8zZrwzxLdWRLrv1L7/B45RQbPGS5uYJ+Mcllqitx+y+YgVG+MHR0TJCjjpe8meYgAk7gCYtf+JAiRlqKqbmHuryqe1dGJ0v3ygDXM1HY3gKH1auE9kIDLnVbORzd0jRHjOjz72RpFFk+I7fJ05RRNyxmkVEKmpFT5K5DvrZizj1UNlsa8b8DWXwiWZDAiRpuNMR55hHrcj7S28TdTy+XfZVOTzJfAKuKQDfjgq1YuVq7EB9W0sx+gIvNvzxfnQiqpfdQ==\"],\"x5t\":\"KeyG6Ct9n6ufBknJClE5jS20LHU\",\"x5t#S256\":\"WYknXYM8JgRnURbarMyOeIITLgwSmfg69HCphgCb6mU\"}]}"
								  )));
		});

		REALM_FACTORY.setWellKnownEndpoint(URI.create(OIDCMockServer.MOCK_SERVER_URL
													  + "/realms/" + OIDCMockServer.REALM_NAME + "/.well-known/uma2-configuration"));
	}

	@AfterAll
	static void deinit() {
		OIDC_MOCK_SERVER.deinit();
	}

	@Test
	void getConfig() {
		assertThatCode(() -> REALM_FACTORY.retrieveIdpConfiguration(CLIENT)).doesNotThrowAnyException();
	}
}
