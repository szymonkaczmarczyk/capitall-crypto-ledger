package com.capitall.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final String ISSUER = "Capitall";
    private static final int PERIOD = 30;
    private static final int DIGITS = 6;
    private static final int DISCREPANCY = 1;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = buildVerifier();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    private DefaultCodeVerifier buildVerifier() {
        DefaultCodeVerifier v = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        v.setAllowedTimePeriodDiscrepancy(DISCREPANCY);
        v.setTimePeriod(PERIOD);
        return v;
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verify(String secret, String code) {
        if (secret == null || code == null) return false;
        return codeVerifier.isValidCode(secret, code.trim());
    }

    public String buildQrDataUri(String secret, String accountLabel) {
        try {
            QrData data = new QrData.Builder()
                    .label(accountLabel)
                    .secret(secret)
                    .issuer(ISSUER)
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(DIGITS)
                    .period(PERIOD)
                    .build();
            byte[] png = qrGenerator.generate(data);
            return Utils.getDataUriForImage(png, qrGenerator.getImageMimeType());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate QR code", ex);
        }
    }
}
