package com.nins77.bandream.network;

import java.util.List;
import java.util.function.Supplier;

import com.nins77.bandream.client.BanStickClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record OpenChoicePacket(
    int actionId,
    String title,
    String detail,
    ChoiceLayout layout,
    List<String> options
) {
    public static void encode(OpenChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.actionId);
        buffer.writeUtf(packet.title);
        buffer.writeUtf(packet.detail);
        buffer.writeEnum(packet.layout);
        buffer.writeCollection(packet.options, FriendlyByteBuf::writeUtf);
    }

    public static OpenChoicePacket decode(FriendlyByteBuf buffer) {
        return new OpenChoicePacket(
            buffer.readVarInt(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readEnum(ChoiceLayout.class),
            buffer.readList(FriendlyByteBuf::readUtf));
    }

    public static void handle(OpenChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT, () -> () -> BanStickClient.openChoice(packet)));
        context.setPacketHandled(true);
    }
}
