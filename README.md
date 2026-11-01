# FailShop

GUI shop for Spigot/Paper 1.20.6+ (Java 21). Requires **Vault** and an economy plugin (e.g. EssentialsX).

## Features
- Fully clickable 6-row GUI shop with categories, paging and a close/back navigation
- Buy and sell with Vault (EssentialsX or any other economy plugin)
- 11 ready-made categories (blocks, ores, decor, gems, nether, redstone, farming, food, drops, fuel, misc) in `price.yml`
- Fully configurable items: custom names, lore, per-item buy/sell price, buy-only or sell-only items
- Main menu categories shown as player heads by nickname (global default + per-category nick); items can use heads too (nick, base64, texture URL or hash)
- All GUI texts in CAPITAL LETTERS (or small capitals ᴛɪᴍᴛᴏ), configurable style
- Command items: run console commands on purchase (`{player}` placeholder), e.g. XP, ranks, kits
- Dynamic pricing: buying raises the price, selling lowers it, slowly recovers over time (min/max limits)
- Daily deals: rotating discounted items with configurable count, discount range and duration
- `/sell hand`, `/sell all`, optional "sell all" button in the menu, `/worth` price check
- Permission-based sell bonus (`failshop.sellmultiplier.<number>`)
- 8 languages (`en`, `cs`, `sk`, `de`, `pl`, `es`, `fr`, `ru`; add your own `lang/xx.yml`), per-language names in `price.yml`
- Transaction log (`transactions.log`), sounds on success/error, configurable currency symbol
- Auto-update checker (Spigot/Spiget) with automatic download to `plugins/update/`
- Tab completion, safe inventory handling (items that don't fit are dropped, items with NBT/meta are never sold)

## Build
Locally: `mvn package` -> `target/FailShop.jar` (Java 21).

GitHub Actions (`.github/workflows/build.yml`) builds on every push / pull request and uploads the jar as an artifact
(Actions -> run -> Artifacts). Pushing a tag like `v1.0.0` also creates a GitHub Release with `FailShop.jar` attached:
```
git tag v1.0.0 && git push origin v1.0.0
```

## Setup
- `config.yml` -> `language: en`, `cs`, `sk`, `de`, `pl`, `es`, `fr` or `ru` (files in `plugins/FailShop/lang/`, add your own `xx.yml`)
- `price.yml` -> categories and prices, one line per block: `DIAMOND: {buy: 100, sell: 12}` (the old `shops.yml` is copied automatically)
- Main menu categories are shown as player heads: `gui.category-head: "Nick"` in `config.yml` (default for all), `head: Nick` on a category in `price.yml` (per category). Items inside categories can use heads too (`gui.item-head` / `head:`), empty by default
- All GUI texts are in CAPITAL LETTERS by default, switch with `gui.text-style: uppercase | small-caps (ᴛɪᴍᴛᴏ) | normal`
- `/shopadmin reload` applies changes

## Commands
- `/shop [category|deals]`, `/sell hand|all`, `/worth`, `/shopadmin reload|refresh-deals|reset-prices`

## Permissions
`failshop.use`, `failshop.sell`, `failshop.worth` (default true), `failshop.admin` (op),
`failshop.sellmultiplier.<number>` (e.g. `failshop.sellmultiplier.1.5` = +50 % on selling)

## Clicks
Left = buy 1, Shift+Left = buy a stack, Right = sell 1, Shift+Right = sell all of that item.

## Auto-update
Set `update-checker.resource-id` to your Spigot resource ID. New versions are downloaded to `plugins/update/`
and installed on the next restart. Upload the plain `.jar` (not a zip) as the resource file.
