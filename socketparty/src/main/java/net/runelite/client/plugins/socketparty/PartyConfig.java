package net.runelite.client.plugins.socketparty;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Socket Party")
public interface PartyConfig extends Config
{
	@ConfigItem(
		position = 1,
		keyName = "getTileColor",
		name = "Ping Tile Color",
		description = "The color of your ping tile."
	)
	default Color getTileColor()
	{
		return new Color(0, 0, 0);
	}

	@ConfigItem(
		position = 2,
		keyName = "getTileTransparency",
		name = "Ping Tile Transparency",
		description = "The color transparency of your ping tile. Ranges from 0 to 255, inclusive."
	)
	default int getTileTransparency()
	{
		return 50;
	}

	@ConfigItem(
		position = 3,
		keyName = "getTileOutline",
		name = "Ping Tile Outline Color",
		description = "The color of the outline of your ping tile."
	)
	default Color getTileOutline()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 4,
		keyName = "getTileOutlineSize",
		name = "Ping Tile Outline Size",
		description = "The size of the outline of your ping tile."
	)
	default int getTileOutlineSize()
	{
		return 1;
	}

	@ConfigItem(
			position = 5,
			keyName = "getTileDuration",
			name = "Ping Tile Duration",
			description = "The duration of pings in game ticks"
	)
	default int getTileDuration()
	{
		return 6;
	}
}
