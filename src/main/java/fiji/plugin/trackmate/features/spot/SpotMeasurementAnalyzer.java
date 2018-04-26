package fiji.plugin.trackmate.features.spot;

import java.util.Iterator;

import net.imagej.ImgPlus;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.RoiSpot;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;

/**
 * {@link SpotAnalyzer} that extracts features via the {@link Measurements} from
 * the underlying {@link Roi} on an {@link ImagePlus}.
 * 
 * @author Stefan Helfrich
 * 
 * @param <T>
 *            type of input
 */
public class SpotMeasurementAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	private ImagePlus imp;

	public SpotMeasurementAnalyzer( ImgPlus< T > img, Iterator< Spot > spots )
	{
		super( img, spots );

		this.imp = ImageJFunctions.wrap( img, "" );
	}

	@Override
	public void process( Spot spot )
	{
		// If the spot is not a Cell we don't process it.
		if ( !( spot instanceof RoiSpot ) ) { return; }

		/*
		 * for each pixel in roi: for each channel: get value and add to
		 * intensity sum divide intensitie sums by number of pixels (size of
		 * roi) for average
		 */
		Roi roi = ( ( RoiSpot ) spot ).getRoi();
		imp.setRoi( roi );

		ImageStatistics stats = imp.getStatistics( Measurements.AREA | Measurements.SHAPE_DESCRIPTORS | Measurements.FERET | Measurements.PERIMETER | Measurements.ELLIPSE );

		/*
		 * Write computed features to the spot that is being processed
		 */
		spot.putFeature( SpotMeasurementAnalyzerFactory.AREA, stats.area );
		// spot.putFeature(VOLUME, stats.area); // TODO Implement a real
		// computation
		spot.putFeature( SpotMeasurementAnalyzerFactory.LENGTH, stats.major );
		// spot.putFeature(PERIMETER, stats.area);
		// spot.putFeature(ROUNDNESS, stats.area);
		// spot.putFeature(SOLIDITY, stats.area);
		// spot.putFeature(FERET_MIN, stats.area);
		// spot.putFeature(FERET_MAX, stats.area);
	}

}
