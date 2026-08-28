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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.NbtReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.collection.DefaultedList;

import java.util.Optional;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;
import com.github.reviversmc.advancedtooltips.JukeboxTooltipMode;

/**
 * Represents a jukebox tooltip component. Displays the inserted disc description and an inventory slot with the disc in fancy mode.
 */
public class JukeboxTooltipComponent extends InventoryTooltipComponent {
	private final AdvancedTooltipsConfig config = AdvancedTooltips.getConfig();
	private final Text description;

	public JukeboxTooltipComponent(ItemStack discStack, Text description) {
		super(DefaultedList.ofSize(1, discStack), 1, null);
		this.description = description;
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().getJukeboxTooltipMode().isEnabled()) return Optional.empty();
		var entityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (entityData == null) return Optional.empty();

		var client = MinecraftClient.getInstance();
		if (client.world == null) return Optional.empty();

		var readView = NbtReadView.create(ErrorReporter.EMPTY, client.world.getRegistryManager(), entityData.copyNbtWithoutId());
		var discStack = readView.read("RecordItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		if (discStack.isEmpty()) return Optional.empty();

		if (discStack.get(DataComponentTypes.JUKEBOX_PLAYABLE) == null) return Optional.empty();

		return Optional.of(new JukeboxTooltipComponent(discStack, discStack.getName()));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		int height = 10;
		if (this.config.getJukeboxTooltipMode() == JukeboxTooltipMode.FANCY)
			height += 20;
		return height;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return textRenderer.getWidth(this.description);
	}

	@Override
	public void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
		context.drawText(textRenderer, this.description, x, y, 11184810, true);
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		if (this.config.getJukeboxTooltipMode() == JukeboxTooltipMode.FANCY)
			super.drawItems(textRenderer, x, y + 10, width, height, context);
	}
}
