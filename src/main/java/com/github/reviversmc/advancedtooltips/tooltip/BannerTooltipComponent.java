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

import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a banner pattern tooltip. Draws the pattern flat, the way the loom screen previews its
 * pattern buttons, rather than as a 3D flag: the 3D path submits a special GUI element which is not
 * rendered during the tooltip pass.
 */
public class BannerTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private static final int BANNER_WIDTH = 20;
	private static final int BANNER_HEIGHT = 40;

	/** The flag occupies pixels x:[0,21) y:[1,41) of the 64x64 pattern sheet. */
	private static final float FLAG_U2 = 21.f / 64.f;
	private static final float FLAG_V1 = 1.f / 64.f;
	private static final float FLAG_V2 = 41.f / 64.f;

	private final List<RegistryEntry<BannerPattern>> patterns;

	private BannerTooltipComponent(List<RegistryEntry<BannerPattern>> patterns) {
		this.patterns = patterns;
	}

	public static Optional<TooltipData> of(TagKey<BannerPattern> pattern) {
		if (!AdvancedTooltips.getConfig().hasBannerPattern())
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		if (client.world == null) return Optional.empty();

		var registry = client.world.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN);
		var entries = registry.getOptional(pattern);
		if (entries.isEmpty()) return Optional.empty();

		var patterns = new ArrayList<RegistryEntry<BannerPattern>>();
		for (var entry : entries.get()) {
			patterns.add(entry);
		}

		if (patterns.isEmpty()) return Optional.empty();

		return Optional.of(new BannerTooltipComponent(patterns));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return BANNER_HEIGHT + 2;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return BANNER_WIDTH;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		context.fill(x, y, x + BANNER_WIDTH, y + BANNER_HEIGHT, DyeColor.GRAY.getEntityColor());

		for (var pattern : this.patterns) {
			var sprite = context.getSprite(TexturedRenderLayers.getBannerPatternTextureId(pattern));
			context.drawTexturedQuad(sprite.getAtlasId(),
					x, y, x + BANNER_WIDTH, y + BANNER_HEIGHT,
					sprite.getFrameU(0.f), sprite.getFrameU(FLAG_U2),
					sprite.getFrameV(FLAG_V1), sprite.getFrameV(FLAG_V2));
		}
	}
}
