package com.bakdata.conquery.introspection;

import java.io.File;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassIntrospection extends AbstractNodeWithMemberIntrospection<ClassOrInterfaceDeclaration> {

	public ClassIntrospection(File file, ClassOrInterfaceDeclaration value) {
		super(file, value);
	}


}
