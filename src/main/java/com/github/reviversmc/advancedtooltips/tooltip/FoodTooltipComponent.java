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
import com.github.reviversmc.advancedtooltips.SaturationTooltipMode;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.type.FoodComponent;

public record FoodTooltipComponent(FoodComponent component) implements ConvertibleTooltipData, TooltipComponent {
	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		var foodConfig = AdvancedTooltips.getConfig().getFoodConfig();

		int height = 11;
		if (foodConfig.hasHunger() && foodConfig.getSaturationMode() == SaturationTooltipMode.SEPARATED)
			height += 11;
		return height;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return Math.max(this.component.nutrition() / 2 * 9, (int) (this.saturationIcons() * 9));
	}

	/**
	 * {@return the saturation expressed in icons}
	 *
	 * <p>{@link FoodComponent#saturation()} is the final saturation value (nutrition * modifier * 2),
	 * while an icon represents two points of it.
	 */
	private float saturationIcons() {
		return this.component.saturation() / 2.f;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		var foodConfig = AdvancedTooltips.getConfig().getFoodConfig();

		int saturationY = y;
		if (foodConfig.getSaturationMode() == SaturationTooltipMode.SEPARATED && foodConfig.hasHunger()) saturationY += 11;

		// Draw hunger outline.
		if (foodConfig.hasHunger()) {
			for (int i = 0; i < (this.component.nutrition() + 1) / 2; i++) {
				HudSprites.draw(context, "food_empty", x + i * 9, y);
			}
		}

		// Draw saturation outline.
		float saturation = this.saturationIcons();
		if (foodConfig.getSaturationMode().isEnabled()) {
			for (int i = 0; i < saturation; i++) {
				int w = 9;
				if (saturation - i < 1f) {
					w = Math.round(w * (saturation - i));
				}
				if (w > 0) {
					HudSprites.draw(context, "food_empty", x + i * 9, saturationY, w);
				}
			}
		}

		// Draw hunger bars.
		if (foodConfig.hasHunger()) {
			for (int i = 0; i < this.component.nutrition() / 2; i++) {
				HudSprites.draw(context, "food_full", x + i * 9, y);
			}
			if (this.component.nutrition() % 2 == 1) {
				HudSprites.draw(context, "food_half", x + this.component.nutrition() / 2 * 9, y);
			}
		}

		// Draw saturation bar if separate (or alone).
		if (foodConfig.getSaturationMode() == SaturationTooltipMode.SEPARATED || !foodConfig.hasHunger()) {
			int intSaturation = Math.max(1, this.getSaturation());
			if (saturation * 2 - intSaturation > 0.2)
				intSaturation++;
			for (int i = 0; i < intSaturation / 2; i++) {
				HudSprites.draw(context, "food_full", x + i * 9, saturationY);
			}
			if (intSaturation % 2 == 1) {
				HudSprites.draw(context, "food_half", x + this.getSaturation() / 2 * 9, saturationY);
			}
		}
	}

	private int getSaturation() {
		return (int) this.component.saturation();
	}
}
