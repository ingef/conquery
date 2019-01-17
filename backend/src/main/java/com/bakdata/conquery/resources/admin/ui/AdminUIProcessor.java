package com.bakdata.conquery.resources.admin.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AdminUIProcessor {
	private final MasterMetaStorage storage;
	
	public void createMandator(String name, String idString) throws JSONException {

		log.info("New mandator:\tName: {}\tId: {} ", name, idString);
		MandatorId mandatorId = new MandatorId(idString);
		Mandator mandator = new Mandator(new SinglePrincipalCollection(mandatorId));
		mandator.setLabel(name);
		mandator.setName(name);
		mandator.setStorage(storage);
		storage.addMandator(mandator);
	}
	
	public List<Mandator> getAllMandators(){
		return new ArrayList<>(storage.getAllMandators());
	}
	
	public List<User> getUsers(MandatorId mandatorId){
		Mandator mandator = (Mandator) mandatorId.getOwner(storage);
		Collection<User> user = storage.getAllUsers();
		return user.stream()
			.filter(u -> u.getRoles().contains(mandator))
			.collect(Collectors.toList());
	}
	
	public List<ConqueryPermission> getPermissions(PermissionOwnerId<?> id){
		PermissionOwner<?> owner = id.getOwner(storage);
		return new ArrayList<>(owner.getPermissions());
	}
}
