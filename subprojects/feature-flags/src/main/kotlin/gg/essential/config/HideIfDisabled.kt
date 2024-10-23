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
package gg.essential.config

/**
 * Ensures that the annotated element is removed from the class file at build time if the given feature flag is
 * disabled at build time. Fails the build if this is not possible.
 *
 *
 * Note that this cannot (and MUST NOT) be used to control behavior because it unconditionally removes the element
 * at build time only. It has no effect at runtime (it is in fact removed at build time) and the element will not be
 * removed if the feature flag is set to any value other than `false` (e.g. A/B testing) at build time.
 * The only purpose of this annotation is to hide code which should not yet be visible to the general public.
 */
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.EXPRESSION, // function/constructor call arguments only
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class HideIfDisabled(
    /**
     * The name of a flag from [FeatureFlags].
     */
    val value: String
)
