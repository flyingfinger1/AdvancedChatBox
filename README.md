# AdvancedChatBox

AdvancedChatBox allows for complex suggestions and tab completion to be added into the Minecraft chat box.

> **Refurbished fork.** DarkKronicle archived the original project. This fork brings AdvancedChatBox
> up to **Minecraft 26.1** and modernises the codebase: the whole mod was ported from Yarn to the new
> Mojang names and the 26.x GUI render-state model, and the LanguageTool spell-checker was made
> robust on Java 25 (graceful degradation + JAXP/dependency fixes). It is a module of, and builds
> against, the refurbished
> [AdvancedChatCore](https://github.com/flyingfinger1/AdvancedChatCore).

## Requirements

| | Version |
| --- | --- |
| Minecraft | **26.1** |
| Java | **25** (required by Minecraft 26.x) |
| Fabric Loader | 0.19.0+ |

## Dependencies

The following are **required** for this mod to run:

- [AdvancedChatCore](https://github.com/flyingfinger1/AdvancedChatCore) **1.6.0+** (this fork's build)
- [MaLiLib](https://modrinth.com/mod/malilib) — for 26.x use the sakura-ryoko builds
- [Fabric API](https://modrinth.com/mod/fabric-api)

[Mod Menu](https://modrinth.com/mod/modmenu) is recommended to open the configuration screen.

### Spell-check languages

The spell-checker's **engine** ships with AdvancedChatBox, but each **language's data** is large and
lives in its own small add-on mod. Install the add-on for the language(s) you want; the spell-checker
picks the one matching your Minecraft language automatically:

- [AdvancedChatBox Language: English](https://github.com/flyingfinger1/AdvancedChatBoxLangEN)
- [AdvancedChatBox Language: German](https://github.com/flyingfinger1/AdvancedChatBoxLangDE)

By default the spell-checker follows your Minecraft language, but you can pick a specific language
under **Spell Checker → Spell-check Language** in the config (handy when you have more than one
language add-on installed). The change takes effect without a restart.

Without a language add-on installed, the rest of the mod works normally and spell-check is simply
disabled. Add-ons require this version of AdvancedChatBox or newer.

## Features

- Spelling Checker powered by [LanguageTool](https://languagetool.org) — see **Spell-check languages** below
- In-Chat Calculator
- Toggle player tab suggestions
- Add custom chat suggestions in the form of shortcuts
- Change the color of the suggestors
- Custom command highlighting
- Built-in JSON linter for commands
- Legacy color code formatting when typing messages

## Building

The build needs a **JDK 25** toolchain (Minecraft 26.x). AdvancedChatBox depends on the refurbished
AdvancedChatCore, which it resolves from your local Maven repository. Publish Core locally first:

```
# in the AdvancedChatCore clone
./gradlew publishToMavenLocal      # publishes io.github.darkkronicle:AdvancedChatCore:1.6.0

# then in AdvancedChatBox
./gradlew build
```

The output jar in `build/libs/` bundles the LanguageTool **engine** (jar-in-jar) but not the
per-language data, which lives in the separate language add-ons (see **Spell-check languages**), so
it is ~8 MB. To run the mod, install it together with AdvancedChatCore, MaLiLib and Fabric API, plus
a language add-on if you want spell-checking.

## Development

To ensure code consistency the hook `pre-commit.sh` can be used. To install it run:

`ln -s ../../pre-commit.sh .git/hooks/pre-commit`

## Credits n' more

- Code & Mastermind: DarkKronicle
- Language & Proofreading: Chronos22
- 26.2 port & modernisation: community fork
