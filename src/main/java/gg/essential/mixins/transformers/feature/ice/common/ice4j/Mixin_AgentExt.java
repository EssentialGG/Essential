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
package gg.essential.mixins.transformers.feature.ice.common.ice4j;

import gg.essential.mixins.impl.feature.ice.common.AgentExt;
import org.ice4j.ice.Agent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = Agent.class, remap = false)
public abstract class Mixin_AgentExt implements AgentExt {
    @Shadow
    @Final
    private List<PropertyChangeListener> stateListeners;

    @Unique
    private boolean isRemoteTricklingDone;

    @Override
    public boolean isRemoteTricklingDone() {
        return isRemoteTricklingDone;
    }

    @Override
    public void setRemoteTricklingDone() {
        if (isRemoteTricklingDone) {
            return;
        }
        isRemoteTricklingDone = true;

        List<PropertyChangeListener> stateListenersCopy;
        synchronized (stateListeners) {
            stateListenersCopy = new ArrayList<>(stateListeners);
        }

        PropertyChangeEvent evt = new PropertyChangeEvent(this, PROPERTY_REMOTE_TRICKLING_DONE, false, true);
        for (PropertyChangeListener l : stateListenersCopy) {
            l.propertyChange(evt);
        }
    }
}
