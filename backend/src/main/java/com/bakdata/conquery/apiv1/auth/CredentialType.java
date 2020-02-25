package com.bakdata.conquery.apiv1.auth;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.UserManageable;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A specific type of credential (e.g. a password, hashed password) that can be used by realms
 * that are {@link UserManageable} for user registration.
 * Make sure that any subtype is serializable (see SerializationTests).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface CredentialType {

}
