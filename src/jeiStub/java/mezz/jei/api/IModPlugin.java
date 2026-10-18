package mezz.jei.api;

import mezz.jei.api.runtime.IJeiRuntime;

public interface IModPlugin {
    Object getPluginUid();

    default void onRuntimeAvailable(IJeiRuntime runtime) {
    }
}
