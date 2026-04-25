package com.zhitao.securitycomplianceapprovalcenter.common.config;

import com.zhitao.securitycomplianceapprovalcenter.common.filter.JwtAuthenticationFilter;
import com.zhitao.securitycomplianceapprovalcenter.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.function.Consumer;

/**
 * Common 模块安全配置
 * 提供通用的 JWT 过滤器和安全配置模板
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class CommonSecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    @ConditionalOnBean(JwtUtil.class)
    @ConditionalOnMissingBean(JwtAuthenticationFilter.class)
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 构建通用的 SecurityFilterChain
     *
     * @param http HttpSecurity
     * @param permitAllPatterns 放行路径模式
     * @return SecurityFilterChain
     */
    protected SecurityFilterChain buildFilterChain(HttpSecurity http, String... permitAllPatterns) throws Exception {
        http
            // 禁用 CSRF（JWT 不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用 Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权
            .authorizeHttpRequests(auth -> {
                // 放行指定路径
                if (permitAllPatterns != null && permitAllPatterns.length > 0) {
                    auth.requestMatchers(permitAllPatterns).permitAll();
                }
                // 其他接口需要认证
                auth.anyRequest().authenticated();
            })
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
