package com.classbridge.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TenantServletFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void cleanUp(){
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateAs(Long userID, Long orgId, String role){
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(userID,orgId,role), null,
                        List.of(new SimpleGrantedAuthority("ROLE_"+role)))
                );

    }

    @Test
    void authenticatedRequest_setsTenantDuringRequest_andClearsAfter() throws Exception {
        authenticateAs(42L, 7L, "ADMIN");
        AtomicReference<Long> seenInsideRequest = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (req, res) -> seenInsideRequest.set(TenantContext.getOrgId()));

        assertEquals(7L, seenInsideRequest.get());
        assertNull(TenantContext.getOrgId());
    }

    @Test
    void  unauthenticatedRequest_allowsChainAndKeepsTenantNull() throws Exception{

        AtomicReference<Long> seenInsideRequest = new AtomicReference<>();

        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (req,res) ->{
                    chainCalled.set(true);
                    seenInsideRequest.set(TenantContext.getOrgId());

                }

        );

        assertTrue(chainCalled.get());
        assertNull(seenInsideRequest.get());
        assertNull(TenantContext.getOrgId());
    }

    @Test
    void exceptionInChain_stillClearsTenantContext(){

        authenticateAs(42L, 7L, "ADMIN");

        assertThrows(
                IllegalStateException.class,
                () -> filter.doFilter(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        (req, res) -> {
                            assertEquals(7L, TenantContext.getOrgId());
                            throw new IllegalStateException("boom");
                        })
        );

        assertNull(TenantContext.getOrgId());
    }
}
