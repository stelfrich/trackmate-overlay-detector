package fiji.plugin.trackmate.detection;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;

import net.imagej.ImgPlus;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.SpotDetector;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 *
 * @param <T>
 *            type of input
 */
public class BinaryMaskDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >, MultiThreaded
{

	/*
	 * FIELDS
	 */
	private final static String BASE_ERROR_MESSAGE = "OverlayDetector: ";

	/** The image to segment. Will not be modified. */
	protected final ImgPlus< T > img;

	/**
	 * The ImagePlus that is used for computing statistics and receiving the
	 * overlay.
	 */
	protected ImagePlus imp;

	protected int frame;

	protected int channel;

	protected double spotRadius;

	/**
	 * The list of {@link Spot}s that will be populated by this detector. Uses
	 * an {@link ArrayList} because this implementation is fast to add elements
	 * at the end of the list.
	 */
	protected List< Spot > spots = new ArrayList<>();

	/** The processing time in ms. */
	protected long processingTime;

	private int numThreads;

	protected String baseErrorMessage;

	protected String errorMessage;

	/*
	 * CONSTRUCTORS
	 */
	public BinaryMaskDetector( final ImgPlus< T > img, final ImagePlus imp, final int frame, final int channel, double spotRadius )
	{
		this.img = img;
		this.imp = imp;
		this.channel = channel;
		this.spotRadius = spotRadius;

		this.frame = frame;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
		setNumThreads();
	}

	/*
	 * METHODS
	 */
	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		spots.clear();

		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer( ParticleAnalyzer.SHOW_OVERLAY_OUTLINES, 0, null, 0, Double.POSITIVE_INFINITY, 0.0, 1.0 );
		particleAnalyzer.analyze(imp);

		ImageProcessor ip = imp.getProcessor();
		Calibration calibration = imp.getCalibration();

		// Get Rois for current frame; the naming scheme is 1-based
		Overlay overlayDuplicate = imp.getOverlay().duplicate();
		if ( imp.isHyperStack() )
		{
			overlayDuplicate.crop( channel, channel, 0, imp.getNSlices(), frame + 1, frame + 1 );
		}
		else
		{
			overlayDuplicate.crop( frame + 1, frame + 1 );
		}

		for ( Roi roi : overlayDuplicate.toArray() )
		{
			// We are not producing any rectangular rois. Thus we can use this
			// information to filter out the SpotOverlay and TrackOverlay.
			// FIXME Improve / Robustify
			if ( roi.getType() == Roi.RECTANGLE )
			{
				continue;
			}

			// Create Spot / Cell (which extends Spots)
			Spot spot = new RoiSpot( roi, ip, calibration );
			spot.putFeature( Spot.RADIUS, spotRadius );
			spots.add( spot );
		}

		return true;
	}

	@Override
	public void setNumThreads()
	{
		setNumThreads( Runtime.getRuntime().availableProcessors() );
	}

	@Override
	public void setNumThreads( int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public String toString()
	{
		return "Binary Mask Detector";
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

}
