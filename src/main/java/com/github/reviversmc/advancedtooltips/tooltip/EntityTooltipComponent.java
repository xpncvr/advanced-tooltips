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
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;
import com.github.reviversmc.advancedtooltips.mixin.EntityAccessor;
import com.github.reviversmc.advancedtooltips.mixin.ItemEntityAccessor;
import com.github.reviversmc.advancedtooltips.mixin.WitherEntityAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents a tooltip component for entities.
 */
public abstract class EntityTooltipComponent<C extends AdvancedTooltipsConfig.EntityConfig> implements ConvertibleTooltipData, TooltipComponent {
	protected final MinecraftClient client = MinecraftClient.getInstance();
	protected final C config;

	private final Quaternionf leftRotation = new Quaternionf();
	private final Quaternionf rightRotation = new Quaternionf();
	private final Vector3f translation = new Vector3f();

	protected EntityTooltipComponent(C config) {
		this.config = config;
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return !this.shouldRender() ? 0 : (this.shouldRenderCustomNames() ? 32 : 24);
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return this.shouldRender() ? 24 : 0;
	}

	protected void renderEntity(DrawContext context, int x, int y, Entity entity, int ageOffset, boolean spin, boolean allowCustomName) {
		this.renderEntity(context, x, y, entity, ageOffset, spin, allowCustomName, 180.f);
	}

	protected void renderEntity(DrawContext context, int x, int y, Entity entity, int ageOffset, boolean spin, boolean allowCustomName, float defaultYaw) {
		float size = 24;
		if (Math.max(entity.getWidth(), entity.getHeight()) > 1.0) {
			size /= Math.max(entity.getWidth(), entity.getHeight());
		}
		if (entity instanceof SquidEntity) {
			size = 16;
		} else if (entity instanceof ItemEntity) {
			size = 48;
		}
		if (entity instanceof LivingEntity living && living.isBaby()) {
			size /= 1.7;
		}

		if (this.client.getCameraEntity() != null) {
			var camera = this.client.getCameraEntity();
			entity.setPos(camera.getX(), camera.getY(), camera.getZ());
		}
		this.setupAngles(entity, this.client.player.age, ageOffset, spin, defaultYaw);
		entity.setFireTicks(((EntityAccessor) entity).getHasVisualFire() ? 1 : entity.getFireTicks());
		entity.setCustomNameVisible(allowCustomName && entity.hasCustomName() && (this.config.shouldAlwaysShowName() || AdvancedTooltips.isControlDown()));

		var renderer = this.client.getEntityRenderDispatcher().getRenderer(entity);
		var state = renderer.getAndUpdateRenderState(entity, 1.0f);
		state.light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		state.shadowPieces.clear();
		state.outlineColor = 0;

		// Reused across frames: these are rebuilt from scratch each draw, not accumulated.
		this.leftRotation.identity().rotateZ((float) Math.PI);
		this.rightRotation.identity().rotateX(-10.f * 0.017453292f);
		this.leftRotation.mul(this.rightRotation);
		this.translation.set(0, state.height / 2f, 0);

		context.addEntity(state, size, this.translation, this.leftRotation, this.rightRotation, x, y, x + 24, y + 24);
	}

	protected void setupAngles(Entity entity, int age, int ageOffset, boolean spin, float defaultYaw) {
		entity.age = age + ageOffset;

		float yaw = spin ? (float) (((System.currentTimeMillis() / 10) + ageOffset) % 360) : defaultYaw;
		entity.setYaw(yaw);
		entity.setHeadYaw(yaw);
		entity.setPitch(0.f);
		if (entity instanceof LivingEntity living) {
			if (living instanceof GoatEntity) living.headYaw = yaw;
			else if (living instanceof WitherEntityAccessor wither) {
				wither.getSideHeadYaws()[0] = wither.getSideHeadYaws()[1] = yaw;
			}
			living.bodyYaw = yaw;
		} else if (entity instanceof ItemEntityAccessor itemEntity) {
			itemEntity.setItemAge(entity.age);
			itemEntity.setUniqueOffset(0.f);
		} else if (entity instanceof EndCrystalEntity endCrystal) {
			endCrystal.endCrystalAge = endCrystal.age;
		}
	}

	protected abstract boolean shouldRender();

	protected abstract boolean shouldRenderCustomNames();

	protected static Entity createEntityWithNbt(EntityType<?> type, World world, NbtCompound overlayNbt) {
		var entity = type.create(world, SpawnReason.LOAD);
		if (entity == null) return null;

		var uuid = entity.getUuid();
		var writeView = NbtWriteView.create(ErrorReporter.EMPTY, world.getRegistryManager());
		entity.writeData(writeView);
		var entityTag = writeView.getNbt();
		entityTag.copyFrom(overlayNbt);
		entity.setUuid(uuid);
		entity.readData(NbtReadView.create(ErrorReporter.EMPTY, world.getRegistryManager(), entityTag));
		return entity;
	}

	protected static void adjustEntity(Entity entity, NbtCompound itemNbt, AdvancedTooltipsConfig.EntitiesConfig config) {
		if (entity instanceof Bucketable bucketable) {
			bucketable.copyDataFromNbt(itemNbt);
			if (entity instanceof PufferfishEntity pufferfish) {
				pufferfish.setPuffState(config.getPufferFishPuffState());
			}
		}
	}
}
