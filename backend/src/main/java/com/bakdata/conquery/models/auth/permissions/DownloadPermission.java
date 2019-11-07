package com.bakdata.conquery.models.auth.permissions;

import com.bakdata.conquery.io.cps.CPSType;

import lombok.ToString;

/**
 * Allows downloading of results.
 *
 */
@CPSType(id="DOWNLOAD_PERMISSION", base=ConqueryPermission.class)
@ToString(callSuper = true)
public class DownloadPermission extends SpecialPermission {
	
	public DownloadPermission(){
		super();
	}

}
