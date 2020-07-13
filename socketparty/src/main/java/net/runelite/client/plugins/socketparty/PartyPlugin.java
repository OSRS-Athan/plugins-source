package net.runelite.client.plugins.socketparty;

import com.google.inject.Provides;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.socket.SocketPlugin;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketBroadcastPacket;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
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
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private KeyManager keyManager;

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

	@Getter(AccessLevel.PUBLIC)
	private Set<TilePing> pings;

	@Setter(AccessLevel.PRIVATE)
	private boolean hotkeyActive;

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(hotkey);
		pings = Collections.synchronizedSet(new HashSet<>());
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkey);
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		pings.removeIf(tilePing -> tilePing.duration < 0);

		for (TilePing ping: pings) {
			ping.duration--;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!hotkeyActive)
		{
			return;
		}

		Tile selectedSceneTile = client.getSelectedSceneTile();
		if (selectedSceneTile == null)
		{
			return;
		}

		boolean isOnCanvas = false;

		for (MenuEntry menuEntry : client.getMenuEntries())
		{
			if (menuEntry == null)
			{
				continue;
			}

			if ("walk here".equalsIgnoreCase(menuEntry.getOption()))
			{
				isOnCanvas = true;
				break;
			}
		}

		if (!isOnCanvas)
		{
			return;
		}

		event.consume();
		final WorldPoint worldPoint = selectedSceneTile.getWorldLocation();

		JSONObject jsonWorldPoint = new JSONObject();
		jsonWorldPoint.put("x", worldPoint.getX());
		jsonWorldPoint.put("y", worldPoint.getY());
		jsonWorldPoint.put("plane", worldPoint.getPlane());
		jsonWorldPoint.put("outline", config.getTileOutline().getRGB());
		jsonWorldPoint.put("color", config.getTileColor().getRGB());
		jsonWorldPoint.put("transparency", config.getTileTransparency());
		jsonWorldPoint.put("outlineSize", config.getTileOutlineSize());

		JSONObject payload = new JSONObject();
		payload.put("party-ping", jsonWorldPoint);

		eventBus.post(SocketBroadcastPacket.class, new SocketBroadcastPacket(payload));
	}

	@Subscribe
	public void onSocketReceivePacket(SocketReceivePacket event)
	{
		try
		{
			JSONObject payload = event.getPayload();
			if (!payload.has("party-ping"))
			{
				return;
			}

			JSONObject jsonTilePing = payload.getJSONObject("party-ping");

			WorldPoint worldPoint = new WorldPoint(
					jsonTilePing.getInt("x"),
					jsonTilePing.getInt("y"),
					jsonTilePing.getInt("plane"));

			if (worldPoint.getPlane() != client.getPlane() || !WorldPoint.isInScene(client, worldPoint.getX(), worldPoint.getY()))
			{
				return;
			}

			TilePing tilePing = new TilePing(
					worldPoint,
					new Color(jsonTilePing.getInt("outline")),
					new Color(jsonTilePing.getInt("color")),
					jsonTilePing.getInt("transparency"),
					jsonTilePing.getInt("outlineSize"),
					config.getTileDuration());

			pings.add(tilePing);
			clientThread.invoke(() -> client.playSoundEffect(SoundEffectID.SMITH_ANVIL_TINK));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private final HotkeyListener hotkey = new HotkeyListener(() -> config.getHotKey())
	{
		@Override
		public void hotkeyPressed()
		{
			setHotkeyActive(true);
		}

		@Override
		public void hotkeyReleased()
		{
			setHotkeyActive(false);
		}
	};
}
