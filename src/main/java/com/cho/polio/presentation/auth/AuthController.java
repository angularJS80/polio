package com.cho.polio.presentation.auth;

import com.cho.polio.presentation.auth.dto.RequestRefresh;
import com.cho.polio.presentation.enums.ApiPaths;
import com.polio.poliokeycloak.keycloak.helper.KeycloakAuthHelper;
import com.polio.poliokeycloak.keycloak.helper.dto.UserLoginRequest;
import com.polio.poliokeycloak.keycloak.helper.dto.UserLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAuthHelper keycloakAuthHelper;

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody UserLoginRequest userLoginRequest) {

        UserLoginResponse userLoginResponse = keycloakAuthHelper.auth(userLoginRequest);

        return ResponseEntity.ok(userLoginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponse> refresh(@RequestBody RequestRefresh refreshToken) {

        UserLoginResponse userLoginResponse = keycloakAuthHelper.authByRefresh(refreshToken.getRefreshToken());

        return ResponseEntity.ok(userLoginResponse);
    }

}