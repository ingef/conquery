package com.bakdata.conquery.introspection;

import java.io.File;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import org.slf4j.LoggerFactory;

public interface Introspection {
	String getDescription();
	String getExample();
	File getFile();

	static Introspection from(File root, ClassInfo cl) {
		File f = new File(root, "backend/src/main/java/"+cl.getName().replace('.', '/')+".java");
		try {
			if(cl.isInnerClass()) {
				Introspection from = from(root, cl.getOuterClasses().get(cl.getOuterClasses().size() - 1));
				if (from instanceof SimpleIntrospection simpleIntrospection) {
					return simpleIntrospection;
				}

				if (from instanceof AbstractNodeWithMemberIntrospection<?> nodeWithMemberIntrospection) {
					return nodeWithMemberIntrospection.findInnerType(cl.getSimpleName());
				}
				else {
					return new SimpleIntrospection(f);
				}
			}
			
			
			CompilationUnit cu = StaticJavaParser.parse(f);

			TypeDeclaration<?> typeDeclaration = cu.getPrimaryType().get();

			if (typeDeclaration instanceof EnumDeclaration enumDeclaration) {
				return new EnumIntrospection(f, enumDeclaration);
			}
			return new AbstractNodeWithMemberIntrospection<>(f, typeDeclaration);
		} catch(Exception e) {
			LoggerFactory.getLogger(Introspection.class).warn("Could not create compilation unit for {}", cl.getName(), e);
			return new SimpleIntrospection(f);
		}
	}
	String getLine();
	Introspection findField(FieldInfo field);
	Introspection findMethod(MethodInfo method);
}
