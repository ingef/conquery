package com.bakdata.conquery.models.auth.entities;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * Class to filter all unused functionality from the actual User class to keep it more compact and clean.
 * These are not supposed to be implemented.
 * 
 */
@JsonIgnoreProperties({ "session", "previousPrincipals", "runAs", "principal", "authenticated", "remembered", "principals" })
public abstract class FilteredUser implements Subject {


	@Override
	public boolean isPermitted(String permission) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean[] isPermitted(String... permissions) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPermittedAll(String... permissions) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkPermission(String permission) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkPermissions(String... permissions) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasRole(String roleIdentifier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean[] hasRoles(List<String> roleIdentifiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAllRoles(Collection<String> roleIdentifiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkRole(String roleIdentifier) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkRoles(Collection<String> roleIdentifiers) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkRoles(String... roleIdentifiers) throws AuthorizationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void login(AuthenticationToken token) throws AuthenticationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Session getSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Session getSession(boolean create) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V> V execute(Callable<V> callable) throws ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void execute(Runnable runnable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V> Callable<V> associateWith(Callable<V> callable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Runnable associateWith(Runnable runnable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void runAs(PrincipalCollection principals) throws NullPointerException, IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRunAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PrincipalCollection getPreviousPrincipals() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PrincipalCollection releaseRunAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAuthenticated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRemembered() {
		throw new UnsupportedOperationException();
	}
	

}
