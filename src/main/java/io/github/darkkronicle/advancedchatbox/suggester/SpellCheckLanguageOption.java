/*
 * Copyright (C) 2026 flyingfinger1
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.suggester;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * A single entry in the spell-check language dropdown ({@link fi.dy.masa.malilib.config.options.ConfigOptionList}).
 * The entries are built dynamically from the installed {@link SpellCheckLanguageProvider}s plus a
 * leading "Automatic" entry, so the dropdown grows as the user installs more language add-ons.
 */
@Environment(EnvType.CLIENT)
public class SpellCheckLanguageOption implements IConfigOptionListEntry {

    /** Config/serialisation value of the "follow the game language" entry. */
    public static final String AUTO = "auto";

    /** Translation key for the "Automatic" entry's display name. */
    private static final String AUTO_KEY = "advancedchatbox.config.spellchecker.language.auto";

    private final String code;
    /** Non-null for the AUTO entry: translated at render time so language-load order never matters. */
    private final String translationKey;
    /** Literal display name for provider entries (comes straight from the provider). */
    private final String literalName;

    private SpellCheckLanguageOption(String code, String translationKey, String literalName) {
        this.code = code;
        this.translationKey = translationKey;
        this.literalName = literalName;
    }

    /** The stored value ("auto", "en", "de", ...). */
    public String getCode() {
        return code;
    }

    /** Current entry list: "Automatic" first, then one per installed provider (in registration order). */
    public static List<SpellCheckLanguageOption> entries() {
        List<SpellCheckLanguageOption> list = new ArrayList<>();
        list.add(new SpellCheckLanguageOption(AUTO, AUTO_KEY, null));
        for (SpellCheckLanguageProvider p : SpellCheckLanguages.getProviders()) {
            list.add(new SpellCheckLanguageOption(p.code(), null, p.displayName()));
        }
        return list;
    }

    /** The default/"Automatic" entry, used as the config default. */
    public static SpellCheckLanguageOption auto() {
        return entries().get(0);
    }

    @Override
    public String getStringValue() {
        return code;
    }

    @Override
    public String getDisplayName() {
        // Translate lazily (per render) so it works regardless of when this entry was created relative
        // to language-resource loading. Provider entries use their own literal display name.
        return translationKey != null ? StringUtils.translate(translationKey) : literalName;
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        List<SpellCheckLanguageOption> entries = entries();
        int idx = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).code.equals(code)) {
                idx = i;
                break;
            }
        }
        idx += forward ? 1 : -1;
        if (idx >= entries.size()) {
            idx = 0;
        } else if (idx < 0) {
            idx = entries.size() - 1;
        }
        return entries.get(idx);
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (SpellCheckLanguageOption e : entries()) {
            if (e.code.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return auto();
    }
}
