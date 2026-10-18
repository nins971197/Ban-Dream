package com.nins77.bandream.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class BanDreamJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation("bandream", "jei");
    private static final Set<ResourceLocation> HIDDEN = new LinkedHashSet<>();
    private static IJeiRuntime runtime;
    private static Set<ResourceLocation> applied = Set.of();

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refresh();
    }

    public static void acceptHiddenItems(List<String> ids) {
        synchronized (HIDDEN) {
            HIDDEN.clear();
            for (String value : ids) {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id != null) {
                    HIDDEN.add(id);
                }
            }
        }
    }

    public static void refresh() {
        if (runtime == null) {
            return;
        }
        Set<ResourceLocation> current = snapshotHidden();
        Set<ResourceLocation> toHide = new LinkedHashSet<>(current);
        toHide.removeAll(applied);
        Set<ResourceLocation> toShow = new LinkedHashSet<>(applied);
        toShow.removeAll(current);
        applied = current;
        applyStacks(toHide, true);
        applyStacks(toShow, false);
    }

    private static void applyStacks(Set<ResourceLocation> ids, boolean hide) {
        if (ids.isEmpty()) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ResourceLocation id : ids) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null) {
                stacks.add(new ItemStack(item));
            }
        }
        if (stacks.isEmpty()) {
            return;
        }
        try {
            Object ingredientManager = runtime.getClass().getMethod("getIngredientManager").invoke(runtime);
            Class<?> vanillaTypes = Class.forName("mezz.jei.api.constants.VanillaTypes");
            Object itemStackType = vanillaTypes.getField("ITEM_STACK").get(null);
            String methodName = hide ? "removeIngredientsAtRuntime" : "addIngredientsAtRuntime";
            Method method = ingredientManager.getClass().getMethod(
                methodName,
                Class.forName("mezz.jei.api.ingredients.IIngredientType"),
                Collection.class);
            method.invoke(ingredientManager, itemStackType, stacks);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Set<ResourceLocation> snapshotHidden() {
        synchronized (HIDDEN) {
            return Set.copyOf(HIDDEN);
        }
    }
}
