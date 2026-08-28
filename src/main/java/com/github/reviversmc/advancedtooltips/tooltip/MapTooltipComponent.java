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
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;

import java.util.Optional;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;

public class MapTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private final MinecraftClient client = MinecraftClient.getInstance();
	private final MapIdComponent map;
	private final MapRenderState renderState = new MapRenderState();

	public MapTooltipComponent(MapIdComponent map) {
		this.map = map;
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().getFilledMapConfig().isEnabled()) return Optional.empty();
		var map = stack.get(DataComponentTypes.MAP_ID);
		if (map == null) return Optional.empty();

		// The client only knows a map's contents once the server has sent them, so skip the
		// tooltip entirely rather than reserving space for a map that cannot be drawn.
		var client = MinecraftClient.getInstance();
		if (client.world == null || FilledMapItem.getMapState(map, client.world) == null)
			return Optional.empty();

		return Optional.of(new MapTooltipComponent(map));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return 128 + 2;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return 128;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		var state = FilledMapItem.getMapState(this.map, this.client.world);
		if (state == null) return;

		this.client.getMapRenderer().update(this.map, state, this.renderState);
		if (this.renderState.texture == null) return;

		context.drawTexture(RenderPipelines.GUI_TEXTURED, this.renderState.texture,
				x, y, 0f, 0f, 128, 128, 128, 128);
	}
}
