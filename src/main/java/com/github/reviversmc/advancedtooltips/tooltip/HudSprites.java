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

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HudSprites {
	// Tooltips redraw every frame, so the identifiers are built once and reused.
	private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

	private HudSprites() {
	}

	private static Identifier id(String hudSpriteName) {
		return CACHE.computeIfAbsent(hudSpriteName, name -> Identifier.ofVanilla("hud/" + name));
	}

	public static void draw(DrawContext context, String hudSpriteName, int x, int y) {
		draw(context, hudSpriteName, x, y, 9);
	}

	public static void draw(DrawContext context, String hudSpriteName, int x, int y, int width) {
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, id(hudSpriteName), x, y, width, 9);
	}
}
