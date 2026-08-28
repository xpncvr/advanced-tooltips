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
import com.github.reviversmc.advancedtooltips.SignTooltipMode;

import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SignItem;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.storage.NbtReadView;
import net.minecraft.text.OrderedText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.ErrorReporter;

import java.util.Optional;

public class SignTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	/**
	 * Face sub-region of the sign sheet, excluding the post and the back.
	 *
	 * <p>The board occupies pixels x:[2,26) y:[2,14) of a 64x32 texture, expressed here as the
	 * normalized fractions {@link Sprite#getFrameU} expects.
	 */
	private static final float FACE_U1 = 2.f / 64.f;
	private static final float FACE_U2 = 26.f / 64.f;
	private static final float FACE_V1 = 2.f / 32.f;
	private static final float FACE_V2 = 14.f / 32.f;
	private static final int PADDING = 4;

	private final SignTooltipMode tooltipMode = AdvancedTooltips.getConfig().getSignTooltipMode();
	private final OrderedText[] text;
	private final DyeColor color;
	private final boolean glowingText;
	private final Sprite sprite;
	private final int textWidth;

	public SignTooltipComponent(OrderedText[] text, DyeColor color, boolean glowingText, Sprite sprite) {
		this.text = text;
		this.color = color;
		this.glowingText = glowingText;
		this.sprite = sprite;

		var textRenderer = MinecraftClient.getInstance().textRenderer;
		int max = 0;
		for (var line : text) {
			max = Math.max(max, textRenderer.getWidth(line));
		}
		this.textWidth = max;
	}

	public static Optional<TooltipData> fromItemStack(ItemStack stack) {
		if (!AdvancedTooltips.getConfig().getSignTooltipMode().isEnabled())
			return Optional.empty();

		if (!(stack.getItem() instanceof SignItem))
			return Optional.empty();

		var entityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (entityData == null)
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		if (client.world == null)
			return Optional.empty();

		var readView = NbtReadView.create(ErrorReporter.EMPTY, client.world.getRegistryManager(), entityData.copyNbtWithoutId());
		var signText = readView.read("front_text", SignText.CODEC).orElse(null);
		if (signText == null)
			return Optional.empty();

		var messages = signText.getMessages(client.shouldFilterText());
		var lines = new OrderedText[messages.length];
		for (int i = 0; i < messages.length; i++) {
			lines[i] = messages[i].asOrderedText();
		}

		Sprite sprite = null;
		if (((BlockItem) stack.getItem()).getBlock() instanceof AbstractSignBlock signBlock) {
			sprite = client.getAtlasManager().getSprite(TexturedRenderLayers.getSignTextureId(signBlock.getWoodType()));
		}

		return Optional.of(new SignTooltipComponent(lines, signText.getColor(), signText.isGlowing(), sprite));
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return this.text.length * 10 + PADDING;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return Math.max(this.textWidth, 94) + PADDING;
	}

	@Override
	public void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
		int signColor = this.color.getSignColor() | 0xFF000000;

		if (this.sprite != null) {
			int left = x - PADDING / 2;
			int top = y - PADDING / 2;
			context.drawTexturedQuad(this.sprite.getAtlasId(),
					left, top, left + this.getWidth(textRenderer), top + this.getHeight(textRenderer),
					this.sprite.getFrameU(FACE_U1), this.sprite.getFrameU(FACE_U2),
					this.sprite.getFrameV(FACE_V1), this.sprite.getFrameV(FACE_V2));
		}

		for (var line : this.text) {
			if (this.tooltipMode == SignTooltipMode.FANCY) {
				int centered = x + (this.getWidth(textRenderer) - textRenderer.getWidth(line)) / 2;
				context.drawText(textRenderer, line, centered, y, signColor, this.glowingText);
			} else {
				context.drawText(textRenderer, line, x, y, signColor, this.glowingText);
			}
			y += 10;
		}
	}
}
