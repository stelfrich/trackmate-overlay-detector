package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * {@link SpotAnalyzerFactory} for a {@link SpotMeasurementAnalyzer}.
 * 
 * @author Stefan Helfrich
 *
 * @param <T>
 */
@Plugin( type = SpotAnalyzerFactory.class )
public class SpotMeasurementAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotAnalyzerFactory< T >
{

	/*
	 * Constants
	 */
	public static final String KEY = "SPOT_MEASUREMENT_ANALYZER";

	public static final String AREA = "AREA";

	public static final String VOLUME = "VOLUME";

	public static final String LENGTH = "LENGHT";

	public static final String PERIMETER = "PERIMETER";

	public static final String ROUNDNESS = "ROUNDNESS";

	public static final String SOLIDITY = "SOLIDITY";

	public static final String FERET_MIN = "FERET_MIN";

	public static final String FERET_MAX = "FERET_MAX";

	// MEAN
	// MAX
	// MIN
	// STDDEV

	public static final ArrayList< String > FEATURES = new ArrayList<>( 8 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap<>( 8 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 8 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 8 );

	private static final Map< String, Boolean > IS_INT = new HashMap<>( 8 );

	static
	{
		FEATURES.add( AREA );
		// FEATURES.add(VOLUME);
		FEATURES.add( LENGTH );
		// FEATURES.add(PERIMETER);
		// FEATURES.add(ROUNDNESS);
		// FEATURES.add(SOLIDITY);
		// FEATURES.add(FERET_MIN);
		// FEATURES.add(FERET_MAX);

		FEATURE_NAMES.put( AREA, "Cell area" );
		// FEATURE_NAMES.put(VOLUME, "Cell volume");
		FEATURE_NAMES.put( LENGTH, "Cell length" );
		// FEATURE_NAMES.put(PERIMETER, "Cell perimeter");
		// FEATURE_NAMES.put(ROUNDNESS, "Roundness");
		// FEATURE_NAMES.put(SOLIDITY, "Solidity");
		// FEATURE_NAMES.put(FERET_MIN, "Feret diameter (min distance)");
		// FEATURE_NAMES.put(FERET_MAX, "Feret diameter");

		FEATURE_SHORT_NAMES.put( AREA, "Area" );
		// FEATURE_SHORT_NAMES.put(VOLUME, "Volume");
		FEATURE_SHORT_NAMES.put( LENGTH, "Length" );
		// FEATURE_SHORT_NAMES.put(PERIMETER, "Perimeter");
		// FEATURE_SHORT_NAMES.put(ROUNDNESS, "Roundness");
		// FEATURE_SHORT_NAMES.put(SOLIDITY, "Solidity");
		// FEATURE_SHORT_NAMES.put(FERET_MIN, "Feret(min)");
		// FEATURE_SHORT_NAMES.put(FERET_MAX, "Feret");

		FEATURE_DIMENSIONS.put( AREA, Dimension.INTENSITY_SQUARED );
		// FEATURE_DIMENSIONS.put(VOLUME, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put( LENGTH, Dimension.LENGTH );
		// FEATURE_DIMENSIONS.put(PERIMETER, Dimension.INTENSITY);
		// FEATURE_DIMENSIONS.put(ROUNDNESS, Dimension.INTENSITY);
		// FEATURE_DIMENSIONS.put(SOLIDITY, Dimension.INTENSITY);
		// FEATURE_DIMENSIONS.put(FERET_MIN, Dimension.LENGTH);
		// FEATURE_DIMENSIONS.put(FERET_MAX, Dimension.LENGTH);

		IS_INT.put( AREA, false );
		// IS_INT.put(VOLUME, false);
		IS_INT.put( LENGTH, false );
		// IS_INT.put(PERIMETER, false);
		// IS_INT.put(ROUNDNESS, false);
		// IS_INT.put(SOLIDITY, false);
		// IS_INT.put(FERET_MIN, false);
		// IS_INT.put(FERET_MAX, false);
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

	@Override
	public String getInfoText()
	{
		// TODO Add info text
		return "";
	}

	@Override
	public ImageIcon getIcon()
	{
		// TODO Add icon
		return null;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return "Spot Measurement Analyzer";
	}

	@Override
	public SpotAnalyzer< T > getAnalyzer( Model model, ImgPlus< T > img, int frame, int channel )
	{
		final ImgPlus< T > imgT = HyperSliceImgPlus.fixTimeAxis( img, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );

		return new SpotMeasurementAnalyzer<>( imgT, spots );
	}

}
