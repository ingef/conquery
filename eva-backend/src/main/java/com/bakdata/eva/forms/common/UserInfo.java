package com.bakdata.eva.forms.common;

import java.util.stream.Collectors;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class UserInfo {

	@NotEmpty
	private String name;
	@NotEmpty
	private String company;
	@NotEmpty
	private String email;
	
	public UserInfo(User user) {
		this.name = user.getName();
		this.email = user.getEmail();
		this.company = user.getRoles().stream().map(Mandator::getLabel).collect(Collectors.joining(", "));
	}

}
