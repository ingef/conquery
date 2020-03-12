package com.bakdata.conquery.introspection;

import java.io.File;

import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;

public interface Introspection {
	String getDescription();
	String getExample();
	File getFile();
	
	public static Introspection from(ClassInfo cl) {
		File f = new File("../backend/src/main/java/"+cl.getName().replace('.', '/')+".java");
		try {
			if(cl.isInnerClass()) {
				ClassIntrospection parent = (ClassIntrospection)from(cl.getOuterClasses().get(cl.getOuterClasses().size()-1));
				return parent.findInnerType(cl.getSimpleName());
			}
			
			
			CompilationUnit cu = StaticJavaParser.parse(f);
			
			var type = cu.getPrimaryType().get().asClassOrInterfaceDeclaration();
			return new ClassIntrospection(f, type);
		} catch(Exception e) {
			LoggerFactory.getLogger(Introspection.class).warn("Could not create compilation unit for "+cl.getName(), e);
			return new SimpleIntrospection(f);
		}
	}
	String getLine();
	Introspection findField(FieldInfo field);
	Introspection findMethod(MethodInfo method);
}
