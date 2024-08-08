package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Authorized;

public interface Owned extends Authorized {
    User getOwner();
}
