package com.nins77.bandream.mixin;

import com.nins77.bandream.config.BanRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
    @Inject(method = "placeInChunk", at = @At("HEAD"), cancellable = true)
    private void bandream$preventBannedStructure(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkGenerator generator,
        RandomSource random,
        BoundingBox boundingBox,
        ChunkPos chunkPos,
        CallbackInfo callback
    ) {
        if (!BanRules.hasStructureRules()) {
            return;
        }
        StructureStart self = (StructureStart) (Object) this;
        ResourceLocation structureId = level.registryAccess()
            .registryOrThrow(Registries.STRUCTURE)
            .getKey(self.getStructure());
        if (structureId == null) {
            return;
        }
        ResourceLocation dimensionId = level.getLevel().dimension().location();
        if (BanRules.isStructureBanned(structureId, dimensionId)) {
            callback.cancel();
        }
    }
}
