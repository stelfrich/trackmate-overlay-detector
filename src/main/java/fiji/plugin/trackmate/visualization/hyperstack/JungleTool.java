package de.fzj.jungle.trackmate.visualization.hyperstack;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import de.fzj.jungle.segmentation.Cell;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.semiauto.SemiAutoTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;

/**
 * An ImageJ tool that is heavily inspired by the TrackMate tool. It changes the
 * adding of {@link Spot}s in such a way, that a connection between the manually
 * added spot and an underlying {@link Roi} is established.
 * 
 * @author Stefan Helfrich
 */
public class JungleTool extends AbstractTool implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ToolWithOptions
{

	private static final boolean DEBUG = true;

	private static final double COARSE_STEP = 2;

	private static final double FINE_STEP = 0.2f;

	private static final String TOOL_NAME = "JuNGLE Tool";

	private static final String TOOL_ICON = "C036D17D18D58D61D68D83D91DccDd4De7De8CeefDebC057D46D47D57D62D66D76D82DacDb8DbcDc6DcbDd6CfffD00D01D02D03D04D05D0aD0bD0cD0dD0eD0fD10D11D12D13D1aD1cD1dD1eD1fD20D21D22D2aD2dD2eD2fD30D31D39D3aD3bD3eD3fD40D49D4aD4bD4cD4fD50D5aD5bD5cD5fD6aD6bD6cD79D7aD7bD87D88D89D8aD8bD94D95D96D97D98D99D9aDa0Da2Da3Da4Da5Da6Da7Da8DafDb0Db1Db2Db3Db4Db5Db6DbfDc0Dc1Dc2Dc3DceDcfDd0Dd1Dd2DddDdeDdfDe0De1De2De3DecDedDeeDefDf0Df1Df2Df3Df4Df5DfaDfbDfcDfdDfeDffC047D4dDadDdaCeffD2bD9fDbeC9bcD23D2cD38D3cD48D59D69D85D93Da9Db7Dc5DcdDd3DdcC046D24D28D37D42D67D84D8eD9cDabDb9Dc7Dd5DdbDe6De9CeffD06D09D4eDc4Df6Df9C379D15D19D32D3dD51D5dD5eD6dD77D8cD92D9bDaaDaeDe5DeaC057D25D26D27D34D35D36D43D44D45D52D53D54D55D56D63D64D65D73D74D75D8dD9dDbbDc8Dc9DcaDd7Dd8Dd9CfffD29D6fD78D86CdeeD07D08D14D1bD41D60D70D7cD7fD80D8fDa1De4Df7Df8C046D16D33D6eD71D7dD7eD81D9eDbdCeffD90C058D72Dba";

	/**
	 * Fall back default radius when the settings does not give a default radius
	 * to use.
	 */
	private static final double FALL_BACK_RADIUS = 1;

	/** The singleton instance. */
	private static JungleTool instance;

	/** Stores the edited spot in each {@link ImagePlus}. */
	private final HashMap< ImagePlus, Spot > editedSpots = new HashMap<>();

	/** Stores the view possible attached to each {@link ImagePlus}. */
	HashMap< ImagePlus, JungleStackDisplayer > displayers = new HashMap<>();

	/** The radius of the previously edited spot. */
	private Double previousRadius = null;

	private Spot quickEditedSpot;

	/** Flag for the auto-linking mode. */
	private boolean autolinkingmode = false;

	SpotEditToolParams params = new SpotEditToolParams();

	private Logger logger = Logger.VOID_LOGGER;

	/**
	 * The last {@link ImagePlus} on which an action happened.
	 */
	ImagePlus imp;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Singleton
	 */
	public JungleTool()
	{}

	/**
	 * Return the singleton instance for this tool. If it was not previously
	 * instantiated, this calls instantiates it.
	 */
	public static JungleTool getInstance()
	{
		if ( null == instance )
		{
			instance = new JungleTool();
			if ( DEBUG )
				System.out.println( "[SpotEditTool] Instantiating: " + instance );
		}
		if ( DEBUG )
			System.out.println( "[SpotEditTool] Returning instance: " + instance );
		return instance;
	}

