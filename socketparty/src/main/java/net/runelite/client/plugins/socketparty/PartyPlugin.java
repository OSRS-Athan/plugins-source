package net.runelite.client.plugins.socketparty;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.socket.SocketPlugin;
import net.runelite.client.plugins.socket.org.json.JSONArray;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketBroadcastPacket;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Socket Party",
	description = "Party plugin using sockets",
	tags = {"socket", "server", "connection", "broadcast", "party"},
	enabledByDefault = false,
	type = PluginType.PVM
)
@PluginDependency(SocketPlugin.class)
public class PartyPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private SocketPlugin socketPlugin;

	@Inject
	private PartyConfig config;

	@Provides
	PartyConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PartyConfig.class);
	}

	@Inject
	private PartyOverlay overlay;

	// This boolean states whether or not the room is currently active.
	@Getter(AccessLevel.PUBLIC)
	private boolean sotetsegActive;

	// This NPC represents the boss.
	private NPC sotetsegNPC;

	// This represents the bad tiles.
	private LinkedHashSet<Point> redTiles;

	@Getter(AccessLevel.PUBLIC)
	private Set<WorldPoint> mazePings;

	// This represents the amount of times to send data.
	private int dispatchCount;

	// This represents the state of the raid.
	private boolean wasInUnderworld;
	private int overworldRegionID;

	@Override
	protected void startUp()
	{
		sotetsegActive = false;
		sotetsegNPC = null;

		redTiles = new LinkedHashSet<>();
		mazePings = Collections.synchronizedSet(new HashSet<>());

		dispatchCount = 5;
		wasInUnderworld = false;
		overworldRegionID = -1;

		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Subscribe // Boss has entered the scene. Played has entered the room.
	public void onNpcSpawned(NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		switch (npc.getId())
		{
			case 8387:
			case 8388:
				sotetsegActive = true;
				sotetsegNPC = npc;
				break;
			default:
				break;
		}
	}

	@Subscribe // Boss has left the scene. Player left, died, or the boss was killed.
	public void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();
		switch (npc.getId())
		{
			case 8387:
			case 8388:
				if (client.getPlane() != 3)
				{
					sotetsegActive = false;
					sotetsegNPC = null;
				}

				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (sotetsegActive)
		{
			final Player player = client.getLocalPlayer();

			// This check resets all the data if sotetseg is attackable.
			if (sotetsegNPC != null && sotetsegNPC.getId() == 8388)
			{
				redTiles.clear();
				mazePings.clear();
				dispatchCount = 5;

				if (isInOverWorld())
				{ // Set the overworld flags.
					wasInUnderworld = false;
					if (player != null && player.getWorldLocation() != null)
					{
						WorldPoint wp = player.getWorldLocation();
						overworldRegionID = wp.getRegionID();
					}
				}
			}

			if (!redTiles.isEmpty() && wasInUnderworld)
			{
				if (dispatchCount > 0)
				{ // Ensure we only send the data a couple times.
					dispatchCount--;

					if (pluginManager.isPluginEnabled(socketPlugin))
					{
						JSONArray data = new JSONArray();

						for (final Point p : redTiles)
						{
							WorldPoint wp = translateMazePoint(p);

							JSONObject jsonwp = new JSONObject();
							jsonwp.put("x", wp.getX());
							jsonwp.put("y", wp.getY());
							jsonwp.put("plane", wp.getPlane());

							data.put(jsonwp);
						}

						JSONObject payload = new JSONObject();
						payload.put("sotetseg-extended", data);

						eventBus.post(SocketBroadcastPacket.class, new SocketBroadcastPacket(payload));
					}
				}
			}
		}
	}

	@Subscribe
	public void onSocketReceivePacket(SocketReceivePacket event)
	{
		try
		{
			JSONObject payload = event.getPayload();
			if (!payload.has("sotetseg-extended"))
			{
				return;
			}

			mazePings.clear();

			JSONArray data = payload.getJSONArray("sotetseg-extended");
			for (int i = 0; i < data.length(); i++)
			{
				JSONObject jsonwp = data.getJSONObject(i);
				int x = jsonwp.getInt("x");
				int y = jsonwp.getInt("y");
				int plane = jsonwp.getInt("plane");

				WorldPoint wp = new WorldPoint(x, y, plane);
				mazePings.add(wp);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (sotetsegActive)
		{
			GroundObject o = event.getGroundObject();
			if (o.getId() == 33035)
			{
				final Tile t = event.getTile();
				final WorldPoint p = WorldPoint.fromLocal(client, t.getLocalLocation());
				final Point point = new Point(p.getRegionX(), p.getRegionY());

				if (this
					.isInOverWorld())
				{  // (9, 22) are magical numbers that represent the overworld maze offset.
					redTiles.add(new Point(point.getX() - 9, point.getY() - 22));
				}

				if (this
					.isInUnderWorld())
				{   // (42, 31) are magical numbers that represent the underworld maze offset.
					redTiles.add(new Point(point.getX() - 42, point.getY() - 31));
					wasInUnderworld = true;
				}
			}
		}
	}

	/**
	 * Returns whether or not the current player is in the overworld.
	 *
	 * @return In the overworld?
	 */
	private boolean isInOverWorld()
	{
		return client.getMapRegions().length > 0 && client.getMapRegions()[0] == 13123;
	}

	/**
	 * Returns whether or not the current player is in the underworld.
	 *
	 * @return In the underworld?
	 */
	private boolean isInUnderWorld()
	{
		return client.getMapRegions().length > 0 && client.getMapRegions()[0] == 13379;
	}

	/**
	 * Translates a maze point to a WorldPoint.
	 *
	 * @param mazePoint Point on the maze.
	 * @return WorldPoint
	 */
	private WorldPoint translateMazePoint(final Point mazePoint)
	{
		Player p = client.getLocalPlayer();

		// (9, 22) are magical numbers that represent the overworld maze offset.
		if (overworldRegionID == -1 && p != null)
		{
			WorldPoint wp = p.getWorldLocation();
			return WorldPoint
				.fromRegion(wp.getRegionID(), mazePoint.getX() + 9, mazePoint.getY() + 22, 0);
		}

		return WorldPoint
			.fromRegion(overworldRegionID, mazePoint.getX() + 9, mazePoint.getY() + 22, 0);
	}
}
