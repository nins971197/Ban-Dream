package com.nins77.bandream.network;

import java.util.List;
import java.util.function.Supplier;

import com.nins77.bandream.client.BanClientHooks;
import com.nins77.bandream.config.BanType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SyncVisualPacket(List<VisualRule> rules, boolean enabled) {
    public static void encode(SyncVisualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
        buffer.writeCollection(packet.rules, (out, rule) -> {
            out.writeEnum(rule.type());
            out.writeUtf(rule.id());
            out.writeUtf(rule.dimension() == null ? "" : rule.dimension());
            out.writeUtf(rule.path() == null ? "" : rule.path());
            out.writeUtf(rule.value() == null ? "" : rule.value());
        });
    }

    public static SyncVisualPacket decode(FriendlyByteBuf buffer) {
        boolean enabled = buffer.readBoolean();
        List<VisualRule> rules = buffer.readList(in -> new VisualRule(
            in.readEnum(BanType.class),
            in.readUtf(),
            emptyToNull(in.readUtf()),
            emptyToNull(in.readUtf()),
            emptyToNull(in.readUtf())));
        return new SyncVisualPacket(rules, enabled);
    }

    public static void handle(SyncVisualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT, () -> () -> BanClientHooks.applyVisualSync(packet)));
        context.setPacketHandled(true);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
