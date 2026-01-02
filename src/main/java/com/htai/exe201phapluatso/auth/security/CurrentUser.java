package com.htai.exe201phapluatso.auth.security;

import java.lang.annotation.*;

/**
 * Annotation to inject current authenticated user into controller methods
 * Usage: @CurrentUser User user
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
