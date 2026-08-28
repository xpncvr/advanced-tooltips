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

import net.minecraft.block.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;
import com.github.reviversmc.advancedtooltips.api.InventoryProvider;
import com.github.reviversmc.advancedtooltips.tooltip.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
	@Shadow
	public abstract Block getBlock();

	public BlockItemMixin(Settings settings) {
		super(settings);
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		var config = AdvancedTooltips.getConfig();
		var containersConfig = config.getContainersConfig();
		var effectsConfig = config.getEffectsConfig();

		if (effectsConfig.hasBeacon() && this.getBlock() instanceof BeaconBlock) {
			var entityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
			NbtCompound blockEntityTag = entityData != null ? entityData.copyNbtWithoutId() : new NbtCompound();
			var effectsList = new ArrayList<StatusEffectInstance>();
			var primary = AdvancedTooltips.getRawEffectFromTag(blockEntityTag, "primary_effect");
			var secondary = AdvancedTooltips.getRawEffectFromTag(blockEntityTag, "secondary_effect");

			if (primary != null && primary.equals(secondary)) {
				primary = new StatusEffectInstance(primary.getEffectType(), 200, 1);
				secondary = null;
			}
			if (primary != null)
				effectsList.add(primary);
			if (secondary != null)
				effectsList.add(secondary);

			return Optional.of(new StatusEffectTooltipComponent(effectsList, 1F));
		} else if (this.getBlock() instanceof BeehiveBlock) {
			var data = BeesTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof CampfireBlock) {
			var data = CampfireTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof JukeboxBlock) {
			var data = JukeboxTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof SpawnerBlock) {
			var data = SpawnEntityTooltipComponent.ofMobSpawner(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof ChiseledBookshelfBlock) {
			var data = ChiseledBookshelfTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else {
			AdvancedTooltipsConfig.StorageContainerConfig currentBlockConfig = containersConfig.forBlock(this.getBlock());
			InventoryProvider.Context context = InventoryProvider.searchInventoryContextOf(stack, currentBlockConfig);

			if (currentBlockConfig == null) {
				currentBlockConfig = containersConfig.getStorageConfig();
			}

			if (context != null) {
				return InventoryTooltipComponent.of(stack, currentBlockConfig.isCompact(), currentBlockConfig.hasBlockRender(), context);
			}
		}

		return super.getTooltipData(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		if (this.getBlock() instanceof ShulkerBoxBlock && !AdvancedTooltips.isControlDown()) {
			AdvancedTooltips.appendBlockItemTooltip(stack, this.getBlock(), textConsumer);
			return;
		}

		super.appendTooltip(stack, context, displayComponent, textConsumer, type);
		AdvancedTooltips.appendBlockItemTooltip(stack, this.getBlock(), textConsumer);
	}
}
