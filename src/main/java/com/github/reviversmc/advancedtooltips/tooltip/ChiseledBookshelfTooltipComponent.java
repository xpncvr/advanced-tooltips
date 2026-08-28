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

package com.github.reviversmc.advancedtooltips.tooltip;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.util.collection.DefaultedList;

import java.util.Optional;

/**
 * Represents a chiseled bookshelf tooltip. Displays the held books in the bookshelf's 3x2 slot layout.
 */
public class ChiseledBookshelfTooltipComponent extends InventoryTooltipComponent {
	private static final int SLOTS = 6;
	private static final int COLUMNS = 3;

	private ChiseledBookshelfTooltipComponent(DefaultedList<ItemStack> inventory) {
		super(inventory, COLUMNS, null);
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().hasChiseledBookshelf())
			return Optional.empty();

		var container = stack.get(DataComponentTypes.CONTAINER);
		if (container == null)
			return Optional.empty();

		var inventory = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);
		container.copyTo(inventory);

		if (inventory.stream().allMatch(ItemStack::isEmpty))
			return Optional.empty();

		return Optional.of(new ChiseledBookshelfTooltipComponent(inventory));
	}
}
