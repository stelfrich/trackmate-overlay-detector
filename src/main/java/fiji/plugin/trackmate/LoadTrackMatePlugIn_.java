package fiji.plugin.trackmate;

import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.CRIMSON_FLUORESCENCE_TOTAL;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.PIXELS;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.YFP_FLUORESCENCE_TOTAL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.fzj.jungle.segmentation.Cell;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

/**
 * Custom implementation of {@link LoadTrackMatePlugIn_} to re-establish the
 * connection between {@link Roi}s in an overlay and TrackMate's {@link Spot}s.
 * 
 * @author Stefan Helfrich
 */
public class LoadTrackMatePlugIn_ extends fiji.plugin.trackmate.LoadTrackMatePlugIn_
{

	public LoadTrackMatePlugIn_()
	{
		super();
	}

	/*
	 * HOOKS
	 */

	/**
	 * Hook for subclassers:<br>
	 * The {@link TrackMate} object is loaded and properly configured. This
	 * method is called just before the controller and GUI are launched.
	 *
	 * @param trackmate
	 *            the {@link TrackMate} instance that was fledged after loading.
	 */
	@Override
	protected void postRead( final TrackMate trackmate )
	{
		ImagePlus imp = trackmate.getSettings().imp;

		if ( imp != null )
		{
			ImageProcessor ip = imp.getProcessor();
			Calibration calibration = trackmate.getSettings().imp.getCalibration();

			// Convert Spots to Cells
			SpotCollection spots = trackmate.getModel().getSpots();

			Set< Spot > spotsFromRois = new HashSet<>();
			Map< Spot, Cell > mapping = new HashMap<>();

			for ( Spot s : spots.iterable( true ) )
			{
				double t = s.getFeature( Spot.FRAME );
				double x = s.getFeature( Spot.POSITION_X );
				double y = s.getFeature( Spot.POSITION_Y );

				Roi r = getRoiForPosition( trackmate, t, calibration.getRawX( x ), calibration.getRawY( y ) );
				Cell c = null;
				if ( r != null )
				{
					c = new Cell( r, ip, calibration );

					Map< String, Double > spotFeatures = s.getFeatures();
					Map< String, Double > cellFeatures = c.getFeatures();
					cellFeatures.putAll( spotFeatures );
					cellFeatures.put( PIXELS, 0.0d );
					cellFeatures.put( YFP_FLUORESCENCE_TOTAL, 0.0d );
					cellFeatures.put( CRIMSON_FLUORESCENCE_TOTAL, 0.0d );

					spotsFromRois.add( c );
					mapping.put( s, c );
				}
			}

			for ( Spot addSpot : spotsFromRois )
			{
				spots.add( addSpot, ( int ) Math.round( addSpot.getFeature( Spot.FRAME ) ) );
			}

			trackmate.computeSpotFeatures( true );

			// Compute spot features for new Cells
			model.beginUpdate();
			try
			{
				for ( Spot s : spotsFromRois )
				{
					assert ( s instanceof Cell );
					trackmate.getModel().updateFeatures( s );
				}
			}
			finally
			{
				model.endUpdate();
			}

			// Reassign features for old spots
			for ( Entry< Spot, Cell > entry : mapping.entrySet() )
			{
				Spot s = entry.getKey();
				Cell c = entry.getValue();

				Map< String, Double > spotFeatures = s.getFeatures();
				spotFeatures.clear();
				spotFeatures.putAll( c.getFeatures() );
			}
		}
	}

	/**
	 * Get's a {@link Roi} at a specified coordinate.
	 * 
	 * @param trackmate
	 * @param t
	 * @param x
	 *            x coordinate in pixel in the image coordinate system
	 * @param y
	 *            x coordinate in pixel in the image coordinate system
	 * @return a {@link Roi}.
	 */
	private Roi getRoiForPosition( TrackMate trackmate, final double t, final double x, final double y )
	{
		ImagePlus imp = trackmate.getSettings().imp;
		Overlay overlay = imp.getOverlay();
		Roi[] rois;

		// Will only work with Overlays
		if ( overlay == null )
		{
			// TODO Log
			return null;
		}
		rois = overlay.toArray();

		for ( Roi r : rois )
		{
			int roiPosition = r.getPosition();

			// Handle hyperstack separately, since r.getPosition()==0 for
			// hyperstacks
			if ( roiPosition <= 0 )
			{
				roiPosition = r.getTPosition() - 1;
			}

			if ( r.contains( ( int ) x, ( int ) y ) && ( roiPosition == ( int ) t ) ) { return r; }
		}

		return null;
	}

}
