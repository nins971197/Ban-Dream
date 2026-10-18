# Ban Dream 1.2.0

- Removed the misspelled `/bbban comfirm` alias; only `/bbban confirm` remains.
- Added player whitelist commands:
  - `/bbban whitelist add <player|uuid>`
  - `/bbban whitelist remove <player|uuid>`
  - `/bbban whitelist list`
- Whitelisted players bypass item removal, enchantment/effect cleanup, use restrictions and dimension bans.
- World-level entity and structure bans still apply normally.
- `/bbban` and every subcommand require permission level 3.
- Extracted shared command suggestion/validation support.
- Split Ban Stick interaction handling from the general enforcement event class.
- Reworked rule lookup around an immutable indexed rule set.
- Simplified optional JEI integration and removed dead lookup code.
