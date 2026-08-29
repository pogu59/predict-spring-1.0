package com.predict.service;

import com.predict.ShareClick;
import com.predict.User;
import com.predict.repository.ShareClickRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ShareClickService {

    /** 헷갈리는 0/O, 1/I 를 뺀 문자셋. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final ShareClickRepository shareClickRepository;
    private final SecureRandom random = new SecureRandom();

    public ShareClickService(ShareClickRepository shareClickRepository) {
        this.shareClickRepository = shareClickRepository;
    }

    public ShareClick recordClick(User user, String channel) {
        String referralCode = generateUniqueReferralCode();
        return shareClickRepository.save(new ShareClick(user, channel, referralCode));
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = randomCode();
        } while (shareClickRepository.existsByReferralCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
