package net.runelite.client.plugins.socketparty;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class PartyOverlay extends Overlay
{

	private final Client client;
	private final PartyPlugin plugin;
	private final PartyConfig config;

	@Inject
	private PartyOverlay(Client client, PartyPlugin plugin, PartyConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		determineLayer();
	}

	private void determineLayer()
	{
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (final TilePing next : plugin.getPings())
		{
			final LocalPoint localPoint = LocalPoint.fromWorld(client, next.getWorldPoint());
			if (localPoint != null)
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
				if (poly == null)
				{
					continue;
				}

				Color color = next.getOutline();
				graphics.setColor(color);

				Stroke originalStroke = graphics.getStroke();
				int strokeSize = Math.max(next.getOutlineSize(), 1);
				graphics.setStroke(new BasicStroke(strokeSize));
				graphics.draw(poly);

				Color fill = next.getColor();
				int alpha = Math.min(Math.max(next.getTransparency(), 0), 255);
				Color realFill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), alpha);
				graphics.setColor(realFill);
				graphics.fill(poly);

				graphics.setStroke(originalStroke);
			}
		}

		return null;
	}
}
