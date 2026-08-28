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
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;
import com.github.reviversmc.advancedtooltips.mixin.EntityAccessor;

/**
 * Represents a tooltip component which displays bees from a beehive.
 */
public class EntityBucketTooltipComponent extends EntityTooltipComponent<AdvancedTooltipsConfig.EntityConfig> {
	private final Entity entity;

	private EntityBucketTooltipComponent(AdvancedTooltipsConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipData> of(EntityType<?> type, NbtCompound itemNbt) {
		var entitiesConfig = AdvancedTooltips.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getFishBucketConfig().isEnabled())
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		var entity = createEntityWithNbt(type, client.world, itemNbt);
		if (entity != null) {
			adjustEntity(entity, itemNbt, entitiesConfig);
			return Optional.of(new EntityBucketTooltipComponent(entitiesConfig.getFishBucketConfig(), entity));
		}
		return Optional.empty();
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		if (this.shouldRender()) {
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setVelocity(1.f, 1.f, 1.f);
			this.renderEntity(context, x + 18, y + 2, this.entity, 0, this.config.shouldSpin(), false, 90.f);
		}
	}

	@Override
	protected boolean shouldRender() {
		return this.entity != null;
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return false;
	}
}
