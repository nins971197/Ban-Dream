package com.nins77.bandream.network;

import java.util.List;

import com.nins77.bandream.BanDream;
import com.nins77.bandream.config.BanRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BanNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(BanDream.MOD_ID, "sync"),
        () -> VERSION,
        NetworkRegistry.acceptMissingOr(VERSION),
        NetworkRegistry.acceptMissingOr(VERSION));

    private BanNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncJeiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncJeiPacket::encode)
            .decoder(SyncJeiPacket::decode)
            .consumerMainThread(SyncJeiPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenChoicePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(OpenChoicePacket::encode)
            .decoder(OpenChoicePacket::decode)
            .consumerMainThread(OpenChoicePacket::handle)
            .add();

        CHANNEL.messageBuilder(ActionChoicePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ActionChoicePacket::encode)
            .decoder(ActionChoicePacket::decode)
            .consumerMainThread(ActionChoicePacket::handle)
            .add();
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new SyncJeiPacket(BanRules.hiddenJeiItems().stream().sorted().toList()));
    }

    public static void syncAll() {
        CHANNEL.send(PacketDistributor.ALL.noArg(),
            new SyncJeiPacket(BanRules.hiddenJeiItems().stream().sorted().toList()));
    }

    public static void sendChoice(
        ServerPlayer player,
        int actionId,
        String title,
        String detail,
        ChoiceLayout layout,
        List<String> options
    ) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new OpenChoicePacket(actionId, title, detail, layout, options));
    }

    public static void sendAction(int actionId, int choice) {
        CHANNEL.sendToServer(new ActionChoicePacket(actionId, choice));
    }
}
