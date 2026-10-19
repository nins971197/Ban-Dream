# Ban Dream 1.3.0

- Added block bans with global or per-dimension scope.
- Added `interact` and `full` block ban modes.
- Added `/bbban ban block`.
- Ban Stick left-click now selects blocks for confirmation.
- Full block bans cancel interaction, breaking, and entity placement.
- Interact block bans allow placement and breaking while blocking right-click use.
- Full block bans remove the block on left-click or right-click.
- Banned blocks reject hopper and Forge-capability item input/output in both modes.
- Banned containers and pipes no longer exchange items after rules change.
- Renamed all user-facing mod titles to Ban Dream.
- Removed Java logging calls and explanatory comments from packaged sources.
- Moved architecture and build notes into the workspace `docs` directory.
- Added Forge 1.12.2, Cleanroom 1.12.2, NeoForge 1.20.1, and NeoForge 1.21.1 workspaces.
