package com.bakdata.conquery.introspection;

import java.io.File;

import com.github.javaparser.ast.body.RecordDeclaration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordIntrospection extends AbstractNodeWithMemberIntrospection<RecordDeclaration> {
	public RecordIntrospection(File file, RecordDeclaration recordDeclaration) {
		super(file, recordDeclaration);
	}
}
