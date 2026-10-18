package com.nins77.bandream.network;

import com.nins77.bandream.BanDream;
import com.nins77.bandream.config.BanRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BanNetwork {
    private static final String VERSION = "1";
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
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new SyncJeiPacket(BanRules.hiddenJeiItems().stream().sorted().toList()));
    }

    public static void syncAll() {
        CHANNEL.send(PacketDistributor.ALL.noArg(),
            new SyncJeiPacket(BanRules.hiddenJeiItems().stream().sorted().toList()));
    }
}
