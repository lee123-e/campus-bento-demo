package com.neuedu.reggie.service;

import com.neuedu.reggie.exception.BusinessException;
import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CustomerVerificationService {

    private static final Duration RESEND_DELAY = Duration.ofSeconds(60);
    private static final Duration VALIDITY = Duration.ofMinutes(5);
    private final SecureRandom random = new SecureRandom();

    public Challenge issue(Challenge previous, String phone) {
        Instant now = Instant.now();
        if (previous != null && previous.code != null && now.isBefore(previous.issuedAt.plus(RESEND_DELAY))) {
            throw new BusinessException(BusinessException.Reason.RATE_LIMITED, "请等待 60 秒后再获取验证码。");
        }
        return new Challenge(phone, String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)), now);
    }

    public void verify(Challenge challenge, String phone, String code) {
        if (challenge == null || challenge.code == null || challenge.attemptsLeft == 0 || !Instant.now().isBefore(challenge.issuedAt.plus(VALIDITY))) {
            throw invalidCode();
        }
        challenge.attemptsLeft--;
        if (!challenge.phone.equals(phone) || !challenge.code.equals(code)) {
            throw invalidCode();
        }
        challenge.attemptsLeft = 0;
    }

    public long resendDelaySeconds() {
        return RESEND_DELAY.toSeconds();
    }

    private static BusinessException invalidCode() {
        return new BusinessException(BusinessException.Reason.INVALID_INPUT, "验证码错误或已失效，请检查后重试。");
    }

    public static final class Challenge implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String phone;
        private final transient String code;
        private final Instant issuedAt;
        private int attemptsLeft = 5;

        private Challenge(String phone, String code, Instant issuedAt) {
            this.phone = phone;
            this.code = code;
            this.issuedAt = issuedAt;
        }

        public String code() {
            return code;
        }
    }
}
