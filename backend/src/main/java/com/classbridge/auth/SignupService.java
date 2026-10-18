package com.classbridge.auth;

import com.classbridge.auth.dto.SignupRequest;
import com.classbridge.organization.Organization;
import com.classbridge.organization.OrganizationRepository;
import com.classbridge.user.Role;
import com.classbridge.user.User;
import com.classbridge.user.UserRepository;
import com.classbridge.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SignupService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(OrganizationRepository organizationRepository, UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.organizationRepository=organizationRepository;
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }

    @Transactional
    public User registerOrgAdmin(SignupRequest request){
        if(userRepository.existsByEmail(request.adminEmail())){
            throw new IllegalArgumentException("User with email " + request.adminEmail() + " already exists.");
        }

        String slug= generateUniqueSlug(request.orgName());
        Organization org = new Organization();
        org.setName(request.orgName());
        org.setSlug(slug);
        Organization saved = organizationRepository.save(org);

        User admin= new User();
        admin.setName(request.adminName());
        admin.setEmail(request.adminEmail());
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setOrganization(saved);

        return userRepository.save(admin);
    }

    private String generateUniqueSlug(String orgName) {
        String baseSlug = orgName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (baseSlug.isBlank()) {
            baseSlug = "organization";
        }

        String slug = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
