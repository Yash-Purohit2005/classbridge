package com.classbridge.security.auth;

import com.classbridge.auth.SignupService;
import com.classbridge.auth.dto.SignupRequest;
import com.classbridge.organization.Organization;
import com.classbridge.organization.OrganizationRepository;
import com.classbridge.user.Role;
import com.classbridge.user.User;
import com.classbridge.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SignupServiceTest {

    @Autowired
    private SignupService signupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerOrgAdmin_createsOrgAndAdminWithHashedPassword(){

        String orgName = "ABC University " + System.currentTimeMillis();
        String adminName = "Rohan Maliwal";
        String adminEmail = "rohan-" + System.currentTimeMillis() + "@gmail.edu";
        String password = "Password123!";

        SignupRequest request = new SignupRequest(
               orgName,
               adminName,
                adminEmail,
                password
        );

        User savedUser = signupService.registerOrgAdmin(request);

        assertNotNull(savedUser.getId());
        assertEquals(adminName,savedUser.getName());
        assertEquals(adminEmail,savedUser.getEmail());
        assertEquals(Role.ADMIN,savedUser.getRole());

        assertNotEquals(password,savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(password,savedUser.getPasswordHash()));

        Organization savedOrg = savedUser.getOrganization();
        assertNotNull(savedOrg.getId());
        assertEquals(orgName,savedOrg.getName());
        assertTrue(savedOrg.getSlug().startsWith("abc-university"));

        assertTrue(userRepository.existsByEmail(adminEmail));
        assertTrue(organizationRepository.existsBySlug(savedOrg.getSlug()));

    }

    @Test
    void registerOrgAdmin_duplicateEmail_throwsException() {
        String email = "dup-" + System.currentTimeMillis() + "@test.com";

        SignupRequest firstRequest = new SignupRequest("Org One " + System.currentTimeMillis(), "Admin One", email, "pass123");
        signupService.registerOrgAdmin(firstRequest);

        SignupRequest duplicateRequest = new SignupRequest("Org Two " + System.currentTimeMillis(), "Admin Two", email, "pass123");

        assertThrows(IllegalArgumentException.class, () -> signupService.registerOrgAdmin(duplicateRequest));
    }

    @Test
    void registerOrgAdmin_handlesSlugCollision() {
        String sharedOrgName = "Stark Tech " + System.currentTimeMillis(); // same value used twice on purpose

        SignupRequest first = new SignupRequest(sharedOrgName, "User One", "one-" + System.currentTimeMillis() + "@test.com", "pass123");
        SignupRequest second = new SignupRequest(sharedOrgName, "User Two", "two-" + System.currentTimeMillis() + "@test.com", "pass123");

        User user1 = signupService.registerOrgAdmin(first);
        User user2 = signupService.registerOrgAdmin(second);

        String firstSlug = user1.getOrganization().getSlug();
        String secondSlug = user2.getOrganization().getSlug();

        assertNotEquals(firstSlug, secondSlug);
        assertTrue(secondSlug.startsWith(firstSlug));
    }

}
