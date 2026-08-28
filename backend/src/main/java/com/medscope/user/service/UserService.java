package com.medscope.user.service;

import com.medscope.common.exception.ResourceNotFoundException;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Fetch a user strictly by the id embedded in their own authenticated
     * context. Callers must never pass a client-supplied id here for a
     * "get my profile" style operation - see UserController#me().
     */
    public User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
