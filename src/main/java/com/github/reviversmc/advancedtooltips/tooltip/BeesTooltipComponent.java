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

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BeesComponent;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;

/**
 * Represents a tooltip component which displays bees from a beehive.
 */
public class BeesTooltipComponent extends EntityTooltipComponent<AdvancedTooltipsConfig.BeeEntityConfig> {
	private static final Identifier HONEY_LEVEL_TEXTURE = Identifier.of(AdvancedTooltips.NAMESPACE, "textures/tooltips/honey_level.png");

	private final List<Bee> bees = new ArrayList<>();
	private final int honeyLevel;

	public BeesTooltipComponent(AdvancedTooltipsConfig.BeeEntityConfig config, int honeyLevel, BeesComponent beesComponent) {
		super(config);
		this.honeyLevel = honeyLevel;

		for (var beeData : beesComponent.bees()) {
			var entity = beeData.loadEntity(this.client.world, this.client.player.getBlockPos());
			if (entity != null) {
				this.bees.add(new Bee(beeData.ticksInHive(), entity));
			}
		}
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		var config = AdvancedTooltips.getConfig().getEntitiesConfig().getBeeConfig();
		if (!config.isEnabled() && !config.shouldShowHoney())
			return Optional.empty();

		int honeyLevel = 0;
		var blockState = stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT);
		try {
			honeyLevel = Integer.parseInt(blockState.properties().getOrDefault("honey_level", "0"));
		} catch (NumberFormatException ignored) {
		}

		var beesComponent = stack.getOrDefault(DataComponentTypes.BEES, BeesComponent.DEFAULT);
		if (beesComponent.bees().isEmpty() && !config.shouldShowHoney())
			return Optional.empty();

		if (!beesComponent.bees().isEmpty() || config.shouldShowHoney())
			return Optional.of(new BeesTooltipComponent(config, honeyLevel, config.isEnabled() ? beesComponent : BeesComponent.DEFAULT));

		return Optional.empty();
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		if (this.bees.isEmpty()) {
			return this.config.shouldShowHoney() ? 12 : 0;
		} else {
			return (this.shouldRenderCustomNames() ? 32 : 24) + (this.config.shouldShowHoney() ? 16 : 0);
		}
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return Math.max(this.bees.size() * 26, (this.config.shouldShowHoney() ? 52 : 0));
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		if (!this.bees.isEmpty()) {
			int xOffset = x + 2;
			for (var bee : this.bees) {
				this.renderEntity(context, xOffset, y + (this.shouldRenderCustomNames() ? 12 : 4), bee.bee(), bee.ticksInHive(),
						this.config.shouldSpin(), true);
				xOffset += 26;
			}
		}

		if (config.shouldShowHoney()) {
			int honeyY = y + (this.bees.isEmpty() ? 0 : (this.shouldRenderCustomNames() ? 32 : 24));
			context.drawTexture(RenderPipelines.GUI_TEXTURED, HONEY_LEVEL_TEXTURE, x, honeyY, 0f, 0f, 52, 10, 32, 16);

			if (honeyLevel != 0) {
				context.drawTexture(RenderPipelines.GUI_TEXTURED, HONEY_LEVEL_TEXTURE, x, honeyY, 0f, 10f, Math.min(50, honeyLevel * 10 + 2), 12, 32, 16);
			}
		}
	}

	@Override
	protected boolean shouldRender() {
		return !this.bees.isEmpty();
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return this.bees.stream().map(bee -> bee.bee().hasCustomName()).reduce(false, (first, second) -> first || second)
				&& (this.config.shouldAlwaysShowName() || AdvancedTooltips.isControlDown());
	}

	record Bee(int ticksInHive, Entity bee) {
	}
}
