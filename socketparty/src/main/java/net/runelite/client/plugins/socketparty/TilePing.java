package net.runelite.client.plugins.socketparty;

import lombok.Value;
import lombok.experimental.NonFinal;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

@Value
public class TilePing {
    private final WorldPoint worldPoint;
    private final Color outline;
    private final Color color;
    private final Integer transparency;
    private final Integer outlineSize;
    @NonFinal
    public Integer duration;
}
