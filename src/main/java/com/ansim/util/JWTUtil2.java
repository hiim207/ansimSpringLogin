package com.ansim.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class JWTUtil2 {

    @Value("${jwt.secret}")
    private String baseKey;
    private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    public static String getUserName(String token, String secretKey){
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).
                getBody().get("username", String.class);
    }

    public static boolean isExpired(String token, String secretKey){
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).
                getBody().getExpiration().before(new Date());
    }

    //키 설정
    private Key createKey() {
//        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(baseKey);
//        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
//        return signingKey;
        Key signingKey = Keys.secretKeyFor(signatureAlgorithm);
        return signingKey;
    }

    //토큰 생성
    public static String generateToken(Map<String,Object> payloads, String secretKey, Long expiredMs) {

        //헤더 부분 설정
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("typ", "JWT");
        headers.put("alg", "HS256");

        String username = (String) payloads.get("username");

        System.out.println(" webSConfig username:"+ username);

        Claims claims = Jwts.claims();
        claims.put("username",username);

        JwtBuilder builder = Jwts.builder()
                .setHeader(headers)
                .setClaims(claims) //username 등
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant())) //오늘 날짜
                .setExpiration(Date.from(ZonedDateTime.now().plusSeconds(expiredMs).toInstant()))
                //현재로부터 특정일 이후까지(만료일) 유효한 토큰 생성
                .signWith(SignatureAlgorithm.HS256, secretKey);

        String result = builder.compact();
        log.info("JWT = {}",result);
        return result;
    }

    //토큰 유효성 검사
    public String validateToken(String token) {

        try {
            Jwts.parserBuilder().setSigningKey(createKey()).build().parseClaimsJws(token);
            return "VALID_JWT";
        }catch(io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            return "INVALID_JWT";
        }catch(ExpiredJwtException e) {
            return "EXPIRED_JWT";
        }catch(UnsupportedJwtException e) {
            return "UNSUPPORTED_JWT";
        }catch(IllegalArgumentException e) {
            return "EMPTY_JWT";
        }

    }

    //http Authorization 헤더에서 토큰 가져 오기
    public String getTokenFromAuthorization(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(!bearerToken.isEmpty() && bearerToken.startsWith("Bearer"))
            return bearerToken.substring(7); //앞의 0-6까지의 문자는 짜르고 다음부터의 문자들을 가져 온다.
        return "INVALID_HEADER";
    }

    //토큰에서 email, password 추출
    public Map<String,Object> getDataFromToken(String token) throws Exception{

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(DatatypeConverter.parseBase64Binary(baseKey))
                .build()
                .parseClaimsJws(token)
                .getBody();

        Map<String, Object> data = new HashMap<>();
        data.put("user_id", claims.get("user_id").toString());
        data.put("password", claims.get("password").toString());

        return data;
    }

}
