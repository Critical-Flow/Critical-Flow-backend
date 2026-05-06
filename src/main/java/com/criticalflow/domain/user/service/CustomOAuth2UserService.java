package com.criticalflow.domain.user.service;

import com.criticalflow.domain.user.entity.User;
import com.criticalflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long githubId = ((Number) attributes.get("id")).longValue();
        String name = (String) attributes.getOrDefault("name", attributes.get("login").toString());
        String email = attributes.get("email") != null
                ? (String) attributes.get("email")
                : githubId + "@users.noreply.github.com";

        userRepository.findByGithubId(githubId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .githubId(githubId)
                                .name(name)
                                .email(email)
                                .createdAt(LocalDateTime.now())
                                .build()
                ));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
    }
}
