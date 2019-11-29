package com.bakdata.conquery.apiv1;

import java.util.ArrayList;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class MeProcessor {

	private final MasterMetaStorage storage;

	public ArrayList<FEGroup> getMyGroups(User user) {
		ArrayList<FEGroup> myGroups = new ArrayList<>();
		for(Group group :storage.getAllGroups()) {
			if(group.containsMember(user)) {
				myGroups.add(FEGroup.from(group));
			}
		}
		return myGroups;
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	public static class FEGroup {

		private GroupId groupId;
		private String label;

		public static FEGroup from(Group group) {
			return new FEGroup(group.getId(), group.getLabel());
		}
	}

	public String getMyName(User user) {
		return storage.getUser(user.getId()).getLabel();
	}

}
