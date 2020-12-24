package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Objects;

public class StopBoardingArea extends Stop {
    private final StopPlatform platform;

    public StopBoardingArea(Stop base, StopPlatform platform) {
super(base);
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public Stop getMostGenericStop() {
        return platform.getMostGenericStop();
    }

    public StopPlatform getPlatform() {
        return platform;
    }
}
