package com.sqlpractice.security;

import com.sqlpractice.config.AppProperties;
import com.sqlpractice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final AppProperties props;

    public JwtService(AppProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getJwt().getSecret()));
    }

    public String createAccessToken(User user) {
        return build(user, TYPE_ACCESS, props.getJwt().getAccessTtlMinutes() * 60);
    }

    public String createRefreshToken(User user) {
        return build(user, TYPE_REFRESH, props.getJwt().getRefreshTtlDays() * 86400);
    }

    private String build(User user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("typ", type)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long refreshTtlSeconds() {
        return props.getJwt().getRefreshTtlDays() * 86400;
    }
}
