package com.kh.last.controller;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import com.kh.last.model.dto.*;
import com.kh.last.model.vo.USERS;
import com.kh.last.model.vo.Subscription;
import com.kh.last.service.UserService;
import com.kh.last.service.VisitService;
import com.kh.last.service.SubscriptionService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SecretKey key;

    @Autowired
    private VisitService visitService;

    @Autowired
    public UserController(UserService userService, SubscriptionService subscriptionService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.key = userService.getKey(); // UserService로부터 SecretKey 주입
    }

    // 사용자 등록 (회원가입)
    @PostMapping("/register")
    public ResponseEntity<USERS> createUser(@RequestBody UserCreateRequest request) {
        try {
            USERS createdUser = userService.createUser(request.getEmail(), request.getPassword(),
                    request.getStatus(), request.getBirthday(), request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 사용자 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody UserLoginRequest request) {
        try {
            String token = userService.loginUser(request.getEmail(), request.getPassword());
            visitService.updateVisitCount();

            // 구독 상태 확인
            boolean isSubscribed = subscriptionService.isUserSubscribed(request.getEmail());
            LoginResponse response = new LoginResponse(token);
            response.setSubscribed(isSubscribed);

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    // 현재 로그인된 사용자 정보 가져오기
    @GetMapping("/me")
    public ResponseEntity<USERS> getCurrentUser(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7); // "Bearer " 부분을 제거
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        String email = claims.getSubject();
        USERS user = userService.getUserByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 이메일 중복 체크
    @PostMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(@RequestBody EmailCheckRequest request) {
        boolean exists = userService.emailExists(request.getEmail());
        return ResponseEntity.ok(new EmailCheckResponse(exists));
    }

    // 비밀번호 확인
    @PostMapping("/check-password")
    public ResponseEntity<PasswordResponse> checkPassword(@RequestBody PasswordRequest request) {
        try {
            boolean isValid = userService.checkPassword(request.getPassword());
            visitService.updateVisitCount();
            return ResponseEntity.ok(new PasswordResponse(isValid));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PasswordResponse(false));
        }
    }

    // 사용자 구독 처리
    @PostMapping("/subscribe")
    public ResponseEntity<Subscription> subscribeUser(@RequestHeader("Authorization") String token, @RequestParam int months) {
        String jwt = token.substring(7); // "Bearer " 부분을 제거
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        String email = claims.getSubject();

        Subscription subscription = subscriptionService.subscribeUser(email, months);
        return ResponseEntity.ok(subscription);
    }
}