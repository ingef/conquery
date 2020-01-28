package com.bakdata.conquery.models.auth.basic;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.ConqueryRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.exceptions.validators.ExistingFile;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.io.BaseEncoding;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.TransactionalComputable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.hibernate.validator.constraints.NotEmpty;

@CPSType(id = "LOCAL_BASIC_CREDENTIAL", base = ConqueryRealm.class)
@Slf4j
public class BasicAuthRealm extends ConqueryRealm {
	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = UsernamePasswordToken.class;
	private static final String STORENAME =  "BasicCredentialStore";
	private static final String PREFIX =  "Basic";
	
	@Getter
	@NotEmpty
	private String storageId;
	@Getter
	@ExistingFile
	private File passwordStoreFile;
	@Getter
	private XodusConfig passwordStoreConfig;
	
	@JsonIgnore
	private Environment passwordEnvironment;
	@JsonIgnore
	private Store passwordStore;
		 
	public BasicAuthRealm() {
		this.setAuthenticationTokenClass(TOKEN_CLASS);
	}
	
	@Override
	protected void onInit() {
		super.onInit();
		passwordEnvironment = Environments.newInstance(passwordStoreFile, passwordStoreConfig.createConfig());
		passwordStore = passwordEnvironment.computeInTransaction(new TransactionalComputable<Store>(){
			@Override
			public Store compute(jetbrains.exodus.env.Transaction txn) {
				return passwordEnvironment.openStore(STORENAME, StoreConfig.WITH_DUPLICATES, txn);
			};
		});
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if(!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			// Incompatible token
			return null;
		}
		
		String username = (String) token.getPrincipal();
		String providedCredentials = new String((char[]) token.getCredentials());
		
		String storedCredentials = passwordEnvironment.computeInReadonlyTransaction(new TransactionalComputable<String>() {
			@Override
			public String compute(jetbrains.exodus.env.Transaction txn) {
				return StringBinding.entryToString(passwordStore.get(txn, StringBinding.stringToEntry(username)));
			};
		});
		
		if(!isCredentialValid(providedCredentials, storedCredentials)) {
			return null;
		}
		UserId userId = new UserId(username);
		User user = getStorage().getUser(userId);
		// try to construct a new User if none could be found in the storage
		if(user == null) {
			log.warn("Provided credentials were valid, but a corresponding user was not found in the System. Add a user to the system with the id: {}", userId);
			return null;
		}
		PrincipalCollection principals  = new SimplePrincipalCollection(List.of(userId), getName());
		return new SimpleAuthenticationInfo(principals, token.getCredentials());
	}

	private static boolean isCredentialValid(String providedCredentials, String storedCredentials) {
		return providedCredentials.equals(storedCredentials);
	}

	/**
	 * Code obtained from the Dropwizard project {@link BasicCredentialAuthFilter}.
	 */
	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {

        final String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
		if (header == null) {
            return null;
        }

        final int space = header.indexOf(' ');
        if (space <= 0) {
            return null;
        }

        final String method = header.substring(0, space);
        if (!PREFIX.equalsIgnoreCase(method)) {
            return null;
        }

        final String decoded;
        try {
            decoded = new String(BaseEncoding.base64().decode(header.substring(space + 1)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Error decoding credentials", e);
            return null;
        }

        // Decoded credentials is 'username:password'
        final int i = decoded.indexOf(':');
        if (i <= 0) {
            return null;
        }

        final String username = decoded.substring(0, i);
        final String password = decoded.substring(i + 1);
		return new UsernamePasswordToken(username, password);
	}

}
