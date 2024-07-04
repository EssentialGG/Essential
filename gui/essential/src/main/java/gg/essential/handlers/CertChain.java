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
package gg.essential.handlers;

import gg.essential.config.LoadsResources;
import kotlin.Pair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Stream-lined api for loading certificates into the default ssl context
 */
public class CertChain {
    private final CertificateFactory cf = CertificateFactory.getInstance("X.509");
    private final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

    public CertChain() throws Exception {
        InputStream keystoreInputStream = null;
        // Skip loading built-in certificates on internal builds to spot missing certificates faster
        // load the built-in certs!
        Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        keystoreInputStream = Files.newInputStream(ksPath);
        keyStore.load(keystoreInputStream, null);
    }

    @LoadsResources("/assets/essential/certs/%filename%.der")
    public CertChain load(String filename) throws Exception {
        try (InputStream cert = CertChain.class.getResourceAsStream("/assets/essential/certs/" + filename + ".der")) {
            InputStream caInput = new BufferedInputStream(cert);
            Certificate crt = cf.generateCertificate(caInput);
            keyStore.setCertificateEntry(filename, crt);
        }
        return this;
    }

    /**
     * Some versions of the game (mainly Java 8 on Windows) don't have the correct SSL certificates.
     * Therefore, we must load them manually.
     * <br>
     * Linear: EM-1923
     * Linear: EM-2165
     */
    public CertChain loadEmbedded() throws Exception {
        // Microsoft is transitioning their certificates to other root CAs because the current one expires in 2025.
        // https://docs.microsoft.com/en-us/azure/security/fundamentals/tls-certificate-changes
        // These are sorted alphabetically for easier comparison to assets folder
        return this
                .load("amazon-root-ca-1") // api.minecraftservices.com; (Unaffected but just for good measure (and so I can test these by using an empty system keystore))
                .load("baltimore-cybertrust-root") // Old root CA (in continued use)
                .load("d-trust-root-class-3-ca-2-2009") // New root CA
                .load("digicert-global-root-ca") // New root CA
                .load("digicert-global-root-g2") // New root CA
                .load("gts-root-r1") // modcore.me
                .load("isrgrootx1") // Other
                .load("lets-encrypt-r3") // Other
                .load("microsoft-ecc-root-ca-2017") // New root CA
                .load("microsoft-rsa-root-ca-2017") // New root CA
                ;
    }

    public Pair<SSLContext, TrustManager[]> done() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        return new Pair<>(sslContext, trustManagers);
    }
}
