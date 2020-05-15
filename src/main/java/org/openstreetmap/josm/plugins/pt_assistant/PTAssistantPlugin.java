// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JMenu;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditorHooks;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionGroup;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.customizepublictransportstop.CustomizeStopAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.AddStopPositionAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.CreatePlatformNodeAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.CreatePlatformNodeThroughReplaceAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.CreatePlatformShortcutAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.DoubleSplitAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.EdgeSelectionAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.ExtractPlatformNodeAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.PTWizardAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.RoutingAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.SortPTRouteMembersAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.SortPTRouteMembersMenuBar;
import org.openstreetmap.josm.plugins.pt_assistant.actions.SplitRoundaboutAction;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTRouteSegment;
import org.openstreetmap.josm.plugins.pt_assistant.gui.PTAssistantLayerManager;
import org.openstreetmap.josm.plugins.pt_assistant.validation.BicycleFootRouteValidatorTest;
import org.openstreetmap.josm.plugins.pt_assistant.validation.PTAssistantValidatorTest;
import org.openstreetmap.josm.plugins.ptl.DistanceBetweenStops;
import org.openstreetmap.josm.plugins.ptl.PublicTransportLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the main class of the PTAssistant plugin.
 *
 * @author darya / Darya Golovko
 *
 */
public class PTAssistantPlugin extends Plugin {
    public static final ImageProvider ICON = new ImageProvider("bus");

    /**
     * last fix that was can be re-applied to all similar route segments, can be
     * null if unavailable
     */
    private static PTRouteSegment lastFix;

    /** list of relation currently highlighted by the layer */
    private static List<Relation> highlightedRelations = new ArrayList<>();

    /**
     * Main constructor.
     *
     * @param info
     *            Required information of the plugin. Obtained from the jar file.
     */
    public PTAssistantPlugin(PluginInformation info) {
        super(info);
        OsmValidator.addTest(PTAssistantValidatorTest.class);
        OsmValidator.addTest(BicycleFootRouteValidatorTest.class);

        // "Public Transport" menu
        JMenu PublicTransportMenu = MainApplication.getMenu()
            .addMenu("File", trc("menu", "Public Transport"), KeyEvent.VK_P, 5, ht("/Menu/Public Transport"));
        addToMenu(PublicTransportMenu);

        SelectionEventManager.getInstance().addSelectionListener(PTAssistantLayerManager.PTLM);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(PTAssistantLayerManager.PTLM);
        initialiseWizard();
        initialiseShorcutsForCreatePlatformNode();
        addButtonsToRelationEditor();
    }

    /**
     * Called when the JOSM map frame is created or destroyed.
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            MainApplication.getMap().addMapMode(new IconToggleButton(new AddStopPositionAction()));
            MainApplication.getMap().addMapMode(new IconToggleButton(new EdgeSelectionAction()));
            MainApplication.getMap().addMapMode(new IconToggleButton(new DoubleSplitAction()));
        }
    }

    /**
     * Sets up the pt_assistant tab in JOSM Preferences
     */
    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new PTAssistantPluginPreferences();
    }

    public static PTRouteSegment getLastFix() {
        return lastFix;
    }

    /**
     * Remembers the last fix and enables/disables the Repeat last fix menu
     *
     * @param segment
     *            The last fix, call be null to disable the Repeat last fix menu
     */
    public static void setLastFix(PTRouteSegment segment) {
        lastFix = segment;
    }

    /**
     * Used in unit tests
     *
     * @param segment
     *            route segment
     */
    public static void setLastFixNoGui(PTRouteSegment segment) {
        lastFix = segment;
    }

    public static List<Relation> getHighlightedRelations() {
        return new ArrayList<>(highlightedRelations);
    }

    public static void addHighlightedRelation(Relation highlightedRelation) {
        highlightedRelations.add(highlightedRelation);
    }

    public static void clearHighlightedRelations() {
        highlightedRelations.clear();
    }

    private void addToMenu(JMenu menu) {
        MainMenu.add(menu, new SplitRoundaboutAction());
        MainMenu.add(menu, new CreatePlatformNodeAction());
        MainMenu.add(menu, new SortPTRouteMembersMenuBar());
        menu.addSeparator();
        MainMenu.add(menu, new PTWizardAction());
        menu.addSeparator();
        MainMenu.add(menu, new PublicTransportLayer.AddLayerAction());
        MainMenu.add(menu, new DistanceBetweenStops());
        menu.addSeparator();
        MainMenu.add(menu, CustomizeStopAction.createCustomizeStopAction());
    }

    private static void initialiseWizard() {
        PTWizardAction wizard = new PTWizardAction();
        wizard.noDialogBox = true;
        wizard.actionPerformed(null);
    }

    private static void initialiseShorcutsForCreatePlatformNode() {
        new CreatePlatformShortcutAction();
        new CreatePlatformNodeThroughReplaceAction();
        new ExtractPlatformNodeAction();
    }

    private void addButtonsToRelationEditor() {

        IRelationEditorActionGroup group1 = new IRelationEditorActionGroup() {
            @Override
            public int order() {
                    return 10;
               }

            @Override
            public List<AbstractRelationEditorAction> getActions(IRelationEditorActionAccess editorAccess) {
                return Arrays.asList(new RoutingAction(editorAccess));
            }
        };

        IRelationEditorActionGroup group2 = new IRelationEditorActionGroup() {
            @Override
            public int order() {
                    return 30;
                }

            @Override
            public List<AbstractRelationEditorAction> getActions(IRelationEditorActionAccess editorAccess) {
                return Arrays.asList(new SortPTRouteMembersAction(editorAccess));
            }
        };
        RelationEditorHooks.addActionsToMembers(group1);
        RelationEditorHooks.addActionsToMembers(group2);
    }
}
