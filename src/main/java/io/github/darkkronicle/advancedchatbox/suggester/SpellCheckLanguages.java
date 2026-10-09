/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.suggester;

import io.github.darkkronicle.advancedchatbox.AdvancedChatBox;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the {@link SpellCheckLanguageProvider}s registered by language add-on mods and picks the
 * one that fits the current game language. See {@link SpellCheckLanguageProvider}.
 */
@Environment(EnvType.CLIENT)
public final class SpellCheckLanguages {

    /** Fabric entrypoint key that language add-ons register under. */
    public static final String ENTRYPOINT = "advancedchatbox:spellcheck";

    private static List<SpellCheckLanguageProvider> providers = null;

    private SpellCheckLanguages() {}

    /** All providers registered via the entrypoint (collected once, lazily). */
    public static List<SpellCheckLanguageProvider> getProviders() {
        if (providers == null) {
            List<SpellCheckLanguageProvider> list = new ArrayList<>();
            for (EntrypointContainer<SpellCheckLanguageProvider> container :
                    FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, SpellCheckLanguageProvider.class)) {
                try {
                    list.add(container.getEntrypoint());
                } catch (Throwable t) {
                    AdvancedChatBox.LOGGER.error("Failed to load a spell-check language provider from {}",
                            container.getProvider().getMetadata().getId(), t);
                }
            }
            providers = list;
        }
        return providers;
    }

    /**
     * Picks the provider whose {@link SpellCheckLanguageProvider#code()} matches the current game
     * language (e.g. game "de_de" → code "de"); falls back to the first installed provider, or
     * {@code null} if no language add-on is installed.
     */
    public static SpellCheckLanguageProvider pickForCurrentLocale() {
        List<SpellCheckLanguageProvider> provs = getProviders();
        if (provs.isEmpty()) {
            return null;
        }
        String gameLang = "en";
        try {
            String selected = Minecraft.getInstance().getLanguageManager().getSelected();
            if (selected != null && !selected.isEmpty()) {
                gameLang = selected.split("_")[0].toLowerCase();
            }
        } catch (Throwable ignored) {
            // Fall back to the first provider below.
        }
        for (SpellCheckLanguageProvider p : provs) {
            String code = p.code() == null ? "" : p.code().toLowerCase();
            if (code.equals(gameLang) || code.startsWith(gameLang + "-") || code.startsWith(gameLang + "_")) {
                return p;
            }
        }
        return provs.get(0);
    }
}
