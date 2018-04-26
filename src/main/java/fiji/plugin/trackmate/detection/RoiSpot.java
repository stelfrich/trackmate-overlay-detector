package fiji.plugin.trackmate.detection;

import java.io.Serializable;

import fiji.plugin.trackmate.Spot;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class RoiSpot extends Spot implements Serializable {

	private Roi roi;
	private ImageProcessor ip;
	private Calibration calibration;
	
	public RoiSpot() {
		super( 0d, 0d, 0d, 0d, 0d );
	}
	
	public RoiSpot(final Roi roi) {
		super( 0d, 0d, 0d, 0d, 0d );
		this.roi = roi;
	}

	/**
	 * TODO Update
	 * 
	 * Constructs a {@link Cell} from a {@link Roi}, an {@link ImageProcessor},
	 * and a {@link Calibration} taken from an {@link ImagePlus}.
	 * 
	 * @param roi
	 *            {@link Roi} to associate with the cell.
	 * @param ip
	 *            {@link ImageProcessor} with which {@code this} is associated.
	 * @param calibration
	 *            {@link Calibration} information that is used for extracting
	 *            measurements about {@code this}.
	 */
	public RoiSpot( final Roi roi, final ImageProcessor ip, final Calibration calibration )
	{
		// This will also call a constructor of Spot (with default values)
		this();

		// Set the Cell variables
		this.roi = roi;
		this.ip = ip;
		this.calibration = calibration;

		// Set the Spot variables
		this.setName( roi.getName() );

		double[] centroid = this.computeCentroidArray( calibration );
		this.putFeature( POSITION_X, Double.valueOf( centroid[ 0 ] ) );
		this.putFeature( POSITION_Y, Double.valueOf( centroid[ 1 ] ) );
		this.putFeature( POSITION_Z, Double.valueOf( centroid[ 2 ] ) );

		this.putFeature( RADIUS, Double.valueOf( 0.25d ) );
		this.putFeature( QUALITY, Double.valueOf( 1d ) );
	}
	
	/**
	 * TODO Documentation
	 * 
	 * @param calibration
	 * @return
	 */
	@SuppressWarnings( "hiding" )
	public double[] computeCentroidArray( Calibration calibration )
	{
		try
		{
			ip.setRoi( roi );
		}
		catch ( IllegalArgumentException e )
		{
			System.err.println( "Blaaaaa" );
		}

		ImageStatistics stats = ImageStatistics.getStatistics( ip, Measurements.CENTROID, calibration );

		return new double[] { stats.xCentroid, stats.yCentroid, 0d };
	}

	public Roi getRoi() {
		return this.roi;
	}
	
}
