/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.suggester;

import com.mojang.brigadier.context.StringRange;
import io.github.darkkronicle.advancedchatbox.AdvancedChatBox;
import io.github.darkkronicle.advancedchatbox.chat.AdvancedSuggestion;
import io.github.darkkronicle.advancedchatbox.chat.AdvancedSuggestions;
import io.github.darkkronicle.advancedchatbox.config.ChatBoxConfigStorage;
import io.github.darkkronicle.advancedchatbox.interfaces.IMessageSuggestor;
import io.github.darkkronicle.advancedchatcore.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.ResultCache;
import org.languagetool.UserConfig;
import org.languagetool.rules.RuleMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class SpellCheckSuggestor implements IMessageSuggestor {
    private final JLanguageTool lt;

    private static final SpellCheckSuggestor INSTANCE = new SpellCheckSuggestor();

    public static SpellCheckSuggestor getInstance() {
        return INSTANCE;
    }

    private SpellCheckSuggestor() {
        // LanguageTool's bundled grammar.xml is large and trips the JDK's hardened JAXP entity
        // limits on modern Java (Java 25 enforces jdk.xml.totalEntitySizeLimit=100000, which the
        // grammar slightly exceeds). These XML files ship inside the mod and are trusted, so lift
        // the limits before LanguageTool parses them. Without this, rule activation throws and the
        // whole spell-check suggestor fails to load.
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.entityExpansionLimit", "0");

        // The language data (English, German, ...) lives in separate add-on mods that register a
        // SpellCheckLanguageProvider; Box only ships the engine. Pick the one matching the game language.
        JLanguageTool tool = null;
        SpellCheckLanguageProvider provider = SpellCheckLanguages.pickForCurrentLocale();
        if (provider != null) {
            try {
                Language language = provider.createLanguage();
                // null motherTongue: this skips LanguageTool's false-friend rules, which require OTHER
                // language modules to be registered (their handler hard-codes loading the "en-US"
                // message bundle) and otherwise crash a single-language setup — e.g. German alone threw
                // "'en-US' is not a language code known to LanguageTool". Chat spell-check doesn't need
                // cross-language false-friend hints anyway.
                tool = new JLanguageTool(language, null, new ResultCache(15),
                        new UserConfig(new ArrayList<>(), new HashMap<>(), 20));
                tool.setMaxErrorsPerWordRate(0.33f);
                // Set it up. Make it so it doesn't freeze later.
                tool.check("a");
                AdvancedChatBox.LOGGER.info("Spell-check language: {} ({})", provider.displayName(), provider.code());
            } catch (Exception e) {
                AdvancedChatBox.LOGGER.error("Failed to initialise spell-check for language {}", provider.code(), e);
                tool = null;
            }
        } else {
            AdvancedChatBox.LOGGER.info("No spell-check language add-on installed; spell-check is disabled.");
        }
        lt = tool;
    }

    @Override
    public Optional<List<AdvancedSuggestions>> suggest(String text) {
        if (lt == null) {
            // No language add-on installed, or the engine failed to start.
            return Optional.empty();
        }
        ArrayList<AdvancedSuggestions> suggestions = new ArrayList<>();
        try {
            List<RuleMatch> matches = lt.check(text);
            for (RuleMatch match : matches) {
                int fromPos = match.getFromPos();
                int toPos = match.getToPos();
                StringRange range = new StringRange(fromPos, toPos);
                String original = text.substring(fromPos, toPos);
                suggestions.add(new AdvancedSuggestions(range, convertSuggestions(match, range, original)));
            }
        } catch (Exception e) {
            AdvancedChatBox.LOGGER.error("Failed to run spell check on text", e);
            return Optional.empty();
        }
        return Optional.of(suggestions);
    }

    private static List<AdvancedSuggestion> convertSuggestions(RuleMatch match, StringRange range, String original) {
        // Rank LanguageTool's replacements by edit distance to the typed word (closest first), so the
        // most likely correction surfaces at the top of the dropdown and survives the display limit.
        // The distance is passed as the suggestion's sort priority, which is the PRIMARY key in
        // AdvancedSuggestion.compareTo; this way the ordering survives both the alphabetical sort in the
        // AdvancedSuggestions constructor and the one in ChatSuggestor.orderSuggestions. Equal distances
        // fall back to alphabetical.
        String originalLower = original.toLowerCase();
        List<AdvancedSuggestion> replacements = new ArrayList<>();
        for (String s : match.getSuggestedReplacements()) {
            int distance = levenshtein(originalLower, s.toLowerCase());
            replacements.add(new AdvancedSuggestion(range, s, new RawText(s, Style.EMPTY),
                    getHover(match.getMessage()), distance));
        }
        return replacements;
    }

    /** Levenshtein edit distance between two strings. */
    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    private static Component getHover(String message) {
        String text = ChatBoxConfigStorage.SpellChecker.HOVER_TEXT.config.getStringValue();
        text = text.replaceAll("&", "§");
        Optional<StringMatch> match = SearchUtils.getMatch(message, "<suggestion>(.+)</suggestion>", FindType.REGEX);
        if (match.isEmpty()) {
            text = text.replaceAll("\\$1", message).replaceAll("\\$2", "").replaceAll("\\$3", "");
            return StyleFormatter.formatText(Component.literal(text));
        }
        StringMatch stringMatch = match.get();
        String start = message.substring(0, stringMatch.start);
        String end = message.substring(stringMatch.end);
        String middle = message.substring(stringMatch.start + 12, stringMatch.end - 13);
        text = text.replaceAll("\\$1", start).replaceAll("\\$2", middle).replaceAll("\\$3", end);
        return StyleFormatter.formatText(Component.literal(text));
    }
}
