package org.acme.utils;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;


/*
 * Stereotype for all test beans that should be injected in production code. 
 */


@Target({TYPE,METHOD})
@Retention(RUNTIME)

@Alternative
@Stereotype
public @interface TestDouble {}
