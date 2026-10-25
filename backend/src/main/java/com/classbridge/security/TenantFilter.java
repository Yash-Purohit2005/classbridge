package com.classbridge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)  throws ServletException, IOException {

        try{

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if(auth != null &&
                    auth.isAuthenticated() &&
                    auth.getPrincipal() instanceof AuthenticatedUser user ){

                if (user.orgId() == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                TenantContext.setOrgId(user.orgId());
            }

            filterChain.doFilter(request, response);

        }finally {
            TenantContext.clear();
        }
    }
}
