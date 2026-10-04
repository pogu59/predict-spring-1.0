package com.predict.service;

import com.predict.ShareClick;
import com.predict.User;
import com.predict.repository.ShareClickRepository;
import com.predict.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShareClickRepository shareClickRepository;

    public UserService(UserRepository userRepository, ShareClickRepository shareClickRepository) {
        this.userRepository = userRepository;
        this.shareClickRepository = shareClickRepository;
    }

    /**
     * 회원가입. referralCode가 주어지면 해당 공유 클릭을 찾아 연결한다.
     * user_id/가입일시/유입경로는 User 엔티티 자체가 레코드이므로 별도 로그가 필요 없다.
     */
    public User signup(String nickname, String signupChannel, String referralCode) {
        ShareClick referredBy = null;
        if (referralCode != null) {
            referredBy = shareClickRepository.findByReferralCode(referralCode)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 추천 코드: " + referralCode));
        }
        return userRepository.save(new User(nickname, signupChannel, referredBy));
    }

    /**
     * 카카오 로그인. 이미 가입된 카카오 회원번호면 그 유저를 반환하고,
     * 처음 보는 카카오 회원번호면 새 유저를 만든다(가입+로그인 동시 처리).
     * created=true면 프론트가 닉네임·약관 단계(/signup/nickname)로 보낸다.
     */
    public KakaoLogin findOrCreateByKakao(String kakaoId, String nickname) {
        Optional<User> existing = userRepository.findByKakaoId(kakaoId);
        if (existing.isPresent()) {
            return new KakaoLogin(existing.get(), false);
        }
        return new KakaoLogin(userRepository.save(new User(nickname, "kakao", null, kakaoId)), true);
    }

    public record KakaoLogin(User user, boolean created) {
    }
}
