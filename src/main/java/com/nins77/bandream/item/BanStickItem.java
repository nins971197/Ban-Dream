package com.nins77.bandream.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class BanStickItem extends Item {
    public BanStickItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bandream.ban_stick.tooltip1")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bandream.ban_stick.tooltip2")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bandream.ban_stick.tooltip3")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
