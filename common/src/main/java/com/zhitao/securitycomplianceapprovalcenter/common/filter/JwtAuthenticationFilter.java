package com.zhitao.securitycomplianceapprovalcenter.common.filter;

import com.zhitao.securitycomplianceapprovalcenter.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 * 从请求头中提取 JWT Token，验证并设置用户认证信息
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头中获取 Token
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 验证 Token
                Claims claims = jwtUtil.parseToken(token);

                // 从 Token 中获取用户信息
                Long userId = claims.get("userId", Long.class);
                String username = claims.getSubject();

                // 安全地获取 roles 和 permissions（处理 null 和类型不匹配）
                List<String> roles = getClaimsList(claims, "roles");
                List<String> permissions = getClaimsList(claims, "permissions");

                // 设置请求头，供下游 Controller 使用
                if (userId != null) {
                    request.setAttribute("X-User-Id", userId.toString());
                }
                if (username != null) {
                    request.setAttribute("X-User-Name", username);
                }

                // 设置 Spring Security 上下文
                setAuthenticationContext(userId, username, roles, permissions, request);
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\": 401, \"message\": \"Token 已过期\"}");
            return;
        } catch (SignatureException e) {
            log.warn("JWT 签名验证失败：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\": 401, \"message\": \"Token 签名无效\"}");
            return;
        } catch (MalformedJwtException e) {
            log.warn("JWT 格式错误：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"code\": 400, \"message\": \"无效的 Token 格式\"}");
            return;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT 类型：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"code\": 400, \"message\": \"不支持的 Token 类型\"}");
            return;
        } catch (Exception e) {
            log.error("JWT 认证处理异常：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"code\": 500, \"message\": \"认证服务异常\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中获取 Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从 Authorization Header 获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从 X-Auth-Token Header 获取
        String token = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        return null;
    }

    /**
     * 从 Claims 中安全地获取 List<String>类型的值
     */
    private List<String> getClaimsList(Claims claims, String key) {
        Object obj = claims.get(key);
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * 设置认证上下文
     */
    private void setAuthenticationContext(Long userId, String username, List<String> roles,
                                          List<String> permissions, HttpServletRequest request) {
        // 转换权限为 GrantedAuthority
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 添加角色（Spring Security 角色需要 ROLE_ 前缀）
        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        // 添加权限
        if (permissions != null) {
            for (String permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        // 创建认证对象
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 设置到 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("用户认证成功：userId={}, username={}, roles={}, permissions={}", userId, username, roles, permissions);
    }
}
