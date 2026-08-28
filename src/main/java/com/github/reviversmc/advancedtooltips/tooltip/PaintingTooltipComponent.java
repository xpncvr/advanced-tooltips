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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Optional;

/**
 * Represents a painting tooltip. Displays the painting's art at a size proportional to its in-world dimensions.
 */
public class PaintingTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private static final int MAX_SIZE = 64;

	private final RegistryEntry<PaintingVariant> variant;
	private final int width;
	private final int height;

	private PaintingTooltipComponent(RegistryEntry<PaintingVariant> variant) {
		this.variant = variant;

		var value = variant.value();
		int blocksWide = value.width();
		int blocksTall = value.height();
		int scale = MAX_SIZE / Math.max(blocksWide, blocksTall);
		this.width = blocksWide * scale;
		this.height = blocksTall * scale;
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().hasPainting())
			return Optional.empty();

		var entityData = stack.get(DataComponentTypes.ENTITY_DATA);
		if (entityData == null)
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		if (client.world == null)
			return Optional.empty();

		var nbt = entityData.copyNbtWithoutId();
		var variantId = nbt.getString("variant").orElse(null);
		if (variantId == null)
			return Optional.empty();

		var id = net.minecraft.util.Identifier.tryParse(variantId);
		if (id == null)
			return Optional.empty();

		var registry = client.world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.PAINTING_VARIANT);
		var entry = registry.getEntry(id).orElse(null);
		if (entry == null)
			return Optional.empty();

		return Optional.of(new PaintingTooltipComponent(entry));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return this.height + 2;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return this.width;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		// Sprites resolve through a SpriteIdentifier; getAtlasTexture keys off definition ids instead.
		var sprite = context.getSprite(new SpriteIdentifier(TexturedRenderLayers.PAINTINGS_ATLAS_TEXTURE,
				this.variant.value().assetId().withPrefixedPath("painting/")));
		context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, sprite, x, y, this.width, this.height);
	}
}
