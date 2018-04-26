package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.OverlayDetectorConfigurationPanel;
import ij.ImagePlus;

/**
 * {@link SpotDetectorFactory} for an {@link BinaryMaskDetector}.
 * 
 * @author Stefan Helfrich
 *
 * @param <T>
 */
@Plugin( type = SpotDetectorFactory.class )
public class BinaryMaskDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */
	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "OVERLAY_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Overlay detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>" + "This detector checks the overlays of a stack and creates Spots from them." + "This detector is currently only supported in IJ1 due to the ongoing api changes in IJ2." + "Developed by Stefan Helfrich." + "</html>";

	/*
	 * FIELDS
	 */
	/** The image to operate on. Multiple frames, single channel. */
	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	protected String errorMessage;

	/*
	 * METHODS
	 */
	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;

		return checkSettings( settings );
	}

	@Override
	public SpotDetector< T > getDetector( final Interval interval, int frame )
	{
		// Extract the IJ1 ImagePlus from the ImgLib2 ImgPlus
		// TODO Robustify
		ImagePlus imp = null;
		if ( img.getImg() instanceof ImagePlusImg )
		{
			ImagePlusImg impimg = ( ImagePlusImg ) img.getImg();
			try
			{
				imp = impimg.getImagePlus();
			}
			catch ( ImgLibException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// parameter is 1-based
		final int targetChannel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ); 
		final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, targetChannel );
		final ImgPlus< T > imgZ = HyperSliceImgPlus.fixZAxis( img, 0 );
		final ImgPlus< T > imgZT = HyperSliceImgPlus.fixTimeAxis( imgZ, frame );

		// get radius from UI
		final double spotRadius = ( Double ) settings.get( KEY_RADIUS );
		BinaryMaskDetector< T > detector = new BinaryMaskDetector<>( imgZT, imp, frame, targetChannel, spotRadius );
		// in TrackMate context, we use 1 thread per detector but multiple detectors
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		// TODO Add proper icon
		return null;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public String toString()
	{
		return NAME;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( settings, element, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new OverlayDetectorConfigurationPanel( settings.imp, settings, model, INFO_TEXT, NAME );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap< >();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_RADIUS, DEFAULT_RADIUS );
		return settings;
	}

	@Override
	public boolean checkSettings( Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );

		final List< String > mandatoryKeys = new ArrayList< >();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

}
