package com.bakdata.conquery.introspection;

import java.io.File;
import java.util.Arrays;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.collect.MoreCollectors;
import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.TypeSignature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassIntrospection extends AbstractJavadocIntrospection<ClassOrInterfaceDeclaration> {

	public ClassIntrospection(File file, ClassOrInterfaceDeclaration value) {
		super(file, value);
	}

	public ClassIntrospection findInnerType(String simpleName) {
		for(var decl : value.getMembers()) {
			if(decl.isClassOrInterfaceDeclaration()) {
				var cType = decl.asClassOrInterfaceDeclaration();
				if((value.getNameAsString()+"$"+cType.getNameAsString()).equals(simpleName)) {
					return new ClassIntrospection(file, cType);
				}
			}
		}
		throw new IllegalStateException(value.getNameAsString()+" has no inner type "+simpleName);
	}

	public Introspection findMethod(MethodInfo method) {
		var types = Arrays.stream(method.getParameterInfo())
			.map(p->p.getTypeSignatureOrTypeDescriptor())
			.map(this::toClass)
			.toArray(Class[]::new);
		
		return new AbstractJavadocIntrospection<>(file, 
			value
				.getMethodsByParameterTypes(types)
				.stream()
				.filter(md->md.getNameAsString().equals(method.getName()))
				.collect(MoreCollectors.onlyElement())
		);
	}
	
	private Class<?> toClass(TypeSignature sig) {
		if(sig instanceof BaseTypeSignature) {
			return ((BaseTypeSignature) sig).getType();
		}
		else if(sig instanceof ArrayTypeSignature) {
			return ((ArrayTypeSignature) sig).loadClass();
		}
		else if(sig instanceof ClassRefTypeSignature) {
			return ((ClassRefTypeSignature) sig).loadClass();
		}
		throw new IllegalStateException("Can't find class for signature "+sig);
	}

	public Introspection findField(FieldInfo field) {
		var f = value.getFieldByName(field.getName());
		if(f.isPresent()) {
			return new AbstractJavadocIntrospection<>(file, f.get());
		}
		log.warn("Could not find field "+field.getName());
		return super.findField(field);
	}
}
