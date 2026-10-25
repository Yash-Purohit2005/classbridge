package com.classbridge.security;

import com.classbridge.auth.SignupService;
import com.classbridge.auth.dto.SignupRequest;
import com.classbridge.organization.OrganizationRepository;
import com.classbridge.user.User;
import com.classbridge.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class TenantAwareTransactionManagerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SignupService signupService;

    private Long orgAId;
    private Long orgBId;

    private Long userAId;
    private Long userBId;

    @BeforeEach
    void setUp(){

        long ts = System.currentTimeMillis();

        User adminA = signupService.registerOrgAdmin(new SignupRequest(
                "Org A " + ts,
                "Admin A",
                "adminA-" + ts + "@test.edu",
                "Password123!"
        ));

        User adminB = signupService.registerOrgAdmin(
                new SignupRequest(
                        "Org B " + ts,
                        "Admin B",
                        "adminB-" + ts + "@test.edu",
                        "Password123!"
                )
        );

        userAId = adminA.getId();
        userBId = adminB.getId();

        orgAId = adminA.getOrganization().getId();
        orgBId = adminB.getOrganization().getId();
    }

    @AfterEach
    void cleanUp(){

        TenantContext.clear();

        if(userAId != null){
            userRepository.deleteById(userAId);
        }

        if(userBId!= null){
            userRepository.deleteById(userBId);
        }

        if (orgAId != null) {
            organizationRepository.deleteById(orgAId);
        }

        if (orgBId != null) {
            organizationRepository.deleteById(orgBId);
        }
    }

    @Test
    void tenantA_onlySeesTenantAUsers(){

        TenantContext.setOrgId(orgAId);


        List<Long> userIds = userRepository.findAll()
                .stream()
                .map(User::getId)
                .toList();

        assertTrue(userIds.contains(userAId));
        assertFalse(userIds.contains(userBId));

        TenantContext.clear();

    }

    @Test
    void  noTenant_seesAllUsers(){

        TenantContext.clear();

        List<Long> userIds = userRepository.findAll()
                .stream()
                .map(User::getId)
                .toList();

        assertTrue(userIds.contains(userAId));
        assertTrue(userIds.contains(userBId));
    }

    @Test
    void switchingTenants_switchesVisibleUsers() {

        TenantContext.setOrgId(orgAId);

        List<Long> orgAUsers = userRepository.findAll()
                .stream()
                .map(User::getId)
                .toList();

        assertTrue(orgAUsers.contains(userAId));
        assertFalse(orgAUsers.contains(userBId));

        TenantContext.setOrgId(orgBId);

        List<Long> orgBUsers = userRepository.findAll()
                .stream()
                .map(User::getId)
                .toList();

        assertTrue(orgBUsers.contains(userBId));
        assertFalse(orgBUsers.contains(userAId));

        TenantContext.clear();
    }

    @Test
    void tenantA_cannotFindUserBById() {

        TenantContext.setOrgId(orgAId);

        assertTrue(
                userRepository.findById(userBId).isEmpty()
        );

        TenantContext.clear();
    }

}
