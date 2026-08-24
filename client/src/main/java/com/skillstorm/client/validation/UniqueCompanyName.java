package com.skillstorm.client.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueCompanyNameValidator.class)
public @interface UniqueCompanyName {

    String message() default "A client with this company name already exists";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
