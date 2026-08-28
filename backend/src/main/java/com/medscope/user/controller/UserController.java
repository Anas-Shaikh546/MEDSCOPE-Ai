package com.medscope.user.controller;

import com.medscope.security.CurrentUser;
import com.medscope.user.dto.UserResponse;
import com.medscope.user.entity.User;
import com.medscope.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Deliberately GET /users/me, not GET /users/{id}.
     * The user id comes from the JWT via the security context, never from
     * a path/query parameter - this is what prevents one user from ever
     * being able to request another user's profile by guessing an id.
     */
    @GetMapping("/users/me")
    public UserResponse me(@CurrentUser Long authenticatedUserId) {
        User user = userService.getByIdOrThrow(authenticatedUserId);
        return UserResponse.from(user);
    }
}
