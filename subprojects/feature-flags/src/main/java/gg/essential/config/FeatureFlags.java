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
package gg.essential.config;

import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Various feature flags which control the behavior of Essential.<br>
 * <br>
 * Before a public build is made, these will be propagated in the source code by feature-flags-processor and any
 * dead code will be eliminated.
 * Certain types of code (mixins, event handlers, command handler methods, etc.) are exempt from elimination because it
 * cannot know whether they are actually required. These can be manually annotated with {@link HideIfDisabled} if their
 * presence should be hidden in release builds.<br>
 * You may also apply {@link HideIfDisabled} to other targets to make sure they are actually eliminated if the
 * feature is disabled. The CI build will fail if any {@link HideIfDisabled} annotations for disabled features remain.
 * <br>
 * All feature flags may be overwritten at build time by setting corresponding environment variables (prefixed with
 * {@code ESSENTIAL_FEATURE_}, e.g. {@code ESSENTIAL_FEATURE_NEW_STUFF}) or the corresponding property in the
 * {@code features.properties} file (preferable because it can be committed to git and thereby allows for reproducible
 * builds) to either {@code true}, {@code false}, or a number between 0 and 100.<br>
 * If set to a number, the feature will be enabled for the given percentage of users based on their UUID.<br>
 * Additionally, if a feature flag is not forced to either true or false, it may be toggled at runtime by setting the
 * corresponding system property to {@code true} or {@code false} (e.g. {@code -Dessential.feature.new_stuff=true}).<br>
 */
public class FeatureFlags {

    private static final Logger LOGGER = LogManager.getLogger("Essential Logger");

    // Dropping Ice4J in favor of less buggy homebrew ICE implementation
    public static final String NEW_ICE_BACKEND = "NEW_ICE_BACKEND";
    public static final boolean NEW_ICE_BACKEND_ENABLED = property(NEW_ICE_BACKEND, abTesting(NEW_ICE_BACKEND, 0));


    // Add any features here that should be displayed in the FeaturesEnabledModal
    public static final Map<String, Pair<String, Boolean>> abTestingFlags = new HashMap<>();


    private static boolean property(String featureName, boolean defaultValue) {
        boolean result = defaultValue;
        String str = System.getProperty("essential.feature." + featureName.toLowerCase(Locale.ROOT));
        if (str != null) {
            result = Boolean.parseBoolean(str);
            LOGGER.warn("Explicitly {} feature flags \"{}\".", result ? "enabled" : "disabled", featureName);
        }
        return result;
    }

    @SuppressWarnings("unused") // used at build time depending on configuration
    private static boolean abTesting(String featureName, int enabledPercentage) {
        // To decide which group this user belongs to, we combine their UUID with the feature name, hash it, treat the
        // result as an integer and modulo it by 100, at which point we can directly compare it to the percentage.
        // This allows us to get an effectively random choice (sha1 results are uniformly distributed) that's the same
        // every time (as long as it's the same user) even if we add/remove or re-order the feature flags.
        // Additionally, given the algorithm does not depend on the percentage, even when we change how many people are
        // in each group, it does not flip for more people than strictly necessary.
        Supplier<String> userIdSupplier;
        try {
            Class<?> cls = Class.forName("gg.essential.config.FeatureFlagAbUserIdProvider");
            //noinspection unchecked
            userIdSupplier = (Supplier<String>) cls.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        String userId = userIdSupplier.get();
        String combinedId = featureName + ":" + userId;
        byte[] combinedHash;
        try {
            combinedHash = MessageDigest.getInstance("SHA-1").digest(combinedId.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException var2) {
            throw new RuntimeException(var2);
        }
        int userValue = new BigInteger(combinedHash).mod(BigInteger.valueOf(100)).intValue();
        boolean enabled = userValue < enabledPercentage;
        if (enabled) {
            LOGGER.info("Rolled a {}, enabling feature flag \"{}\".", userValue, featureName);
        }
        return enabled;
    }

}
