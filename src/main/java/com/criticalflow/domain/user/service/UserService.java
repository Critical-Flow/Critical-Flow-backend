package com.criticalflow.domain.user.service;

import com.criticalflow.domain.auth.repository.RefreshTokenRepository;
import com.criticalflow.domain.conversation.repository.AiConversationRepository;
import com.criticalflow.domain.conversation.repository.AiMessageRepository;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import com.criticalflow.domain.user.dto.ProfileResponse;
import com.criticalflow.domain.user.dto.ProfileUpdateRequest;
import com.criticalflow.domain.user.dto.UserInfoResponse;
import com.criticalflow.domain.user.entity.User;
import com.criticalflow.domain.user.repository.UserRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AiConversationRepository aiConversationRepository;
    private final AiMessageRepository aiMessageRepository;
    private final StudyNoteRepository studyNoteRepository;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        return UserInfoResponse.from(findUser(userId));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(findUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = findUser(userId);
        user.update(request.name(), request.affiliation());
        return ProfileResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        findUser(userId);

        List<Long> conversationIds = aiConversationRepository.findConversationIdsByUserId(userId);
        if (!conversationIds.isEmpty()) {
            aiMessageRepository.deleteByConversationIdIn(conversationIds);
        }
        aiConversationRepository.deleteByUserId(userId);
        studyNoteRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND));
    }
}
