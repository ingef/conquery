package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.entities.User;

public interface Owned extends Authorized {
    User getOwner();
}
