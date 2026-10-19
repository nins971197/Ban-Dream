package com.nins77.bandream.block;

import com.nins77.bandream.BanDream;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BanBlocks {
    private static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, BanDream.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BanDream.MOD_ID);

    public static final RegistryObject<Block> BAKABAN =
        BLOCKS.register("bakaban", BakabanBlock::new);
    public static final RegistryObject<BlockEntityType<BakabanBlockEntity>> BAKABAN_ENTITY =
        BLOCK_ENTITY_TYPES.register("bakaban",
            () -> BlockEntityType.Builder.of(BakabanBlockEntity::new, BAKABAN.get()).build(null));

    private BanBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