	/**
	 * Return true if the tool is currently present in ImageJ toolbar.
	 */
	public static boolean isLaunched()
	{
		final Toolbar toolbar = Toolbar.getInstance();
		if ( null != toolbar && toolbar.getToolId( TOOL_NAME ) >= 0 )
			return true;
		return false;
	}

	/*
	 * METHODS
	 */

	@Override
	public String getToolName()
	{
		return TOOL_NAME;
	}

	@Override
	public String getToolIcon()
	{
		return TOOL_ICON;
	}

	/**
	 * Overridden so that we can keep track of the last ImagePlus actions are
	 * taken on. Very much like ImageJ.
	 */
	@Override
	public ImagePlus getImagePlus( final ComponentEvent e )
	{
		imp = super.getImagePlus( e );
		return imp;
	}

	/**
	 * Register the given {@link HyperStackDisplayer}. If this method id not
	 * called, the tool will not respond.
	 */
	public void register( final ImagePlus imp, final JungleStackDisplayer displayer )
	{
		if ( DEBUG )
			System.out.println( "[SpotEditTool] Currently registered: " + displayers );

		if ( displayers.containsKey( imp ) )
		{
			unregisterTool( imp );
			if ( DEBUG )
				System.out.println( "[SpotEditTool] De-registering " + imp + " as tool listener." );
		}

		displayers.put( imp, displayer );
		if ( DEBUG )
		{
			System.out.println( "[SpotEditTool] Registering " + imp + " and " + displayer + "." + " Currently registered: " + displayers );
		}
	}

	/*
	 * MOUSE AND MOUSE MOTION
	 */

