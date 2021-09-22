package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.auth0.jwt.JWT;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.util.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.util.LifecycleUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests the basic functionality of the LocalAuthenticationRealm, which stores
 * the credential data in a local database and offers simple user management
 * (add/update/delete).
 */
@TestInstance(Lifecycle.PER_CLASS)
public class LocalAuthRealmTest {

	private File tmpDir;
	private LocalAuthenticationRealm realm;
	private MetaStorage storage;
	private User user1;
	private ConqueryTokenRealm conqueryTokenRealm;

	@BeforeAll
	public void setupAll() throws Exception {
		storage =  new NonPersistentStoreFactory().createMetaStorage();
		tmpDir = Files.createTempDirectory(LocalAuthRealmTest.class.getName()).toFile();

		assert tmpDir.exists();

		conqueryTokenRealm = new ConqueryTokenRealm(storage);

		realm = new LocalAuthenticationRealm(Validators.newValidator(), Jackson.BINARY_MAPPER, conqueryTokenRealm, "localtestRealm", tmpDir, new XodusConfig(), Duration.hours(1));
		LifecycleUtils.init(realm);
	}

	@BeforeEach
	public void setupEach() {
		// Create User in Realm
		user1 = new User("TestUser", "Test User");
		PasswordCredential user1Password = new PasswordCredential("testPassword".toCharArray());
		storage.addUser(user1);
		realm.addUser(user1, List.of(user1Password));
	}

	@AfterEach
	public void cleanUpEach() {
		// Well there is an extra test case for this, but lets do it like this for now.
		realm.removeUser(user1);
	}

	@AfterAll
	public void cleanUpAll() {

		FileUtils.deleteQuietly(tmpDir);

	}

	@Test
	public void testEmptyUsername() {
		assertThatThrownBy(() -> realm.createAccessToken("", "testPassword".toCharArray()))
			.isInstanceOf(IncorrectCredentialsException.class).hasMessageContaining("Username was empty");
	}

	@Test
	public void testEmptyPassword() {
		assertThatThrownBy(() -> realm.createAccessToken("TestUser", "".toCharArray()))
			.isInstanceOf(IncorrectCredentialsException.class).hasMessageContaining("Password was empty");
	}

	@Test
	public void testWrongPassword() {
		assertThatThrownBy(() -> realm.createAccessToken("TestUser", "wrongPassword".toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");
	}

	@Test
	public void testWrongUsername() {
		assertThatThrownBy(() -> realm.createAccessToken("NoTestUser", "testPassword".toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");
	}

	@Test
	public void testValidUsernamePassword() {
		// Right username and password should yield a JWT
		String jwt = realm.createAccessToken("TestUser", "testPassword".toCharArray());
		assertThatCode(() -> JWT.decode(jwt)).doesNotThrowAnyException();

		assertThat(conqueryTokenRealm.doGetAuthenticationInfo(new BearerToken(jwt)).getPrincipals().getPrimaryPrincipal())
			.isEqualTo(new UserId("TestUser"));
	}

	@Test
	public void testUserUpdate() {

		realm.updateUser(user1, List.of(new PasswordCredential("newTestPassword".toCharArray())));
		// Wrong (old) password
		assertThatThrownBy(() -> realm.createAccessToken("TestUser", "testPassword".toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

		// Right (new) password
		String jwt = realm.createAccessToken("TestUser", "newTestPassword".toCharArray());
		assertThatCode(() -> JWT.decode(jwt)).doesNotThrowAnyException();
	}

	@Test
	public void testRemoveUser() {
		realm.removeUser(user1);
		// Wrong password
		assertThatThrownBy(() -> realm.createAccessToken("TestUser", "testPassword".toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

	}
}
