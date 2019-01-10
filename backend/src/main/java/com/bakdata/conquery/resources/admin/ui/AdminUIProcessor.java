package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;

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
}
