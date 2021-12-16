package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.util.SubjectPrincipalCollection;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.apache.shiro.authc.AuthenticationInfo;

/**
 * Specialization class of the {@link AuthenticationInfo} that enforces the use
 * of a {@link UserId} as primary principal.
 */
@SuppressWarnings("serial")
@Getter
@FieldNameConstants
@EqualsAndHashCode
public class ConqueryAuthenticationInfo implements AuthenticationInfo {

	private final SubjectPrincipalCollection principals;
	
	/**
	 * The credential a realm used for authentication.
	 */
	private final Object credentials;
	
	
	/**
	 * A realm can indicate whether a logout button is shown for the user or not
	 */
	private final boolean displayLogout; 

	public ConqueryAuthenticationInfo(Subject subject, Object credentials, ConqueryAuthenticationRealm realm, boolean displayLogout) {
		this.credentials = credentials;
		this.displayLogout = displayLogout;
		principals = new SubjectPrincipalCollection(subject, realm);
	}

}
