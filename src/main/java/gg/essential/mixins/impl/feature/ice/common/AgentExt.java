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
package gg.essential.mixins.impl.feature.ice.common;

import java.beans.PropertyChangeEvent;

public interface AgentExt {
    /**
     * The name of the {@link PropertyChangeEvent} that we use to deliver
     * events on changes of the {@link #isRemoteTricklingDone()} state.
     */
    String PROPERTY_REMOTE_TRICKLING_DONE = "RemoteTricklingDone";

    /**
     * Whether the remote end has indicated that it is done trickling new candidates.
     */
    boolean isRemoteTricklingDone();
    void setRemoteTricklingDone();
}
