package com.edstem.library.book;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

public class NotInFutureYearValidator implements ConstraintValidator<NotInFutureYear, Integer> {

    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        return year == null || year <= Year.now().getValue();
    }
}
