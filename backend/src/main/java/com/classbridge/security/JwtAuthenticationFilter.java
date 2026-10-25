package com.classbridge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService){
        this.jwtService=jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
              HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

       String header = request.getHeader("Authorization");

       if(header == null || !header.startsWith("Bearer ")){
           filterChain.doFilter(request,response);
           return;
       }

       String token = header.substring(7);

       try{
            Claims claims = jwtService.parseToken(token);

            Long userId = jwtService.extractUserId(claims);
            Long orgId = jwtService.extractOrgId(claims);
            String role = jwtService.extractRole(claims);

            if(orgId == null || role == null || role.isBlank()){
                throw new IllegalArgumentException("Invalid JWT claims");
            }

            AuthenticatedUser principal = new AuthenticatedUser(
                    userId,
                    orgId,
                    role
            );

            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

           UsernamePasswordAuthenticationToken authentication =
                   new UsernamePasswordAuthenticationToken(
                           principal, null, authorities
                   );

           SecurityContextHolder.getContext()
                   .setAuthentication(authentication);
       }catch (JwtException | IllegalArgumentException ex) {
           SecurityContextHolder.clearContext();
       }

       filterChain.doFilter(request,response);
    }
}
