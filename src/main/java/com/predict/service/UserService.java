package com.predict.service;

import com.predict.ShareClick;
import com.predict.User;
import com.predict.repository.ShareClickRepository;
import com.predict.repository.UserRepository;
import org.springframework.stereotype.Service;

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
}
