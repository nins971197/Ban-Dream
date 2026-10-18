package com.nins77.bandream.network;

import java.util.List;
import java.util.function.Supplier;

import com.nins77.bandream.client.BanClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SyncJeiPacket(List<String> hiddenItems) {
    public static void encode(SyncJeiPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.hiddenItems, FriendlyByteBuf::writeUtf);
    }

    public static SyncJeiPacket decode(FriendlyByteBuf buffer) {
        return new SyncJeiPacket(buffer.readList(FriendlyByteBuf::readUtf));
    }

    public static void handle(SyncJeiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT, () -> () -> BanClientHooks.applyJeiSync(packet.hiddenItems)));
        context.setPacketHandled(true);
    }
}
