package com.medscope.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated user's id (from the JWT / SecurityContext)
 * straight into a controller method parameter, e.g.:
 *
 *   @GetMapping("/users/me")
 *   public UserResponse me(@CurrentUser Long authenticatedUserId) { ... }
 *
 * This exists specifically so controllers never have a reason to accept
 * a client-supplied userId for "this user's own resource" endpoints.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
