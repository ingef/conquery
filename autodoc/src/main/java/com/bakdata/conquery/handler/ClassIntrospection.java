package com.bakdata.conquery.handler;

import java.io.File;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;

import io.github.classgraph.ClassInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassIntrospection implements Introspection {
	@Getter
	private final ClassOrInterfaceDeclaration type;
	
	@Getter(lazy = true)
	private final String description = extractDescription();
	@Getter(lazy = true)
	private final String example = extractExample();

	public static ClassIntrospection from(ClassInfo cl) {
		try {
			if(cl.isInnerClass()) {
				ClassIntrospection parent = from(cl.getOuterClasses().get(cl.getOuterClasses().size()-1));
				return parent.findInnerType(cl.getSimpleName());
			}
			
			CompilationUnit cu = StaticJavaParser.parse(new File("../backend/src/main/java/"+cl.getName().replace('.', '/')+".java"));
			
			var type = cu.getPrimaryType().get().asClassOrInterfaceDeclaration();
			return new ClassIntrospection(type);
		} catch(Exception e) {
			throw new RuntimeException("Could not create compilation unit for "+cl.getName(), e);
		}
	}

	private ClassIntrospection findInnerType(String simpleName) {
		for(var decl : type.getMembers()) {
			if(decl.isClassOrInterfaceDeclaration()) {
				var cType = decl.asClassOrInterfaceDeclaration();
				if((type.getNameAsString()+"$"+cType.getNameAsString()).equals(simpleName)) {
					return new ClassIntrospection(cType);
				}
			}
		}
		throw new IllegalStateException(type.getNameAsString()+" has no inner type "+simpleName);
	}
	
	private String extractDescription() {
		if(!type.getJavadoc().isPresent()) {
			return "";
		}
		var javadoc = type.getJavadoc().get();
		return javadoc.getDescription().toText();
	}
	
	private String extractExample() {
		if(!type.getJavadoc().isPresent()) {
			return "";
		}
		var javadoc = type.getJavadoc().get();
		return javadoc.getBlockTags()
			.stream()
			.filter(b->b.getTagName().equals("jsonExample"))
			.findAny()
			.map(JavadocBlockTag::getContent)
			.map(JavadocDescription::toText)
			.orElse("");
	}
}
