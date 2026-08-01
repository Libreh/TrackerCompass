package me.libreh.trackercompass.integration;

import me.libreh.trackercompass.TrackerCompass;
import me.libreh.worldreset.api.PlayerResetEvents;

public final class WorldResetIntegration {
    private WorldResetIntegration() {}

    public static void register() {
        PlayerResetEvents.onAfterPlayerReset(TrackerCompass::syncCompass);
    }
}
