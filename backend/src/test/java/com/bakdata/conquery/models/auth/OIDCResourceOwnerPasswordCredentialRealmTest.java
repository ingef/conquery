package com.bakdata.conquery.models.auth;

import static org.mockito.Mockito.mock;

import java.util.Map;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealm;
import com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import io.dropwizard.validation.BaseValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class OIDCResourceOwnerPasswordCredentialRealmTest {
	
	private static final MetaStorage STORAGE = mock(MetaStorage.class);
	private static final OIDCResourceOwnerPasswordCredentialRealmFactory CONFIG = new OIDCResourceOwnerPasswordCredentialRealmFactory();
	private static final Validator VALIDATOR = BaseValidator.newValidator();
	
	@BeforeAll
	public static void beforeAll() {
		CONFIG.setResource("test_cred");
		CONFIG.setCredentials(Map.of(OIDCResourceOwnerPasswordCredentialRealm.CONFIDENTIAL_CREDENTIAL, "test_cred"));
		CONFIG.setAuthServerUrl("http://localhost");
		
		ValidatorHelper.failOnError(log, VALIDATOR.validate(CONFIG));
	}
	
	@Test
	public void test() {
		TestRealm realm = new TestRealm(STORAGE, CONFIG);
		realm.init();
	}
	
	
	
	private static class TestRealm extends OIDCResourceOwnerPasswordCredentialRealm {

		public TestRealm(MetaStorage storage, OIDCResourceOwnerPasswordCredentialRealmFactory config) {
			super(storage, config);
			// TODO Auto-generated constructor stub
		}
		
	}
}
