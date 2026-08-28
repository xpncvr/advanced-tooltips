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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.storage.NbtReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

import com.github.reviversmc.advancedtooltips.AdvancedTooltips;
import com.github.reviversmc.advancedtooltips.AdvancedTooltipsConfig;
import com.github.reviversmc.advancedtooltips.mixin.EntityAccessor;

public class SpawnEntityTooltipComponent extends EntityTooltipComponent<AdvancedTooltipsConfig.EntityConfig> {
	private final Entity entity;

	public SpawnEntityTooltipComponent(AdvancedTooltipsConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipData> of(EntityType<?> entityType, NbtCompound itemEntityNbt) {
		var entitiesConfig = AdvancedTooltips.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getSpawnEggConfig().isEnabled() || entityType == null)
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		itemEntityNbt = itemEntityNbt.copy();

		if (!itemEntityNbt.contains("VillagerData")) {
			var villagerData = new NbtCompound();
			villagerData.putString("profession", "minecraft:none");
			villagerData.putInt("level", 1);
			villagerData.putString("type", "minecraft:plains");
			itemEntityNbt.put("VillagerData", villagerData);
		}

		if (itemEntityNbt.contains(Entity.ID_KEY)) { // The spawn egg specifies its own entity type.
			var id = itemEntityNbt.getString(Entity.ID_KEY).orElse("");
			if (id.startsWith("minecraft:")) {
				id = id.substring(10);
			}
			if (id.replaceAll("[^a-z0-9/._-]", "").matches(id)) {
				itemEntityNbt.putString(Entity.ID_KEY, id);
				var readView = NbtReadView.create(ErrorReporter.EMPTY, client.world.getRegistryManager(), itemEntityNbt);
				Optional<EntityType<?>> specifiedEntityType = EntityType.fromData(readView);
				if (specifiedEntityType.isPresent()) {
					entityType = specifiedEntityType.get();
				}
			}
		}

		var entity = createEntityWithNbt(entityType, client.world, itemEntityNbt);
		if (entity != null) {
			adjustEntity(entity, itemEntityNbt, entitiesConfig);
			return Optional.of(new SpawnEntityTooltipComponent(entitiesConfig.getSpawnEggConfig(), entity));
		}

		return Optional.empty();
	}

	public static Optional<TooltipData> ofMobSpawner(ItemStack stack) {
		var entitiesConfig = AdvancedTooltips.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getMobSpawnerConfig().isEnabled())
			return Optional.empty();

		var entityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (entityData == null)
			return Optional.empty();
		var nbt = entityData.copyNbtWithoutId();

		var client = MinecraftClient.getInstance();

		var logic = new MobSpawnerLogic() {
			@Override
			public void sendStatus(World world, BlockPos pos, int eventType) {
			}
		};
		var pos = client.player.getBlockPos();
		logic.readData(client.world, pos, NbtReadView.create(ErrorReporter.EMPTY, client.world.getRegistryManager(), nbt));

		var entity = logic.getRenderedEntity(client.world, pos);
		if (entity != null) {
			return Optional.of(new SpawnEntityTooltipComponent(entitiesConfig.getMobSpawnerConfig(), entity));
		}

		return Optional.empty();
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return super.getHeight(textRenderer) + 36;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return 128;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		if (this.shouldRender()) {
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setVelocity(1.f, 1.f, 1.f);
			this.renderEntity(context, x + 50, y + 20, this.entity, 0, this.config.shouldSpin(), true, 90.f);
		}
	}

	@Override
	protected boolean shouldRender() {
		return this.entity != null;
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return this.entity.hasCustomName() && (this.config.shouldAlwaysShowName() || AdvancedTooltips.isControlDown());
	}
}
