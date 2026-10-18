package com.classbridge.security;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TenantContextTest {

    @AfterEach
    void cleanUp(){
        TenantContext.clear();
    }

    @Test
    void setThenGet_returnsTheSameValue(){
        TenantContext.setOrgId(5L);
        assertEquals(5L,TenantContext.getOrgId());
    }

    @Test
    void clear_removesTheValue(){
        TenantContext.setOrgId(5L);
        TenantContext.clear();
        assertNull(TenantContext.getOrgId());
    }

    @Test
    void valueIsNotVisibleFromAnotherThread() throws InterruptedException{

        TenantContext.setOrgId(5L);

        AtomicReference<Long> valueSeenByOtherThread = new AtomicReference<>();

        Thread otherThread = new Thread(() ->
                valueSeenByOtherThread.set(TenantContext.getOrgId())
        );

        otherThread.start();
        otherThread.join();

        assertNull(valueSeenByOtherThread.get());
        assertEquals(5L,TenantContext.getOrgId());
    }
}
