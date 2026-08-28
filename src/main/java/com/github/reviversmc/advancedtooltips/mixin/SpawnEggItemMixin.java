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
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;

import com.github.reviversmc.advancedtooltips.tooltip.SpawnEntityTooltipComponent;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin extends Item {
	public SpawnEggItemMixin(Settings settings) {
		super(settings);
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		var entityData = stack.get(DataComponentTypes.ENTITY_DATA);
		var nbt = entityData != null ? entityData.copyNbtWithoutId() : new NbtCompound();
		EntityType<?> type = entityData != null ? entityData.getType() : null;
		if (type == null) return super.getTooltipData(stack);
		return SpawnEntityTooltipComponent.of(type, nbt).or(() -> super.getTooltipData(stack));
	}
}
