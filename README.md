# TrackerCompass
Feature-rich Manhunt tracker compass mod for Fabric.

## Commands and permissions

| Command                   | Permission                                | Description          |
|---------------------------|-------------------------------------------|----------------------|
| /trackercompass           | trackercompass.commands.main (default)    | About info           |
| /trackercompass reload    | trackercompass.commands.reload (op 4)     | Reloads mods config  |
| /tracker, /compass, /hunt | trackercompass.commands.tracker (default) | Toggles compass item |

## Configuration
Config file is found at `config/trackercompass.json`.
```json5
{
  // How often (in ticks) the compass updates tracking direction (10 ticks = 0.5 seconds)
  "compassUpdateTicks": 10,
  // Enable right-click GUI menu to select which player to track
  "enableTrackerGui": true,
  // Display tracking information in the action bar above hotbar
  "actionBarInfo": true,
  // Only show action bar info when holding the compass
  "onlyShowWhenHoldingCompass": true,
  // Show distance in meters to the tracked player
  "showDistance": true,
  // Show directional arrow (↑, →, ↓, ←, etc.) pointing to target
  "showDirectionArrow": true,
  // Show status indicators like (Portal) or (Offline) after player name
  "showStatusIndicators": true,
  // Automatically give compass to players when they join the server
  "giveCompassByDefault": false,
  // Show offline players in the tracker GUI selection menu
  "showOfflinePlayersInGui": true
}
```
