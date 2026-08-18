package com.example.nova.service;

import com.example.nova.config.SecurityProperties;
import com.example.nova.dto.MfaSetupResponse;
import com.example.nova.entity.User;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Time-based One-Time-Password (TOTP) multi-factor authentication support,
 * compatible with Google Authenticator / Authy / 1Password etc.
 */
@Service
@RequiredArgsConstructor
public class MfaService {

    private final SecurityProperties securityProperties;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), timeProvider);

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public MfaSetupResponse buildSetupResponse(User user, String secret) {
        QrData qrData = new QrData.Builder()
                .label(user.getUsername())
                .secret(secret)
                .issuer(securityProperties.getMfa().getIssuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrBase64;
        try {
            byte[] png = qrGenerator.generate(qrData);
            // Utils.getDataUriForImage already returns a full "data:image/png;base64,...." URI
            qrBase64 = Utils.getDataUriForImage(png, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Unable to generate MFA QR code", e);
        }

        return new MfaSetupResponse(secret, qrBase64, qrData.getUri());
    }

    public boolean verifyCode(String secret, String code) {
        return code != null && secret != null && codeVerifier.isValidCode(secret, code);
    }
}
