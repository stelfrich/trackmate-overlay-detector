package de.fzj.jungle.trackmate.visualization.hyperstack;

import ij.ImagePlus;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;

/**
 * A custom {@link SpotOverlay} implementation to properly support the
 * {@link Roi}s in an overlay.
 * 
 * @author Stefan Helfrich
 */
@SuppressWarnings( "serial" )
public class JungleSpotOverlay extends SpotOverlay
{

	public JungleSpotOverlay( Model model, ImagePlus imp, Map< String, Object > displaySettings )
	{
		super( model, imp, displaySettings );
	}

	public void setEditingSpot( Spot spot )
	{
		this.editingSpot = spot;
	}

	public Spot getEditingSpot()
	{
		return this.editingSpot;
	}
}
