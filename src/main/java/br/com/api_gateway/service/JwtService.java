package br.com.api_gateway.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private DecodedJWT decode(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("identity-ms")
                .build()
                .verify(token);
    }

    public boolean isTokenInvalid(String token) {
        try {
            decode(token);
            return false;
        } catch (JWTVerificationException e) {
            System.out.println(e.getMessage());
            return true;
        }
    }

    public Long extractId(String token) { return decode(token).getClaim("id").asLong(); }

    public List<String> extractRoles(String token) {
        return decode(token).getClaim("roles").asList(String.class);
    }

    public String extractSubject(String token) {
        return decode(token).getSubject();
    }

    public String extractName(String token) { return decode(token).getClaim("name").asString(); }
}