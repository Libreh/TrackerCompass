# TrackerCompass
Simple yet powerful tracker compass mod for Fabric

## Commands and permissions
| Command                   | Permission                                | Description                            |
|---------------------------|-------------------------------------------|----------------------------------------|
| /trackercompass           | trackercompass.commands.main (default)    | Sends information about trackercompass |
| /trackercompass reload    | trackercompass.commands.reload (op 4)     | Reloads mods config                    |
| /tracker, /compass, /hunt | trackercompass.commands.tracker (default) | Toggles compass item                   |
## Configuration
Config file is found at `config/trackercompass.json`.
```json5
{
  "compassUpdateTicks": 10,
  "enableTrackerGui": true,
  "actionBarInfo": true,
  "onlyShowWhenHoldingCompass": true,
  "showDistance": true,
  "showDirectionArrow": true,
  "showStatusIndicators": true,
  "giveCompassByDefault": false,
  "showOfflinePlayersInGui": true
}
```