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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.Optional;

/**
 * Represents a campfire tooltip. Displays a campfire inventory and the flame if lit.
 */
public class CampfireTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private final DefaultedList<ItemStack> inventory;
	private final Identifier fireTexture;

	public CampfireTooltipComponent(DefaultedList<ItemStack> inventory, Identifier fireTexture) {
		this.inventory = inventory;
		this.fireTexture = fireTexture;
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().getContainersConfig().isCampfireEnabled())
			return Optional.empty();

		var container = stack.get(DataComponentTypes.CONTAINER);
		if (container == null)
			return Optional.empty();

		var inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
		container.copyTo(inventory);

		boolean empty = true;
		for (var item : inventory) {
			if (!item.isEmpty()) {
				empty = false;
				break;
			}
		}

		if (empty)
			return Optional.empty();

		var itemId = Registries.ITEM.getId(stack.getItem());
		var fireId = Identifier.of(itemId.getNamespace(), "block/" + itemId.getPath() + "_fire");

		var blockState = stack.get(DataComponentTypes.BLOCK_STATE);
		if (blockState != null && "false".equals(blockState.properties().get("lit"))) {
			fireId = null;
		}

		return Optional.of(new CampfireTooltipComponent(inventory, fireId));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return 3 * 18 + 2;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return 3 * 18 + 2;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int xOffset, int yOffset, int width, int height, DrawContext context) {
		int x = 1 + 18 * 2;
		int y = 1 + 18 * 2;

		for (int i = 0; i < this.inventory.size(); i++) {
			var stack = this.inventory.get(i);

			InventoryTooltipComponent.drawSlot(context, x + xOffset - 1, y + yOffset - 1, null);
			context.drawItem(stack, xOffset + x, yOffset + y);
			context.drawStackOverlay(textRenderer, stack, xOffset + x, yOffset + y);

			if (i == 1)
				y -= 18 * 2;
			else if (i == 0)
				x -= 18 * 2;
			else if (i == 2)
				x += 18 * 2;
		}

		if (this.fireTexture != null) {
			// Sprites resolve through a SpriteIdentifier; getAtlasTexture keys off definition ids instead.
			var sprite = context.getSprite(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, this.fireTexture));
			context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, sprite, xOffset + 19, yOffset + 19, 16, 16);
		}
	}
}