	@Override
	public void mouseClicked( final MouseEvent e )
	{

		final ImagePlus imp = getImagePlus( e );
		final JungleStackDisplayer displayer = displayers.get( imp );
		if ( DEBUG )
		{
			System.out.println( "[SpotEditTool] @mouseClicked" );
			System.out.println( "[SpotEditTool] Got " + imp + " as ImagePlus" );
			System.out.println( "[SpotEditTool] Matching displayer: " + displayer );

			for ( final MouseListener ml : imp.getCanvas().getMouseListeners() )
			{
				System.out.println( "[SpotEditTool] mouse listener: " + ml );
			}

		}

		if ( null == displayer )
			return;

		final Spot clickLocation = makeSpot( imp, displayer, getImageCanvas( e ), e.getPoint() );
		final int frame = displayer.imp.getFrame() - 1;
		final Model model = displayer.getModel();
		Spot target = model.getSpots().getSpotAt( clickLocation, frame, true );
		Spot editedSpot = editedSpots.get( imp );

		final SelectionModel selectionModel = displayer.getSelectionModel();

		// Check desired behavior
		switch ( e.getClickCount() )
		{

		case 1:
		{
			// Change selection
			// only if we are not currently editing.
			if ( null != editedSpot ) { return; }
			// If no target, we clear selection
			if ( null == target )
			{

				if ( !autolinkingmode )
				{
					selectionModel.clearSelection();
				}

			}
			else
			{

				updateStatusBar( target, imp.getCalibration().getUnits() );
				final int addToSelectionMask = InputEvent.SHIFT_DOWN_MASK;
				if ( ( e.getModifiersEx() & addToSelectionMask ) == addToSelectionMask )
				{
					if ( selectionModel.getSpotSelection().contains( target ) )
					{
						selectionModel.removeSpotFromSelection( target );
					}
					else
					{
						selectionModel.addSpotToSelection( target );
					}
				}
				else
				{
					selectionModel.clearSpotSelection();
					selectionModel.addSpotToSelection( target );
				}
			}
			break;
		}

		case 2:
		{
			// Edit spot

			if ( null == editedSpot )
			{
				// No spot is currently edited, we pick one to edit
				Double radius;
				if ( null != target && null != target.getFeature( Spot.RADIUS ) )
				{
					radius = target.getFeature( Spot.RADIUS );
				}
				else
				{
					radius = previousRadius;
					if ( null == radius )
					{
						radius = FALL_BACK_RADIUS;
					}
				}
				if ( null == target || target.squareDistanceTo( clickLocation ) > radius * radius )
				{
					// Create a new spot if not inside one
					target = clickLocation;
					if ( null == previousRadius )
					{
						previousRadius = radius;
					}
					target.putFeature( Spot.RADIUS, previousRadius );
				}
				editedSpot = target;
				displayer.spotOverlay.setEditingSpot( editedSpot );
				displayer.refresh();
				// Edit spot
				if ( DEBUG )
					System.out.println( "[SpotEditTool] mouseClicked: Set " + editedSpot + " as editing spot for this imp." );

			}
			else
			{
				// We leave editing mode
				if ( DEBUG )
					System.out.println( "[SpotEditTool] mouseClicked: Got " + editedSpot + " as editing spot for this imp, leaving editing mode." );

				// A hack: we update the current z and t of the edited spot to
				// the current one,
				// because it is not updated otherwise: there is no way to
				// listen to slice change
				final double calibration[] = TMUtils.getSpatialCalibration( imp );
				final double zslice = ( displayer.imp.getSlice() - 1 ) * calibration[ 2 ];
				editedSpot.putFeature( Spot.POSITION_Z, zslice );
				final Double initFrame = editedSpot.getFeature( Spot.FRAME );
				// Move it in Z
				final double z = ( displayer.imp.getSlice() - 1 ) * calibration[ 2 ];
				editedSpot.putFeature( Spot.POSITION_Z, z );
				editedSpot.putFeature( Spot.POSITION_T, frame * imp.getCalibration().frameInterval );
				editedSpot.putFeature( Spot.FRAME, Double.valueOf( frame ) );

				model.beginUpdate();
				try
				{
					if ( initFrame == null )
					{
						// Means that the spot was created
						model.addSpotTo( editedSpot, frame );
					}
					else if ( initFrame != frame )
					{
						// Move it to the new frame
						model.moveSpotFrom( editedSpot, initFrame.intValue(), frame );
					}
					else
					{
						// The spots pre-existed and was not moved across frames
						model.updateFeatures( editedSpot );
					}

				}
				finally
				{
					model.endUpdate();
				}

				/*
				 * If we are in auto-link mode, we create an edge with spot in
				 * selection, if there is just one and if it is in a previous
				 * frame
				 */
				if ( autolinkingmode )
				{
					final Set< Spot > spotSelection = selectionModel.getSpotSelection();
					if ( spotSelection.size() == 1 )
					{
						final Spot source = spotSelection.iterator().next();
						if ( editedSpot.diffTo( source, Spot.FRAME ) > 0 )
						{
							model.beginUpdate();
							try
							{
								model.addEdge( source, editedSpot, -1 );
								logger.log( "Created a link between " + source + " and " + editedSpot + ".\n" );
							}
							finally
							{
								model.endUpdate();
							}
						}
					}
				}

				// Set selection
				selectionModel.clearSpotSelection();
				selectionModel.addSpotToSelection( editedSpot );

				// Forget edited spot, but remember its radius
				previousRadius = editedSpot.getFeature( Spot.RADIUS );
				editedSpot = null;
				displayer.spotOverlay.setEditingSpot( null );
			}
			break;
		}
		}
		editedSpots.put( imp, editedSpot );
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{}

	@Override
	public void mouseReleased( final MouseEvent e )
	{}

	@Override
	public void mouseEntered( final MouseEvent e )
	{}

	@Override
	public void mouseExited( final MouseEvent e )
	{}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		final ImagePlus imp = getImagePlus( e );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final JungleStackDisplayer displayer = displayers.get( imp );
		if ( null == displayer )
			return;
		final Spot editedSpot = editedSpots.get( imp );
		if ( null == editedSpot )
			return;

		final Point mouseLocation = e.getPoint();
		final ImageCanvas canvas = getImageCanvas( e );
		final double x = ( -0.5 + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ];
		final double y = ( -0.5 + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ];
		final double z = ( imp.getSlice() - 1 ) * calibration[ 2 ];
		editedSpot.putFeature( Spot.POSITION_X, x );
		editedSpot.putFeature( Spot.POSITION_Y, y );
		editedSpot.putFeature( Spot.POSITION_Z, z );
		displayer.imp.updateAndDraw();
		updateStatusBar( editedSpot, imp.getCalibration().getUnits() );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		if ( quickEditedSpot == null )
			return;
		final ImagePlus imp = getImagePlus( e );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final JungleStackDisplayer displayer = displayers.get( imp );
		if ( null == displayer )
			return;
		final Spot editedSpot = editedSpots.get( imp );
		if ( null != editedSpot )
			return;

		final Point mouseLocation = e.getPoint();
		final ImageCanvas canvas = getImageCanvas( e );
		final double x = ( -0.5 + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ];
		final double y = ( -0.5 + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ];
		final double z = ( imp.getSlice() - 1 ) * calibration[ 2 ];

		quickEditedSpot.putFeature( Spot.POSITION_X, x );
		quickEditedSpot.putFeature( Spot.POSITION_Y, y );
		quickEditedSpot.putFeature( Spot.POSITION_Z, z );
		displayer.imp.updateAndDraw();

	}

