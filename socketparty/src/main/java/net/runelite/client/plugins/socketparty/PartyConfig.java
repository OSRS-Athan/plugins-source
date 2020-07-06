package net.runelite.client.plugins.socketparty;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Socket Party")
public interface PartyConfig extends Config
{

	@ConfigItem(
		position = 0,
		keyName = "mirrorMode",
		name = "Mirror Mode Compatibility?",
		description = "Should we show the overlay on Mirror Mode?"
	)
	default boolean mirrorMode()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = "getTileColor",
		name = "Tile Color",
		description = "The color of the tiles."
	)
	default Color getTileColor()
	{
		return new Color(0, 0, 0);
	}

	@ConfigItem(
		position = 2,
		keyName = "getTileTransparency",
		name = "Tile Transparency",
		description = "The color transparency of the tiles. Ranges from 0 to 255, inclusive."
	)
	default int getTileTransparency()
	{
		return 50;
	}

	@ConfigItem(
		position = 3,
		keyName = "getTileOutline",
		name = "Tile Outline Color",
		description = "The color of the outline of the tiles."
	)
	default Color getTileOutline()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 4,
		keyName = "getTileOutlineSize",
		name = "Tile Outline Size",
		description = "The size of the outline of the tiles."
	)
	default int getTileOutlineSize()
	{
		return 1;
	}
}
