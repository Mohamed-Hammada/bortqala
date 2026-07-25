package com.bemo.license.tools;
import java.security.KeyPairGenerator;
import java.util.Base64;
public final class LicenseSigningKeyTool {
    private LicenseSigningKeyTool() { }
    public static void main(String[] args) throws Exception {
        var pair=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        System.out.println("LICENSE_SIGNING_PRIVATE_KEY="+Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        System.out.println("LICENSE_SIGNING_PUBLIC_KEY="+Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
    }
}
