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
package gg.essential.network.connectionmanager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class StateCallbackManager<E> {

    private final List<WeakReference<E>> stateCallbacks = new ArrayList<>();


    protected List<E> getCallbacks() {
        List<E> retainedManagers = new ArrayList<>();
        stateCallbacks.removeIf(ref -> {
            final E manager = ref.get();
            if (manager != null) {
                retainedManagers.add(manager);
                return false;
            } else {
                return true;
            }
        });

        return retainedManagers;
    }

    public void registerStateManager(E manager) {
        stateCallbacks.add(new WeakReference<>(manager));
    }
}
