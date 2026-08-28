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

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;
import com.github.reviversmc.advancedtooltips.tooltip.*;
import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract Item getItem();

	@Inject(
			method = "getTooltip",
			at = @At(value = "RETURN", ordinal = 1),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void advancedtooltips$onGetTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type,
			CallbackInfoReturnable<List<Text>> cir, net.minecraft.component.type.TooltipDisplayComponent display, List<Text> tooltip) {
		var stack = (ItemStack) (Object) this;
		AdvancedTooltipsConfig.AdvancedConfig advancedTooltipsConfig = AdvancedTooltips.getConfig().getAdvancedConfig();

		if (advancedTooltipsConfig.hasLodestoneCoords() && stack.getItem() instanceof CompassItem) {
			var lodestone = stack.get(DataComponentTypes.LODESTONE_TRACKER);
			if (lodestone != null && lodestone.target().isPresent()) {
				GlobalPos globalPos = lodestone.target().get();
				BlockPos pos = globalPos.pos();
				var posText = Text.literal(String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()))
						.formatted(Formatting.GOLD);

				tooltip.add(Text.translatable("advancedtooltips.tooltip.lodestone_compass.target", posText).formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("advancedtooltips.tooltip.lodestone_compass.dimension",
								Text.literal(globalPos.dimension().getValue().toString()).formatted(Formatting.GOLD))
						.formatted(Formatting.GRAY));
			}
		}

		int repairCost;
		if (advancedTooltipsConfig.hasRepairCost() && (repairCost = stack.getOrDefault(DataComponentTypes.REPAIR_COST, 0)) != 0) {
			tooltip.add(Text.translatable("advancedtooltips.tooltip.repair_cost", repairCost)
					.formatted(Formatting.GRAY));
		}
	}

	@Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
	private void getTooltipData(CallbackInfoReturnable<Optional<TooltipData>> info) {
		// Data is the plural and datum is the singular actually, but no one cares
		var datas = new ArrayList<TooltipData>();
		info.getReturnValue().ifPresent(datas::add);

		var config = AdvancedTooltips.getConfig();
		var stack = (ItemStack) (Object) this;

		var food = stack.get(DataComponentTypes.FOOD);
		if (food != null) {
			if (config.getFoodConfig().isEnabled()) {
				datas.add(new FoodTooltipComponent(food));
			}

			if (config.getEffectsConfig().hasPotions()) {
				if (stack.isIn(AdvancedTooltips.HIDDEN_EFFECTS_TAG)
						|| AdvancedTooltips.hiddenEffectsItems.contains(stack.getItem())) {
					datas.add(new StatusEffectTooltipComponent());
				} else {
					var consumable = stack.get(DataComponentTypes.CONSUMABLE);
					var consumeEffectPairs = new ArrayList<Pair<StatusEffectInstance, Float>>();
					if (consumable != null) {
						for (var effect : consumable.onConsumeEffects()) {
							if (effect instanceof ApplyEffectsConsumeEffect applyEffects) {
								for (var instance : applyEffects.effects()) {
									consumeEffectPairs.add(new Pair<>(instance, applyEffects.probability()));
								}
							}
						}
					}

					var stew = stack.get(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
					if (!consumeEffectPairs.isEmpty()) {
						datas.add(new StatusEffectTooltipComponent(consumeEffectPairs));
					} else if (stew != null) {
						var effects = new ArrayList<StatusEffectInstance>();
						for (var stewEffect : stew.effects()) {
							effects.add(stewEffect.createStatusEffectInstance());
						}
						datas.add(new StatusEffectTooltipComponent(effects, 1.f));
					} else {
						var potionEffects = new ArrayList<StatusEffectInstance>();
						stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
								.getEffects().forEach(potionEffects::add);
						if (!potionEffects.isEmpty()) {
							datas.add(new StatusEffectTooltipComponent(potionEffects, 1.f));
						}
					}
				}
			}
		}

		if (stack.isOf(net.minecraft.item.Items.PAINTING)) {
			PaintingTooltipComponent.of(stack).ifPresent(datas::add);
		}

		var providesBannerPatterns = stack.get(DataComponentTypes.PROVIDES_BANNER_PATTERNS);
		if (providesBannerPatterns != null) {
			BannerTooltipComponent.of(providesBannerPatterns).ifPresent(datas::add);
		}

		var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		if (config.hasArmor() && equippable != null && equippable.slot().isArmorSlot()) {
			var attributeModifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
			double[] protection = {0};
			attributeModifiers.applyModifiers(equippable.slot(), (attribute, modifier) -> {
				if (attribute.matches(EntityAttributes.ARMOR)) {
					protection[0] += modifier.value();
				}
			});
			if (protection[0] > 0) {
				datas.add(new ArmorTooltipComponent((int) Math.round(protection[0])));
			}
		}

		if (datas.size() == 1) {
			info.setReturnValue(Optional.of(datas.get(0)));
		} else if (datas.size() > 1) {
			var comp = new CompoundTooltipComponent();
			for (var data : datas) {
				if (data instanceof ConvertibleTooltipData convertibleTooltipData) {
					comp.addComponent(convertibleTooltipData.getComponent());
				} else {
					comp.addComponent(TooltipComponentCallback.EVENT.invoker().getComponent(data));
				}
			}
			info.setReturnValue(Optional.of(comp));
		}
	}
}
