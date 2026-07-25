package com.bemo.license.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

@Service
public class LicenseCryptoService {
    private final PrivateKey signingPrivateKey;
    private final PublicKey signingPublicKey;

    public LicenseCryptoService(@Value("${license.signing.private-key:}") String privateKey,
                                @Value("${license.signing.public-key:}") String publicKey,
                                @Value("${license.signing.allow-ephemeral:false}") boolean allowEphemeral) {
        try {
            if (privateKey.isBlank() || publicKey.isBlank()) {
                if (!allowEphemeral) throw new IllegalStateException("Configure the Ed25519 license signing key pair.");
                var pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
                signingPrivateKey = pair.getPrivate(); signingPublicKey = pair.getPublic();
            } else {
                var factory = KeyFactory.getInstance("Ed25519");
                signingPrivateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
                signingPublicKey = factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            }
        } catch (GeneralSecurityException error) { throw new IllegalStateException("Invalid Ed25519 signing keys.", error); }
    }

    public String sign(String canonical) { return sign(signingPrivateKey, canonical); }
    public boolean verifyDevice(String encodedPublicKey, String canonical, String signature) {
        try {
            var publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encodedPublicKey)));
            var verifier = Signature.getInstance("Ed25519"); verifier.initVerify(publicKey);
            verifier.update(canonical.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (GeneralSecurityException | IllegalArgumentException error) { return false; }
    }
    public String publicKey() { return Base64.getEncoder().encodeToString(signingPublicKey.getEncoded()); }
    private String sign(PrivateKey key, String value) {
        try { var signature=Signature.getInstance("Ed25519");signature.initSign(key);signature.update(value.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(signature.sign()); }
        catch(GeneralSecurityException error){throw new IllegalStateException(error);}
    }
}
