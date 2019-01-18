package com.bakdata.conquery.models.exceptions.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.models.exceptions.validators.DetailedValid.DetailedValidValidator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DetailedValidValidator.class)
public @interface DetailedValid {

	String message() default "";
	
	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
	
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ValidationMethod2 {}
	
	
	class DetailedValidValidator implements ConstraintValidator<DetailedValid, Object> {
		
		private final Multimap<Class<?>, Method> methods = HashMultimap.create();
		
		@Override
		public void initialize(DetailedValid anno) {
		}

		@Override
		public boolean isValid(Object obj, ConstraintValidatorContext context) {
			Class<?> cl = obj.getClass();
			if(!methods.containsKey(cl)) {
				for(Method m:obj.getClass().getMethods()) {
					if(m.isAnnotationPresent(ValidationMethod2.class)
							&& boolean.class.equals(m.getReturnType())
							&& Arrays.equals(new Class<?>[]{ConstraintValidatorContext.class}, m.getParameterTypes())) {
						m.setAccessible(true);
						methods.put(cl, m);
					}
				}
			}
			
			context.disableDefaultConstraintViolation();
			boolean passed = true;
			for(Method m:methods.get(cl)) {
				try {
					passed &= (Boolean)m.invoke(obj, context);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new IllegalStateException(e);
				}
			}
			return passed;
		}
	}
}





