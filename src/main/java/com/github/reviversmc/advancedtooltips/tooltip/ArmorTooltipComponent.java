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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

public class ArmorTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private final int prot;

	public ArmorTooltipComponent(int prot) {
		this.prot = prot;
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return 11;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return this.prot / 2 * 9;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		for (int i = 0; i < this.prot / 2; i++) {
			HudSprites.draw(context, "armor_full", x + i * 9, y);
		}
		if (this.prot % 2 == 1) {
			HudSprites.draw(context, "armor_half", x + this.prot / 2 * 9, y);
		}
	}
}
