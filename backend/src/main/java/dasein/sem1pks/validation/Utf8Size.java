package dasein.sem1pks.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = Utf8SizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8Size {
    String message() default "Значение должно занимать не более {max} байт UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int max();
}
