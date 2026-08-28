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
import com.github.reviversmc.advancedtooltips.HiddenEffectMode;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class StatusEffectTooltipComponent implements ConvertibleTooltipData, TooltipComponent {
	private static final Identifier MYSTERY_TEXTURE = Identifier.of(AdvancedTooltips.NAMESPACE, "textures/mob_effects/mystery.png");
	private List<StatusEffectInstance> list = Lists.newArrayList();
	private final FloatList chances = new FloatArrayList();
	private boolean hidden = false;
	private float multiplier;

	public StatusEffectTooltipComponent(List<StatusEffectInstance> list, float multiplier) {
		this.list = list;
		this.multiplier = multiplier;
	}

	public StatusEffectTooltipComponent(List<Pair<StatusEffectInstance, Float>> list) {
		for (var pair : list) {
			this.list.add(pair.getFirst());
			this.chances.add(pair.getSecond().floatValue());
		}
		this.multiplier = 1.f;
	}

	public StatusEffectTooltipComponent() {
		this.hidden = true;
	}

	private Text getHiddenText() {
		var effectsConfig = AdvancedTooltips.getConfig().getEffectsConfig();
		boolean hiddenMotion = effectsConfig.hasHiddenMotion();
		HiddenEffectMode hiddenEffectMode = effectsConfig.getHiddenEffectMode();

		return hiddenEffectMode.stylize(Text.literal(hiddenEffectMode.getText(true, hiddenMotion)), hiddenMotion);
	}

	private Text getHiddenTime() {
		var effectsConfig = AdvancedTooltips.getConfig().getEffectsConfig();
		boolean hiddenMotion = effectsConfig.hasHiddenMotion();
		HiddenEffectMode hiddenEffectMode = effectsConfig.getHiddenEffectMode();

		String timeColon = hiddenEffectMode == HiddenEffectMode.ENCHANTMENT && hiddenMotion ? "i" : ":";

		MutableText minutes = hiddenEffectMode.stylize(Text.literal(hiddenEffectMode.getText(false, hiddenMotion)), hiddenMotion);
		Text seconds = minutes.copy();

		return Text.empty().append(minutes)
				.append(hiddenEffectMode.stylize(Text.literal(timeColon), false))
				.append(seconds);
	}

	@Override
	public TooltipComponent getComponent() {
		return this;
	}

	private static String durationText(StatusEffectInstance instance, float multiplier) {
		return StatusEffectUtil.getDurationText(instance, multiplier, 1.0f).getString();
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		if (this.hidden) {
			return 20;
		}
		return this.list.size() * 20;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		if (this.hidden) {
			return 26 + textRenderer.getWidth(this.getHiddenText());
		}

		int max = 64;
		for (int i = 0; i < list.size(); i++) {
			StatusEffectInstance statusEffectInstance = list.get(i);
			String statusEffectName = I18n.translate(statusEffectInstance.getEffectType().value().getTranslationKey());
			if (statusEffectInstance.getAmplifier() >= 1 && statusEffectInstance.getAmplifier() <= 9) {
				statusEffectName = statusEffectName + ' ' + I18n.translate("enchantment.level." + (statusEffectInstance.getAmplifier() + 1));
			}
			if (statusEffectInstance.getDuration() > 1) {
				String duration = durationText(statusEffectInstance, multiplier);
				if (this.chances.size() > i && this.chances.getFloat(i) < 1f) {
					duration += " - " + (int) (this.chances.getFloat(i) * 100f) + "%";
				}
				max = Math.max(max, 26 + textRenderer.getWidth(duration));
			} else if (this.chances.size() > i && this.chances.getFloat(i) < 1f) {
				String string2 = (int) (this.chances.getFloat(i) * 100f) + "%";
				max = Math.max(max, 26 + textRenderer.getWidth(string2));
			}
			max = Math.max(max, 26 + textRenderer.getWidth(statusEffectName));
		}
		return max;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		if (this.hidden) {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, MYSTERY_TEXTURE, x, y, 0f, 0f, 18, 18, 18, 18);
		} else {
			for (int i = 0; i < list.size(); i++) {
				StatusEffectInstance statusEffectInstance = list.get(i);
				var effectId = Registries.STATUS_EFFECT.getId(statusEffectInstance.getEffectType().value());
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, effectId.withPrefixedPath("mob_effect/"), x, y + i * 20, 18, 18);
			}
		}
	}

	@Override
	public void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
		if (this.hidden) {
			context.drawText(textRenderer, this.getHiddenText(), x + 24, y, 8355711, true);
			context.drawText(textRenderer, this.getHiddenTime(), x + 24, y + 10, 8355711, true);
		} else {
			for (int i = 0; i < list.size(); i++) {
				StatusEffectInstance statusEffectInstance = list.get(i);
				String statusEffectName = I18n.translate(statusEffectInstance.getEffectType().value().getTranslationKey());
				if (statusEffectInstance.getAmplifier() >= 1 && statusEffectInstance.getAmplifier() <= 9) {
					statusEffectName = statusEffectName + ' ' + I18n.translate("enchantment.level." + (statusEffectInstance.getAmplifier() + 1));
				}
				int off = 0;
				if (statusEffectInstance.getDuration() <= 1) {
					off += 5;
				}
				Integer color = statusEffectInstance.getEffectType().value().getCategory().getFormatting().getColorValue();
				context.drawText(textRenderer, statusEffectName, x + 24, y + i * 20 + off, color != null ? color : 16777215, true);
				if (statusEffectInstance.getDuration() > 1) {
					String duration = durationText(statusEffectInstance, multiplier);
					if (this.chances.size() > i && this.chances.getFloat(i) < 1f) {
						duration += " - " + (int) (this.chances.getFloat(i) * 100f) + "%";
					}
					context.drawText(textRenderer, duration, x + 24, y + i * 20 + 10, 8355711, true);
				} else if (this.chances.size() > i && this.chances.getFloat(i) < 1f) {
					String chance = (int) (this.chances.getFloat(i) * 100f) + "%";
					context.drawText(textRenderer, chance, x + 24, y + i * 20 + 10, 8355711, true);
				}
			}
		}
	}
}
