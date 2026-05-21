package org.example.CampusMart.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.CampusMart.common.exception.CampusMartException;
import org.example.CampusMart.common.result.ResultCodeEnum;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final String DEFAULT_SECRET = "cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv";
    private static SecretKey secretKey = Keys.hmacShaKeyFor(getSecret().getBytes());

    public static String createToken(Long userId, String username) {
        String jwt = Jwts.builder().
                setExpiration(new Date(System.currentTimeMillis() + 3600000)).
                setSubject("LOGIN_USER").
                claim("userId",userId).
                claim("username",username).signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        return jwt;
    }

    public static Claims parseToken(String token){

        if (token == null) {
            throw new CampusMartException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }

        try {
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(secretKey).build();
            Jws<Claims> jws = jwtParser.parseClaimsJws(token);
            Claims claims = jws.getBody();
            return claims;
        }catch (ExpiredJwtException e){
            throw new CampusMartException(ResultCodeEnum.TOKEN_EXPIRED);
        }catch (JwtException e){
            throw new CampusMartException(ResultCodeEnum.TOKEN_INVALID);
        }
    }

    public static void main(String[] args) {
        System.out.println(createToken(8L, "13418049114"));
    }

    private static String getSecret() {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            return envSecret;
        }
        String propertySecret = System.getProperty("jwt.secret");
        if (propertySecret != null && !propertySecret.isBlank()) {
            return propertySecret;
        }
        return DEFAULT_SECRET;
    }
}
