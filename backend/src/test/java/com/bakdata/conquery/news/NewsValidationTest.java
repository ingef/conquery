package com.bakdata.conquery.news;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

public class NewsValidationTest {
    
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    @Test
    public void testValidIdLength() {
        NewsItem item = new NewsItem("valid-id", "Title", "Description", null, LocalDate.now());
        Set<ConstraintViolation<NewsItem>> violations = validator.validate(item);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    public void testInvalidIdTooLong() {
        NewsItem item = new NewsItem("a".repeat(101), "Title", "Description", null, LocalDate.now());
        Set<ConstraintViolation<NewsItem>> violations = validator.validate(item);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }
}