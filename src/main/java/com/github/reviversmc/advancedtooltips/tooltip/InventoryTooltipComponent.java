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

import com.github.reviversmc.advancedtooltips.api.InventoryProvider;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private static final int BLOCK_RENDER_SIZE = 48;

	private final List<ItemStack> inventory;
	private final int columns;
	private final DyeColor color;
	private final ItemStack blockRenderStack;

	public InventoryTooltipComponent(List<ItemStack> inventory, int columns, @Nullable DyeColor color) {
		this(inventory, columns, color, null);
	}

	public InventoryTooltipComponent(List<ItemStack> inventory, int columns, @Nullable DyeColor color, @Nullable ItemStack blockRenderStack) {
		this.inventory = inventory;
		this.columns = columns == 0 ? inventory.size() / 3 : columns;
		this.color = color;
		this.blockRenderStack = blockRenderStack;
	}

	public static Optional<TooltipData> of(ItemStack stack, boolean compact, @Nullable InventoryProvider.Context context) {
		return of(stack, compact, false, context);
	}

	public static Optional<TooltipData> of(ItemStack stack, boolean compact, boolean blockRender, @Nullable InventoryProvider.Context context) {
		if (context == null) {
			return Optional.empty();
		}

		List<ItemStack> inventory = context.inventory();

		if (inventory.stream().allMatch(ItemStack::isEmpty))
			return Optional.empty();

		int columns = Math.min(inventory.size() % 3 == 0 ? inventory.size() / 3 : inventory.size(), 9);

		if (compact) {
			var compactedInventory = new ArrayList<ItemStack>();
			inventory.forEach(invStack -> {
				if (invStack.isEmpty())
					return;
				compactedInventory.stream().filter(other -> ItemStack.areItemsAndComponentsEqual(other, invStack))
						.findFirst()
						.ifPresentOrElse(
								s -> s.increment(invStack.getCount()),
								() -> compactedInventory.add(invStack)
						);
			});

			inventory = compactedInventory;
			columns = 9;
		}

		return Optional.of(new InventoryTooltipComponent(inventory, columns, context.color(), blockRender ? stack : null));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		int rows = this.inventory.size() / this.getColumns();
		if (this.inventory.size() % this.getColumns() != 0)
			rows++;
		int height = 18 * rows + 3;
		if (this.blockRenderStack != null)
			height += BLOCK_RENDER_SIZE + 2;
		return height;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return Math.max(this.getColumns() * 18, this.blockRenderStack != null ? BLOCK_RENDER_SIZE : 0);
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int xOffset, int yOffset, int width, int height, DrawContext context) {
		if (this.blockRenderStack != null) {
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(xOffset, yOffset);
			context.getMatrices().scale(BLOCK_RENDER_SIZE / 16f, BLOCK_RENDER_SIZE / 16f);
			context.drawItem(this.blockRenderStack, 0, 0);
			context.getMatrices().popMatrix();
			yOffset += BLOCK_RENDER_SIZE + 2;
		}

		int x = 1;
		int y = 1;
		int lines = this.getColumns();

		for (var stack : this.inventory) {
			drawSlot(context, x + xOffset - 1, y + yOffset - 1, this.color);
			context.drawItem(stack, xOffset + x, yOffset + y);
			context.drawStackOverlay(textRenderer, stack, xOffset + x, yOffset + y);
			x += 18;
			if (x >= 18 * lines) {
				x = 1;
				y += 18;
			}
		}
	}

	/** Vanilla's inventory slot palette, as used by the container GUI textures. */
	private static final int SLOT_BASE = 0xFF8B8B8B;
	private static final int SLOT_SHADOW = 0xFF373737;
	private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;

	public static void drawSlot(DrawContext context, int x, int y, @Nullable DyeColor color) {
		int base = color == null ? SLOT_BASE : tint(SLOT_BASE, color.getEntityColor());
		int shadow = color == null ? SLOT_SHADOW : tint(SLOT_SHADOW, color.getEntityColor());
		int highlight = color == null ? SLOT_HIGHLIGHT : tint(SLOT_HIGHLIGHT, color.getEntityColor());

		context.fill(x + 1, y + 1, x + 18, y + 18, base);
		// Vanilla slots are inset: dark along the top/left, white along the bottom/right.
		context.fill(x, y, x + 18, y + 1, shadow);
		context.fill(x, y, x + 1, y + 18, shadow);
		context.fill(x + 1, y + 17, x + 18, y + 18, highlight);
		context.fill(x + 17, y + 1, x + 18, y + 18, highlight);
	}

	private static int tint(int argb, int tintRgb) {
		int r = (((argb >> 16) & 0xFF) * ((tintRgb >> 16) & 0xFF)) / 255;
		int g = (((argb >> 8) & 0xFF) * ((tintRgb >> 8) & 0xFF)) / 255;
		int b = ((argb & 0xFF) * (tintRgb & 0xFF)) / 255;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	protected int getColumns() {
		return this.columns;
	}
}
