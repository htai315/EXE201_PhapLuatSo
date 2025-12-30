package com.htai.exe201phapluatso.auth.oauth2;

import com.htai.exe201phapluatso.auth.entity.Role;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.RoleRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oidcUser);
        } catch (Exception ex) {
            logger.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OidcUser processOAuth2User(OidcUserRequest userRequest, OidcUser oidcUser) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oidcUser.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepo.findByEmail(userInfo.getEmail());
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            
            // Update existing user
            if (!user.getProvider().equals(registrationId.toUpperCase())) {
                throw new OAuth2AuthenticationException(
                    "Email already registered with " + user.getProvider() + " provider"
                );
            }
            
            user = updateExistingUser(user, userInfo);
        } else {
            // Create new user
            user = registerNewUser(registrationId, userInfo);
        }
        
        return new CustomOidcUser(oidcUser, user);
    }

    private User registerNewUser(String provider, OAuth2UserInfo userInfo) {
        User user = new User();
        user.setProvider(provider.toUpperCase());
        user.setProviderId(userInfo.getId());
        user.setEmail(userInfo.getEmail());
        user.setFullName(userInfo.getName());
        user.setAvatarUrl(userInfo.getImageUrl());
        user.setEmailVerified(true);
        user.setEnabled(true);
        // No password for OAuth users
        user.setPasswordHash(null);
        
        // Assign default USER role
        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new RuntimeException("USER role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        
        User savedUser = userRepo.save(user);
        logger.info("New OAuth2 user registered: {}", savedUser.getEmail());
        
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        existingUser.setFullName(userInfo.getName());
        existingUser.setAvatarUrl(userInfo.getImageUrl());
        existingUser.setEmailVerified(true);
        
        User updatedUser = userRepo.save(existingUser);
        logger.info("OAuth2 user updated: {}", updatedUser.getEmail());
        
        return updatedUser;
    }
}
