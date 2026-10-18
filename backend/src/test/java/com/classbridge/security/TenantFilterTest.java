package com.classbridge.security;


import com.classbridge.organization.Organization;
import com.classbridge.organization.OrganizationRepository;
import com.classbridge.user.Role;
import com.classbridge.user.User;
import com.classbridge.user.UserRepository;
import com.classbridge.user.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class TenantFilterTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void filterRestrictsResultsToOneOrganization(){

        Organization org1 = new Organization();
        org1.setName("Org one");
        org1.setSlug("org-one-" + System.currentTimeMillis());
        org1 = organizationRepository.save(org1);

        Organization org2 = new Organization();
        org2.setName("Org Two");
        org2.setSlug("org-two-" + System.currentTimeMillis());
        org2 = organizationRepository.save(org2);


        User userInOrg1 = new User();
        userInOrg1.setOrganization(org1);
        userInOrg1.setName("User One");
        userInOrg1.setEmail("user1-" + System.currentTimeMillis() + "@test.com");
        userInOrg1.setPasswordHash("placeholder");
        userInOrg1.setRole(Role.STUDENT);
        userInOrg1.setStatus(UserStatus.ACTIVE);
        userRepository.save(userInOrg1);

        User userInOrg2 = new User();
        userInOrg2.setOrganization(org2);
        userInOrg2.setName("User Two");
        userInOrg2.setEmail("user2-" + System.currentTimeMillis() + "@test.com");
        userInOrg2.setPasswordHash("placeholder");
        userInOrg2.setRole(Role.STUDENT);
        userInOrg2.setStatus(UserStatus.ACTIVE);
        userRepository.save(userInOrg2);

        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("orgId", org1.getId());


        List<User> result = userRepository.findAll();

        assertEquals(1, result.size());
        assertEquals(userInOrg1.getId(), result.get(0).getId());
    }

}
