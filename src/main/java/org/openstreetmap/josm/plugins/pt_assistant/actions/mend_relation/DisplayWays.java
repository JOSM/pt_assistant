// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.tools.Logging;

import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays the routes on the map
 *
 * @author sudhanshu2
 */
public class DisplayWays {
    private AbstractMapViewPaintable temporaryLayer;
    private final DisplayWaysInterface display;
    private final MendRelationPaintVisitorInterface paint;
    private  HashMap<Character, List<Way>> wayListColoring;

    /**
     *
     * @param display
     */
    public DisplayWays(DisplayWaysInterface display, MendRelationPaintVisitorInterface paint) {
        this.display = display;
        this.paint = paint;
    }

    /**
     * This method specifies what happens when the escape key is pressed (the routing helper is exited)
     */
    private void escapeSequence() {
        display.setNextIndex(false);
        display.setShorterRoutes(false);
        display.setSetEnable(true);
        display.setHalt(true);
        display.setEnabled(true);
        MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
    }

    /**
     * Returns list of letters/numbers to be displayed for the ways
     *
     * @return allowedCharacters which contains list of allowed character for the ways
     */
    private List<Character> getAllowedCharacters() {
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        List<Character> allowedCharacters = new ArrayList<>();

        if (numeric) {
            allowedCharacters.add('7');
            if (display.getShowOption0()) {
                allowedCharacters.add('0');
            }
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (display.getShowOption0()) {
                allowedCharacters.add('W');
            }
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        return allowedCharacters;
    }

    /**
     *
     * @param fixVariants
     */
    void displayFixVariants(List<Way> fixVariants) {
        List<Character> allowedCharacters = getAllowedCharacters();
        HashMap<Way, Character> wayColoring = new HashMap<>();
        char alphabet;
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        if (numeric) {
            alphabet = '1';
        } else {
            alphabet = 'A';
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayColoring.put(fixVariants.get(i), alphabet);
            alphabet++;
        }

        display.setWayColoring(wayColoring);

        // remove any existing temporary layer
        display.removeTemporaryLayers();

        if (display.getAbort()) {
            return;
        }

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                display.setDownloadCounter(0);
                if (display.getAbort()) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    display.setNextIndex(true);
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        display.setShorterRoutes(!display.getShorterRoutes());
                        removeKeyListenerAndTemporaryLayer(this);
                        display.callNextWay(display.getCurrentIndex());
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.getNextWayAfterSelection(Collections.singletonList(fixVariants.get(idx)));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    escapeSequence();
                }
            }
        });
    }

    /**
     *
     * @param fixVariants
     * @param idx1
     */
    void displayBacktrackFixVariant(List<Way> fixVariants, int idx1) {
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        HashMap<Way, Character> wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();
        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (display.getShowOption0())
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (display.getShowOption0())
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayColoring.put(fixVariants.get(i), alphabet);
            alphabet++;
        }

        display.setWayColoring(wayColoring);

        // remove any existing temporary layer
        display.removeTemporaryLayers();

        if (display.getShowOption0()) {
            return;
        }

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                display.setDownloadCounter(0);
                if (display.getAbort()) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    display.setNextIndex(true);
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        display.setShorterRoutes(!display.getShorterRoutes());
                        removeKeyListenerAndTemporaryLayer(this);
                        display.callNextWay(display.getCurrentIndex());
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.backTrack(display.getCurrentWay(), idx1 + 1);
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.findWayAfterChunk(display.getCurrentWay());
                        display.getNextWayAfterBackTrackSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    escapeSequence();
                }
            }
        });
    }

    /**
     *
     * @param fixVariants
     */
    void displayFixVariantsWithOverlappingWays(List<List<Way>> fixVariants) {
        // find the letters of the fix variants:
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        wayListColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();

        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (display.getShowOption0())
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (display.getShowOption0())
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayListColoring.put(alphabet, fixVariants.get(i));
            alphabet++;
        }

        // remove any existing temporary layer
        display.removeTemporaryLayers();

        if (display.getAbort()) {
            return;
        }

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants.stream().flatMap(Collection::stream).collect(Collectors.toList()));

        // display the fix variants:
        temporaryLayer = new MendRelationAddMultipleLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                display.setDownloadCounter(0);
                if (display.getAbort()) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    display.setNextIndex(true);
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        display.setShorterRoutes(!display.getShorterRoutes());
                        removeKeyListenerAndTemporaryLayer(this);
                        display.callNextWay(display.getCurrentIndex());
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        display.setShorterRoutes(false);
                        display.getNextWayAfterSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    escapeSequence();
                }
            }
        });
    }

    /**
     *
     * @param wayIndices
     */
    void displayWaysToRemove(List<Integer> wayIndices) {
        // find the letters of the fix variants:
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        if (numeric)
            alphabet = '1';
        HashMap<Way, Character> wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();

        if (numeric) {
            allowedCharacters.add('1');
            allowedCharacters.add('2');
            allowedCharacters.add('3');
        } else {
            allowedCharacters.add('A');
            allowedCharacters.add('B');
            allowedCharacters.add('R');
        }

        for (int i = 0; i < 5 && i < wayIndices.size(); i++) {
            wayColoring.put(display.getMembers().get(wayIndices.get(i)).getWay(), alphabet);
        }

        display.setWayColoring(wayColoring);

        if (display.getNotice().equals("vehicle travels against oneway restriction")) {
            if (numeric) {
                allowedCharacters.add('4');
            } else {
                allowedCharacters.add('C');
            }
        }

        // remove any existing temporary layer
        display.removeTemporaryLayers();

        if (display.getAbort()) {
            return;
        }

        // zoom to problem:
        final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

        for (Integer i : wayIndices) {
            waysToZoom.add(display.getMembers().get(i).getWay());
        }

        AutoScaleAction.zoomTo(waysToZoom);

        // display the fix variants:
        temporaryLayer = new MendRelationRemoveLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyListener() {

            /* TODO: Use KeyAdapter instead of KeyListener, and make a new private class*/

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void keyPressed(KeyEvent e) {
                display.setDownloadCounter(0);
                if (display.getAbort()) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    return;
                }
                Character typedKey = e.getKeyChar();
                Character typedKeyUpperCase = typedKey.toString().toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    display.setNextIndex(true);
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    Logging.debug(String.valueOf(typedKeyUpperCase));
                    if (typedKeyUpperCase == 'R' || typedKeyUpperCase == '3') {
                        wayIndices.add(0, display.getCurrentIndex());
                    }
                    display.removeWayAfterSelection(wayIndices, typedKeyUpperCase);
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    Logging.debug("ESC");
                    display.setNextIndex(false);
                    display.setSetEnable(true);
                    display.setHalt(true);
                    display.setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
            }
        });
    }

    /**
     *
     * @param keyListener
     */
    private void removeKeyListenerAndTemporaryLayer(KeyListener keyListener) {
        MainApplication.getMap().mapView.removeKeyListener(keyListener);
        MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
    }

    /**
     *
     */
    private class MendRelationAddLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv, paint);
            paintVisitor.drawVariants();
        }
    }

    /**
     *
     */
    private class MendRelationRemoveLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv, paint);
            paintVisitor.drawOptionsToRemoveWays();
        }
    }

    /**
     *
     */
    private class MendRelationAddMultipleLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv, paint);
            paintVisitor.drawMultipleVariants(wayListColoring);
        }
    }
}
