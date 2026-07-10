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
  "compass_update_ticks": 10,
  // Right-click the compass to open a player selection GUI
  "enable_tracker_gui": true,
  // Render the action bar tracking line above the hotbar
  "action_bar_info": true,
  // Only render the action bar while the player is holding the compass
  "hold_compass_for_action_bar": true,
  // Show distance in meters to the tracked target
  "show_distance": true,
  // Show a directional arrow (↑, ↗, →, ↘, ↓, ↙, ←, ↖) pointing to the target
  "show_direction_arrow": true,
  // Show the target's dimension when they are in a different dimension
  "show_dimension": true,
  // Also show the dimension label when target and observer share a dimension
  "show_same_dimension": false,
  // Give a tracker compass to players automatically when they join
  "give_compass_by_default": false,
  // Show offline players in the tracker selection GUI
  "show_offline_players": true,
  // Placeholder color for each dimension key
  "dimension_colors": {
    "minecraft:overworld": "<green>",
    "minecraft:the_nether": "<red>",
    "minecraft:the_end": "<light_purple>"
  },
  // Priority-ordered target resolution rules. The highest-priority enabled
  // condition with a matching player wins, and nearest in-dimension match
  // is chosen. Conditions are tested against each candidate via predicate-api.
  "tracker_conditions": [
    {
      // Special id: tracks the player selected through the GUI
      "id": "selected_target",
      // Always-true predicate; swap for tags/gamemode/permissions etc.
      "condition": { "type": "always_true" },
      // Action bar text rendered while this condition is the active one
      "actionbar_format": "${player}<red>${offline} <gray>${distance}m <aqua>${arrow} ${dimension}",
      // Higher wins on tie
      "priority": 100,
      "enabled": true
    }
  ],
  // Title shown at the top of the tracker GUI
  "gui_title": "Track Player",
  // Title of each player entry in the tracker GUI
  "player_title": "<white>${player}",
  // Lore lines shown under each player entry (one string per line)
  "player_lore_lines": [
    "<red>${offline}",
    "${dimension}",
    "<gray>${distance}m",
    "<yellow>Click to track"
  ]
}
```

## Placeholders
Available in `actionbar_format`, `player_title`, and `player_lore_lines`:

| Placeholder   | Resolves to                                                       |
|---------------|-------------------------------------------------------------------|
| `${player}`   | Target's player name                                              |
| `${offline}`  | `Offline ` when the target is offline, empty otherwise             |
| `${online}`   | `Online` when the target is online, empty otherwise               |
| `${dimension}`| Target's dimension label, blank when same dim and `showSameDimension` is off |
| `${distance}` | Distance in blocks between observer and target                    |
| `${arrow}`    | Directional arrow (↑, ↗, →, ↘, ↓, ↙, ←, ↖) pointing to the target |

TextPlaceholderAPI placeholders use the `%namespace:name%` syntax (distinct from the `${...}` placeholders above). Plain `%player:...%` placeholders resolve against the observer holding the compass. Every `player:*` placeholder is also mirrored into a `target:*` namespace that resolves against the tracked player, so use `%target:name%`, `%target:health%`, etc. to show the target's data.

## Format strings
`actionbar_format`, `player_title`, and `player_lore_lines` are parsed as text-format strings by [TextPlaceholderAPI](https://placeholders.pb4.eu/user/general/). All placeholders and tags documented there are supported, including:

- [Default placeholders](https://placeholders.pb4.eu/user/default-placeholders/) (e.g. `%player:name%`, `%player:displayname%`, or `%target:name%` for the tracked player)
- [Mod-registered placeholders](https://placeholders.pb4.eu/user/mod-placeholders/) (any mod that registers one)
- [QuickText tags](https://placeholders.pb4.eu/user/quicktext/) (`<red>`, `<hover show_text 'x'>`, etc.)
- [Simplified Text Format tags](https://placeholders.pb4.eu/user/text-format/) (same tags, colon-style args)
