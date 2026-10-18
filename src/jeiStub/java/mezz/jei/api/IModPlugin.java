package mezz.jei.api;

import mezz.jei.api.runtime.IJeiRuntime;

/**
 * Compile-only shape of JEI's plugin interface. The real interface is supplied
 * by JEI at runtime; this stub is excluded from the built jar.
 */
public interface IModPlugin {
    Object getPluginUid();

    default void onRuntimeAvailable(IJeiRuntime runtime) {
    }
}
