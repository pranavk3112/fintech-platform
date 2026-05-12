package com.fintech.platform.user.controller;

import com.fintech.platform.common.response.ApiResponse;
import com.fintech.platform.user.dto.UserResponse;
import com.fintech.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse response = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", response));
    }
}