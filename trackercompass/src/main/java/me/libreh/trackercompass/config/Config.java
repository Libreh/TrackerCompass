package me.libreh.trackercompass.config;

import com.google.gson.annotations.SerializedName;
import eu.pb4.predicate.api.BuiltinPredicates;
import me.libreh.trackercompass.api.TrackerCondition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public int compassUpdateTicks = 10;
    public boolean enableTrackerGui = true;
    public boolean actionBarInfo = true;
    @SerializedName(value = "hold_compass_for_action_bar", alternate = {"only_show_when_holding_compass"})
    public boolean holdCompassForActionBar = true;
    public boolean showDistance = true;
    public boolean showDirectionArrow = true;
    public boolean showDimension = true;
    public boolean showSameDimension = false;
    public boolean giveCompassByDefault = false;
    @SerializedName(value = "show_offline_players", alternate = {"show_offline_players_in_gui"})
    public boolean showOfflinePlayers = true;
    public Map<String, String> dimensionColors = new LinkedHashMap<>();
    public List<TrackerCondition> trackerConditions = new ArrayList<>();
    public String guiTitle = "Track Player";
    public String playerTitle = "<white>${player}";
    public List<String> playerLoreLines = new ArrayList<>();

    public Config() {
        dimensionColors.put("minecraft:overworld", "<green>");
        dimensionColors.put("minecraft:the_nether", "<red>");
        dimensionColors.put("minecraft:the_end", "<light_purple>");

        playerLoreLines.add("<red>${offline}");
        playerLoreLines.add("${dimension}");
        playerLoreLines.add("<gray>${distance}m");
        playerLoreLines.add("<yellow>Click to track");

        trackerConditions.add(new TrackerCondition(
            TrackerCondition.SELECTED_TARGET_ID,
            BuiltinPredicates.alwaysTrue(),
            null,
            "${player}<red>${offline} <gray>${distance}m <aqua>${arrow} ${dimension}",
            100,
            true
        ));
    }
}
