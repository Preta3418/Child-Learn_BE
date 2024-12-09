package com.prgrms.ijuju.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${secret.key}")
    private String SECRET_KEY;

    private static SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public static SecretKey getKey() {
        return key;
    }

    // Access Token 생성
    public static String encodeAccessToken(long minute, Map<String, Object> data) {

        return Jwts.builder()
                .subject("access_token")
                .expiration(getExpirationDate(minute))
                .claim("data", data)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Refresh Token 생성
    public static String encodeRefreshToken(long minute,Map<String, Object> data) {

        return Jwts.builder()
                .subject("refresh_token")
                .expiration(getExpirationDate(minute))
                .claim("data", data)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // 토큰에서 클레임 추출
    public static Claims decode(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰 만료 시간 계산
    private static Date getExpirationDate(long minute) {
        return new Date(System.currentTimeMillis() + 1000 * 60 * minute);
    }

}
