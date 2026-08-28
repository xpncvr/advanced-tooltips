/*
 * Copyright (c) 2020 - 2022 LambdAurora <email@lambdaurora.dev>, Emi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.reviversmc.advancedtooltips.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.tooltip.StatusEffectTooltipComponent;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(PotionItem.class)
public abstract class PotionItemMixin extends Item {
	public PotionItemMixin(Settings settings) {
		super(settings);
	}

	@Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true, require = 0)
	private void onAppendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type, CallbackInfo ci) {
		if (AdvancedTooltips.getConfig().getEffectsConfig().hasPotions()) {
			ci.cancel();
		}
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().getEffectsConfig().hasPotions()) return super.getTooltipData(stack);
		var effects = new ArrayList<net.minecraft.entity.effect.StatusEffectInstance>();
		stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects().forEach(effects::add);
		return Optional.of(new StatusEffectTooltipComponent(effects, 1.f));
	}
}
