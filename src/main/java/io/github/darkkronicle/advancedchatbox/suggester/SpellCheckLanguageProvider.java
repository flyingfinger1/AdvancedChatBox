/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.suggester;

import org.languagetool.Language;

/**
 * A spell-check language supplied by a separate add-on mod.
 *
 * <p>AdvancedChatBox ships only the LanguageTool <em>engine</em> ({@code languagetool-core}); the
 * actual language data (English, German, ...) is large, so each language lives in its own small mod
 * that bundles its {@code language-XX} artifact and registers a provider through the Fabric entrypoint
 * {@value SpellCheckLanguages#ENTRYPOINT}. Box then picks the provider matching the game language.
 *
 * <p>Example {@code fabric.mod.json} in a language add-on:
 * <pre>
 * "entrypoints": { "advancedchatbox:spellcheck": [ "com.example.GermanProvider" ] }
 * </pre>
 */
public interface SpellCheckLanguageProvider {

    /** Language code, matched against the game language, e.g. {@code "de"}, {@code "en"}, {@code "en-US"}. */
    String code();

    /** Human-readable name for logs / future UI, e.g. {@code "Deutsch"}. */
    String displayName();

    /**
     * Creates a fresh LanguageTool {@link Language} instance (e.g. {@code new GermanyGerman()}). Called
     * from Box; the implementing class lives in the add-on, which bundles the matching {@code language-XX}.
     */
    Language createLanguage();
}
