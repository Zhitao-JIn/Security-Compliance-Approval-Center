package com.zhitao.securitycomplianceapprovalcenter.auth.util;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 功能：生成 Token、解析 Token、验证 Token 有效性
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成签名密钥（HS256 算法）
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * @param user 用户实体
     * @return JWT Token 字符串
     */
    public String generateToken(Map<String, Object> claims,User user) {
        // 自定义载荷：存入 userId 和 username
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        return createToken(claims, user.getUsername());
    }

    /**
     * 构建 Token
     * @param claims 自定义载荷
     * @param subject 主题（通常是用户名）
     * @return JWT Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)           // 设置自定义载荷
                .setSubject(subject)          // 设置主题（用户名）
                .setIssuedAt(now)             // 设置签发时间
                .setExpiration(expiryDate)    // 设置过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名算法和密钥
                .compact();
    }

    /**
     * 解析 Token 获取载荷（Claims）
     * @param token JWT Token
     * @return Claims 载荷对象
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中获取 userId
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取 username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取 Token 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * 判断 Token 是否过期
     */
    public Boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 验证 Token 是否有效
     * @param token JWT Token
     * @param user 用户实体（用于比对用户名）
     * @return 是否有效
     */
    public Boolean validateToken(String token, User user) {
        String username = getUsernameFromToken(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }
}
