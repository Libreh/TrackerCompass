# TrackerCompass
Feature-rich Manhunt tracker compass mod for Fabric.

## Commands and permissions

| Command                   | Permission                  | Description                       |
|---------------------------|-----------------------------|-----------------------------------|
| /trackercompass           | trackercompass.tracker      | Toggles the tracker compass       |
| /trackercompass <player>  | trackercompass.tracker      | Tracks the named player           |
| /trackercompass reload    | trackercompass.reload (op 4)| Reloads the mod config            |
| /tracker, /compass, /hunt | trackercompass.tracker      | Aliases that toggle or track      |

## Configuration
Config file is found at `config/trackercompass.json`.
```json5
{
  // Ticks between compass direction updates (10 = 0.5s)
  "compassUpdateTicks": 10,
  // Right-click the compass to open a player selection GUI
  "enableTrackerGui": true,
  // Render the action bar tracking line above the hotbar
  "actionBarInfo": true,
  // Only render the action bar while the player is holding the compass
  "hold_compass_for_action_bar": true,
  // Show distance in meters to the tracked target
  "showDistance": true,
  // Show a directional arrow (↑, ↗, →, ↘, ↓, ↙, ←, ↖) pointing to the target
  "showDirectionArrow": true,
  // Show the target's dimension when they are in a different dimension
  "showDimension": true,
  // Also show the dimension label when target and observer share a dimension
  "showSameDimension": false,
  // Give a tracker compass to players automatically when they join
  "giveCompassByDefault": false,
  // Show offline players in the tracker selection GUI
  "show_offline_players": true,
  // Placeholder color for each dimension key
  "dimensionColors": {
    "minecraft:overworld": "<green>",
    "minecraft:the_nether": "<red>",
    "minecraft:the_end": "<light_purple>"
  },
  // Title shown at the top of the tracker GUI
  "guiTitle": "Track Player",
  // Title of each player entry in the tracker GUI
  "playerTitle": "<white>${player}",
  // Lore lines shown under each player entry (one string per line)
  "playerLoreLines": [
    "<red>${offline}",
    "${dimension}",
    "<gray>${distance}m",
    "<yellow>Click to track"
  ],
  // Priority-ordered target resolution rules. The highest-priority enabled
  // condition with a matching player wins, and nearest in-dimension match
  // is chosen. Conditions are tested against each candidate via predicate-api.
  "trackerConditions": [
    {
      // Special id: tracks the player selected through the GUI
      "id": "selected_target",
      // Always-true predicate; swap for tags/gamemode/permissions etc.
      "condition": { "id": "minecraft:true" },
      // id of another condition to try if no candidate matches
      "fallback": null,
      // Action bar text rendered while this condition is the active one
      "actionbarFormat": "${player}<red>${offline} <gray>${distance}m <aqua>${arrow} ${dimension}",
      // Higher wins on tie
      "priority": 100,
      "enabled": true
    }
  ]
}
```

## Placeholders
Available in `actionbarFormat`, `playerTitle`, and `playerLoreLines`:

| Placeholder   | Resolves to                                                       |
|---------------|-------------------------------------------------------------------|
| `${player}`   | Target's player name                                              |
| `${offline}`  | `Offline ` when the target is offline, empty otherwise             |
| `${online}`   | `Online` when the target is online, empty otherwise               |
| `${dimension}`| Target's dimension label, blank when same dim and `showSameDimension` is off |
| `${distance}` | Distance in blocks between observer and target                    |
| `${arrow}`    | Directional arrow (↑, ↗, →, ↘, ↓, ↙, ←, ↖) pointing to the target |

Any placeholder registered by other mods (player stats, scoreboard, etc.) also resolves for the target player.

## Format strings
`actionbarFormat`, `playerTitle`, and `playerLoreLines` are parsed as text-format strings by [Patbox's placeholder-api](https://placeholders.pb4.eu/user/general/). All placeholders and tags documented there are supported, including:

- [Default placeholders](https://placeholders.pb4.eu/user/default-placeholders/) (e.g. `${player_name}`, `${player_gamemode}`)
- [Mod-registered placeholders](https://placeholders.pb4.eu/user/mod-placeholders/) (any mod that registers one)
- [QuickText tags](https://placeholders.pb4.eu/user/quicktext/) (`<red>`, `<hover show_text 'x'>`, etc.)
- [Simplified Text Format tags](https://placeholders.pb4.eu/user/text-format/) (same tags, colon-style args)