	/*
	 * MOUSEWHEEL
	 */

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		final ImagePlus imp = getImagePlus( e );
		final JungleStackDisplayer displayer = displayers.get( imp );
		if ( null == displayer )
			return;
		final Spot editedSpot = editedSpots.get( imp );
		if ( null == editedSpot || !e.isAltDown() )
			return;
		double radius = editedSpot.getFeature( Spot.RADIUS );
		final double dx = imp.getCalibration().pixelWidth;
		if ( e.isShiftDown() )
		{
			radius += e.getWheelRotation() * dx * COARSE_STEP;
		}
		else
		{
			radius += e.getWheelRotation() * dx * FINE_STEP;
		}
		if ( radius < dx ) { return; }

		editedSpot.putFeature( Spot.RADIUS, radius );
		displayer.imp.updateAndDraw();
		e.consume();
		updateStatusBar( editedSpot, imp.getCalibration().getUnits() );
	}

	/*
	 * KEYLISTENER
	 */

	@Override
	public void keyTyped( final KeyEvent e )
	{}

	@Override
	public void keyPressed( final KeyEvent e )
	{

		if ( DEBUG )
			System.out.println( "[SpotEditTool] keyPressed: " + e.getKeyChar() );

		final ImagePlus imp = getImagePlus( e );
		if ( imp == null )
			return;
		final JungleStackDisplayer displayer = displayers.get( imp );
		if ( null == displayer )
			return;

		final Model model = displayer.getModel();
		final SelectionModel selectionModel = displayer.getSelectionModel();
		Spot editedSpot = editedSpots.get( imp );
		final ImageCanvas canvas = getImageCanvas( e );

		final int keycode = e.getKeyCode();

		switch ( keycode )
		{

		// Delete currently edited spot
		case KeyEvent.VK_DELETE:
		{

			if ( null == editedSpot )
			{
				final ArrayList< Spot > spotSelection = new ArrayList<>( selectionModel.getSpotSelection() );
				final ArrayList< DefaultWeightedEdge > edgeSelection = new ArrayList<>( selectionModel.getEdgeSelection() );
				model.beginUpdate();
				try
				{
					selectionModel.clearSelection();
					for ( final DefaultWeightedEdge edge : edgeSelection )
					{
						model.removeEdge( edge );
					}
					for ( final Spot spot : spotSelection )
					{
						model.removeSpot( spot );
					}
				}
				finally
				{
					model.endUpdate();
				}

			}
			else
			{
				model.beginUpdate();
				try
				{
					model.removeSpot( editedSpot );
				}
				finally
				{
					model.endUpdate();
				}
				editedSpot = null;
				editedSpots.put( imp, null );
			}
			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick add spot at mouse
		case KeyEvent.VK_A:
		{

			if ( null == editedSpot )
			{

				if ( e.isShiftDown() )
				{

					// Semi-auto tracking
					semiAutoTracking( model, selectionModel, imp );

				}
				else
				{

					// Create and drop a new spot
					double radius;
					if ( null != previousRadius )
					{
						radius = previousRadius;
					}
					else
					{
						radius = FALL_BACK_RADIUS;
					}

					final Spot newSpot = makeSpot( imp, displayer, canvas, null );
					final double dt = imp.getCalibration().frameInterval;
					final int frame = displayer.imp.getFrame() - 1;
					newSpot.putFeature( Spot.POSITION_T, frame * dt );
					newSpot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
					newSpot.putFeature( Spot.RADIUS, radius );
					newSpot.putFeature( Spot.QUALITY, -1d );

					model.beginUpdate();
					try
					{
						model.addSpotTo( newSpot, frame );
					}
					finally
					{
						model.endUpdate();
					}

					/*
					 * If we are in auto-link mode, we create an edge with spot
					 * in selection, if there is just one and if it is in a
					 * previous frame
					 */
					if ( autolinkingmode )
					{
						final Set< Spot > spotSelection = selectionModel.getSpotSelection();
						if ( spotSelection.size() == 1 )
						{
							final Spot source = spotSelection.iterator().next();
							if ( newSpot.diffTo( source, Spot.FRAME ) != 0 )
							{
								model.beginUpdate();
								try
								{
									model.addEdge( source, newSpot, -1 );
									logger.log( "Created a link between " + source + " and " + newSpot + ".\n" );
								}
								finally
								{
									model.endUpdate();
								}
							}
						}
						selectionModel.clearSpotSelection();
						selectionModel.addSpotToSelection( newSpot );
					}

					imp.updateAndDraw();
					e.consume();
				}

			}
			else
			{

			}
			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D:
		{

			if ( null == editedSpot )
			{

				final int frame = displayer.imp.getFrame() - 1;
				final Spot clickLocation = makeSpot( imp, displayer, canvas, null );
				final Spot target = model.getSpots().getSpotAt( clickLocation, frame, true );
				if ( null == target )
				{
					e.consume(); // Consume it anyway, so that we are not
									// bothered by IJ
					return;
				}

				selectionModel.removeSpotFromSelection( target );
				model.beginUpdate();
				try
				{
					model.removeSpot( target );
				}
				finally
				{
					model.endUpdate();
				}

				imp.updateAndDraw();

			}
			else
			{

			}
			e.consume();
			break;
		}

		// Quick move spot under the mouse
		case KeyEvent.VK_SPACE:
		{

			if ( null == quickEditedSpot )
			{
				final int frame = displayer.imp.getFrame() - 1;
				final Spot clickLocation = makeSpot( imp, displayer, canvas, null );
				quickEditedSpot = model.getSpots().getSpotAt( clickLocation, frame, true );
				if ( null == quickEditedSpot ) { return; // un-consumed event
				}
			}
			e.consume();
			break;

		}

		// Quick change spot radius
		case KeyEvent.VK_Q:
		case KeyEvent.VK_E:
		{

			e.consume();
			if ( null == editedSpot )
			{

				final int frame = displayer.imp.getFrame() - 1;
				final Spot clickLocation = makeSpot( imp, displayer, canvas, null );
				final Spot target = model.getSpots().getSpotAt( clickLocation, frame, true );
				if ( null == target ) { return; }

				int factor;
				if ( e.getKeyCode() == KeyEvent.VK_Q )
				{
					factor = -1;
				}
				else
				{
					factor = 1;
				}
				double radius = target.getFeature( Spot.RADIUS );
				final double dx = imp.getCalibration().pixelWidth;
				if ( e.isShiftDown() )
				{
					radius += factor * dx * COARSE_STEP;
				}
				else
				{
					radius += factor * dx * FINE_STEP;
				}
				if ( radius <= dx ) { return; }

				target.putFeature( Spot.RADIUS, radius );
				model.beginUpdate();
				try
				{
					model.updateFeatures( target );
				}
				finally
				{
					model.endUpdate();
				}

				imp.updateAndDraw();
			}

			break;
		}

		// Copy spots from previous frame
		case KeyEvent.VK_V:
		{
			if ( e.isShiftDown() )
			{

				final int currentFrame = imp.getFrame() - 1;
				if ( currentFrame > 0 )
				{

					final SpotCollection spots = model.getSpots();
					if ( spots.getNSpots( currentFrame - 1, true ) == 0 )
					{
						e.consume();
						break;
					}
					final HashSet< Spot > copiedSpots = new HashSet<>( spots.getNSpots( currentFrame - 1, true ) );
					final HashSet< String > featuresKey = new HashSet<>( spots.iterator( currentFrame - 1, true ).next().getFeatures().keySet() );
					featuresKey.remove( Spot.POSITION_T ); // Deal with time
															// separately
					double dt = imp.getCalibration().frameInterval;
					if ( dt == 0 )
					{
						dt = 1;
					}

					for ( final Iterator< Spot > it = spots.iterator( currentFrame - 1, true ); it.hasNext(); )
					{
						final Spot spot = it.next();
						final Spot newSpot = new Spot( spot );
						// Deal with features
						Double val;
						for ( final String key : featuresKey )
						{
							val = spot.getFeature( key );
							if ( val == null )
							{
								continue;
							}
							newSpot.putFeature( key, val );
						}
						newSpot.putFeature( Spot.POSITION_T, spot.getFeature( Spot.POSITION_T ) + dt );
						copiedSpots.add( newSpot );
					}

					model.beginUpdate();
					try
					{
						// Remove old ones
						for ( final Iterator< Spot > it = spots.iterator( currentFrame, true ); it.hasNext(); )
						{
							model.removeSpot( it.next() );
						}
						// Add new ones
						for ( final Spot spot : copiedSpots )
						{
							model.addSpotTo( spot, currentFrame );
						}
					}
					finally
					{
						model.endUpdate();
						imp.updateAndDraw();
					}
				}

				e.consume();
			}
			break;
		}

		case KeyEvent.VK_L:
		{

			if ( e.isShiftDown() )
			{
				/*
				 * Toggle auto-linking mode
				 */
				autolinkingmode = !autolinkingmode;
				logger.log( "Toggled auto-linking mode " + ( autolinkingmode ? "on.\n" : "off.\n" ) );

			}
			else
			{
				/*
				 * Toggle a link between two spots.
				 */
				final Set< Spot > selectedSpots = selectionModel.getSpotSelection();
				if ( selectedSpots.size() == 2 )
				{
					final Iterator< Spot > it = selectedSpots.iterator();
					final Spot source = it.next();
					final Spot target = it.next();

					if ( model.getTrackModel().containsEdge( source, target ) )
					{
						/*
						 * Remove it
						 */
						model.beginUpdate();
						try
						{
							model.removeEdge( source, target );
							logger.log( "Removed edge between " + source + " and " + target + ".\n" );
						}
						finally
						{
							model.endUpdate();
						}

					}
					else
					{
						/*
						 * Create a new link
						 */
						final int ts = source.getFeature( Spot.FRAME ).intValue();
						final int tt = target.getFeature( Spot.FRAME ).intValue();

						if ( tt != ts )
						{
							model.beginUpdate();
							try
							{
								model.addEdge( source, target, -1 );
								logger.log( "Created an edge between " + source + " and " + target + ".\n" );
							}
							finally
							{
								model.endUpdate();
							}
							/*
							 * To emulate a kind of automatic linking, we put
							 * the last spot to the selection, so several spots
							 * can be tracked in a row without having to
							 * de-select one
							 */
							Spot single;
							if ( tt > ts )
							{
								single = target;
							}
							else
							{
								single = source;
							}
							selectionModel.clearSpotSelection();
							selectionModel.addSpotToSelection( single );

						}
						else
						{
							logger.error( "Cannot create an edge between two spots belonging in the same frame." );
						}
					}

				}
				else
				{
					logger.error( "Expected selection to contain 2 spots, found " + selectedSpots.size() + ".\n" );
				}

			}
			e.consume();
			break;

		}

		case KeyEvent.VK_W:
		{
			e.consume(); // consume it: we do not want IJ to close the window
			break;
		}

		}

	}

	private Map< Roi, Cell > cellStore = new HashMap<>();

	private Spot makeSpot( final ImagePlus imp, JungleStackDisplayer displayer, final ImageCanvas canvas, Point mouseLocation )
	{
		if ( displayer == null )
		{
			displayer = displayers.get( imp );
		}
		if ( mouseLocation == null )
		{
			mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen( mouseLocation, canvas );
		}
		final double[] calibration = TMUtils.getSpatialCalibration( imp );

		int ox = canvas.offScreenX( mouseLocation.x );
		int oy = canvas.offScreenY( mouseLocation.y );

		Roi r = mouseOverRoi( ox, oy );
		if ( r != null )
		{
			if ( cellStore.containsKey( r ) )
			{
				return cellStore.get( r );
			}
			else
			{
				Cell c = new Cell( r, imp.getProcessor(), imp.getCalibration() );
				cellStore.put( r, c );

				return c;
			}
		}

		return new Spot( ( -0.5d + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ], ( -0.5d + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ], ( imp.getSlice() - 1 ) * calibration[ 2 ], FALL_BACK_RADIUS, -1d );
	}

	private Roi mouseOverRoi( int ox, int oy )
	{
		Overlay overlay = imp.getOverlay();
		Roi[] rois;

		// There is no overlay .. work with Rois and RoiManager
		if ( overlay == null )
		{
			RoiManager manager = RoiManager.getInstance();
			if ( manager == null )
			{
				manager = new RoiManager( true );
			}

			rois = manager.getRoisAsArray();
		}
		else
		{
			rois = overlay.toArray();
		}

		for ( Roi r : rois )
		{
			int roiPosition = r.getPosition();
			int impPosition = imp.getCurrentSlice();

			// Handle hyperstack separately, since r.getPosition()==0 for
			// hyperstacks
			if ( roiPosition <= 0 )
			{
				roiPosition = r.getTPosition();
				impPosition = imp.getT();
			}

			if ( r.contains( ox, oy ) && ( roiPosition == impPosition ) ) { return r; }
		}

		return null;
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
		if ( DEBUG )
			System.out.println( "[SpotEditTool] keyReleased: " + e.getKeyChar() );

		switch ( e.getKeyCode() )
		{
		case KeyEvent.VK_SPACE:
		{
			if ( null == quickEditedSpot )
				return;
			final ImagePlus imp = getImagePlus( e );
			if ( imp == null )
				return;
			final JungleStackDisplayer displayer = displayers.get( imp );
			if ( null == displayer )
				return;
			final Model model = displayer.getModel();
			model.beginUpdate();
			try
			{
				model.updateFeatures( quickEditedSpot );
			}
			finally
			{
				model.endUpdate();
			}
			quickEditedSpot = null;
			break;
		}
		}

	}

	/*
	 * PRIVATE METHODS
	 */

	private void updateStatusBar( final Spot spot, final String units )
	{
		if ( null == spot )
			return;
		String statusString = "";
		if ( null == spot.getName() || spot.getName().equals( "" ) )
		{
			statusString = String.format( "Spot ID%d, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", spot.ID(), spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.RADIUS ), units );
		}
		else
		{
			statusString = String.format( "Spot %s, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", spot.getName(), spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.RADIUS ), units );
		}
		IJ.showStatus( statusString );
	}

	void semiAutoTracking( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		@SuppressWarnings( "rawtypes" )
		final SemiAutoTracker autotracker = new SemiAutoTracker( model, selectionModel, imp, logger );
		autotracker.setParameters( params.qualityThreshold, params.distanceTolerance, -1 );
		autotracker.setNumThreads( 4 );
		new Thread( "TrackMate semi-automated tracking thread" )
		{
			@Override
			public void run()
			{
				final boolean ok = autotracker.checkInput() && autotracker.process();
				if ( !ok )
				{
					logger.error( autotracker.getErrorMessage() );
				}
			}
		}.start();
	}

	@Override
	public void showOptionDialog()
	{}

	/*
	 * INNER CLASSES
	 */

	static class SpotEditToolParams
	{
		/*
		 * Semi-auto tracking parameters
		 */
		/**
		 * The fraction of the initial quality above which we keep new spots.
		 * The highest, the more intolerant.
		 */
		double qualityThreshold = 0.5;

		/**
		 * How close must be the new spot found to be accepted, in radius units.
		 */
		double distanceTolerance = 2d;

		@Override
		public String toString()
		{
			return super.toString() + ": " + "QualityThreshold = " + qualityThreshold + ", DistanceTolerance = " + distanceTolerance;
		}
	}

}
