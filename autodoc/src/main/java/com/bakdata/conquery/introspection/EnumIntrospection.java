package com.bakdata.conquery.introspection;

import java.io.File;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.javadoc.Javadoc;

public class EnumIntrospection extends AbstractNodeWithMemberIntrospection<EnumDeclaration> {


	public EnumIntrospection(File file, EnumDeclaration enumDeclaration) {
		super(file, enumDeclaration);
	}

	@Override
	protected String extractDescription() {
		String description = super.extractDescription();

		String values = value.getEntries().stream()
							 .map(e -> "`%s`: %s".formatted(e.getNameAsString(), e.getJavadoc().map(Javadoc::toText).orElse("No description available")))
							 .collect(Collectors.joining("\n- ", "\nValues:\n- ", ""));
		description += values;
		return description;
	}
}
