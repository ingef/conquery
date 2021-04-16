package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.auth.entities.User;

public interface Owned {
    User getOwner();
}
