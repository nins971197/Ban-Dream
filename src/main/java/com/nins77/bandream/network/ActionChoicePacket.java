package com.nins77.bandream.network;

import java.util.function.Supplier;

import com.nins77.bandream.enforce.BanActionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ActionChoicePacket(int actionId, int choice) {
    public static void encode(ActionChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.actionId);
        buffer.writeVarInt(packet.choice);
    }

    public static ActionChoicePacket decode(FriendlyByteBuf buffer) {
        return new ActionChoicePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ActionChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> BanActionHandler.applyChoice(player, packet.actionId(), packet.choice()));
        }
        context.setPacketHandled(true);
    }
}
