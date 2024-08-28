/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.quic.backend;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;

/**
 * A simplified version of Netty's {@code SelfSignedCertificate} that does not suffer from any
 * <a href="https://github.com/bcgit/bc-java/issues/879">locale specific issues.</a>
 */
public class SelfSignedCert {

    /** Current time minus 1 year, just in case software clock goes back due to time synchronization */
    private static final Date DEFAULT_NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);

    /** The maximum possible value in X.509 specification: 9999-12-31 23:59:59 */
    private static final Date DEFAULT_NOT_AFTER = new Date(253402300799000L);

    private final X509Certificate certificate;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public SelfSignedCert() throws Exception {
        SecureRandom random = new SecureRandom();

        KeyPair keypair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048, random);
            keypair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            // Should not reach here because every Java implementation must have RSA and EC key pair generator.
            throw new Error(e);
        }

        X500Name owner = new X500Name("CN=localhost");

        BigInteger serial = new BigInteger(64, random);

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
            owner,
            serial,
            DEFAULT_NOT_BEFORE, DEFAULT_NOT_AFTER, Locale.ROOT,
            owner,
            SubjectPublicKeyInfo.getInstance(keypair.getPublic().getEncoded())
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keypair.getPrivate());
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certHolder);
        certificate.verify(keypair.getPublic());

        this.certificate = certificate;
        this.publicKey = keypair.getPublic();
        this.privateKey = keypair.getPrivate();
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public PrivateKey privateKey() {
        return privateKey;
    }
}
