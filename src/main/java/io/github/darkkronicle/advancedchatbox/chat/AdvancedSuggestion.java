/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.chat;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import io.github.darkkronicle.advancedchatcore.util.RawText;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

/**
 * Suggestion that contains render text, suggested text, suggested start/stop, and tooltip.
 */
@Environment(EnvType.CLIENT)
public class AdvancedSuggestion extends Suggestion {
    @Nonnull
    @Getter
    private final Component render;

    /**
     * Primary sort key for the dropdown. Lower comes first; equal priorities fall back to the usual
     * alphabetical comparison. Normal suggestions all use 0 (so they stay alphabetical); spell-check
     * sets it to the edit distance from the typed word, so the closest corrections surface at the top
     * and survive both the {@link AdvancedSuggestions} and {@link ChatSuggestor} sorts.
     */
    @Getter
    private final int sortPriority;

    /**
     * @param range   Range from the original string where it is recommending
     * @param text    Suggested text to use
     * @param render  How the suggestion will render
     * @param tooltip Message to show up on hover
     */
    public AdvancedSuggestion(StringRange range, String text, Component render, Message tooltip) {
        this(range, text, render, tooltip, 0);
    }

    /**
     * @param sortPriority Primary sort key (lower first); see {@link #sortPriority}.
     */
    public AdvancedSuggestion(StringRange range, String text, Component render, Message tooltip, int sortPriority) {
        super(range, text, tooltip);
        if (render == null) {
            this.render = new RawText(text, Style.EMPTY);
        } else {
            this.render = render;
        }
        this.sortPriority = sortPriority;
    }

    public AdvancedSuggestion(StringRange range, String text) {
        this(range, text, null, null);
    }

    @Override
    public int compareTo(final Suggestion o) {
        if (o instanceof AdvancedSuggestion other) {
            if (sortPriority != other.sortPriority) {
                return Integer.compare(sortPriority, other.sortPriority);
            }
            return render.getString().compareTo(other.getRender().getString());
        }
        return render.getString().compareTo(o.getText());
    }

    @Override
    public int compareToIgnoreCase(final Suggestion o) {
        if (o instanceof AdvancedSuggestion other) {
            if (sortPriority != other.sortPriority) {
                return Integer.compare(sortPriority, other.sortPriority);
            }
            return render.getString().compareToIgnoreCase(other.getRender().getString());
        }
        return render.getString().compareToIgnoreCase(o.getText());
    }

    /**
     * Create's an {@link AdvancedSuggestion} from an {@link Suggestion}
     *
     * @param suggestion Suggestion to convert
     * @return New objeect
     */
    public static AdvancedSuggestion fromSuggestion(Suggestion suggestion) {
        return new AdvancedSuggestion(suggestion.getRange(), suggestion.getText(), null, suggestion.getTooltip());
    }
}
