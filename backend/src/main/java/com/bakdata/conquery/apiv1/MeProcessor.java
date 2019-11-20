package com.bakdata.conquery.apiv1;

import java.util.ArrayList;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MeProcessor {

	private final MasterMetaStorage storage;

	public ArrayList<GroupId> getMyGroups(User user) {
		ArrayList<GroupId> myGroups = new ArrayList<>();
		for(Group group :storage.getAllGroups()) {
			if(group.containsMember(user)) {
				myGroups.add(group.getId());
			}
		}
		return myGroups;
	}

}
