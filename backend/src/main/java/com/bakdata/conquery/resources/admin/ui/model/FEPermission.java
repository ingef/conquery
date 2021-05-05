package com.bakdata.conquery.resources.admin.ui.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TimeZone;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Frontend Permission -- special type that allows easier handling of permission in Freemarker.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FEPermission implements Comparable<FEPermission>{
	private static final ZoneId TIMEZONE = TimeZone.getDefault().toZoneId();

	private final Set<String> domains;
	private final Set<String> abilities;
	private final Set<String> targets;
	private final String creationTime;
	private final String rawPermission;
	
	public static FEPermission from(ConqueryPermission cPermission) {
		return new FEPermission(
			cPermission.getDomains(),
			cPermission.getAbilities(),
			cPermission.getInstances(),
			LocalDateTime.ofInstant(cPermission.getCreationTime(), TIMEZONE).format(DateTimeFormatter.ISO_DATE_TIME),
			cPermission.toString());
	}

	@Override
	public int compareTo(FEPermission o) {
		return rawPermission.compareTo(o.rawPermission);
	}

}
