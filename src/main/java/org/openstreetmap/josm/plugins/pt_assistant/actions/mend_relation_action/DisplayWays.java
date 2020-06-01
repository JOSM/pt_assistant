// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation_action;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.actions.MendRelationAction;
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

    /**
     *
     * @param display
     */
    public DisplayWays(DisplayWaysInterface display) {
        this.display = display;
    }

    void displayFixVariants(List<Way> fixVariants) {
        // find the letters of the fix variants:
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
        display.removeTemporarylayers();

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
                downloadCounter = 0;
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
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(Collections.singletonList(fixVariants.get(idx)));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    shorterRoutes = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

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
        display.removeTemporarylayers();

        if (display.getShowOption0()) {
            return;
        }

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAction.MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
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
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backTrack(currentWay, idx1 + 1);
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        findWayAfterChunk(currentWay);
                        getNextWayAfterBackTrackSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    shorterRoutes = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

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
        display.removeTemporarylayers();

        if (display.getAbort()) {
            return;
        }

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants.stream().flatMap(Collection::stream).collect(Collectors.toList()));

        // display the fix variants:
        temporaryLayer = new MendRelationAction.MendRelationAddMultipleLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
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
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = shorterRoutes ? false : true;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    setEnable = true;
                    shorterRoutes = false;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

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
            wayColoring.put(members.get(wayIndices.get(i)).getWay(), alphabet);
        }

        display.setWayColoring(wayColoring);

        if (notice.equals("vehicle travels against oneway restriction")) {
            if (numeric) {
                allowedCharacters.add('4');
            } else {
                allowedCharacters.add('C');
            }
        }

        // remove any existing temporary layer
        display.removeTemporarylayers();

        if (display.getAbort()) {
            return;
        }

        // zoom to problem:
        final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

        for (Integer i : wayIndices) {
            waysToZoom.add(members.get(i).getWay());
        }

        AutoScaleAction.zoomTo(waysToZoom);

        // display the fix variants:
        temporaryLayer = new MendRelationRemoveLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (display.getAbort()) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    return;
                }
                Character typedKey = e.getKeyChar();
                Character typedKeyUpperCase = typedKey.toString().toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    nextIndex = true;
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    Logging.debug(String.valueOf(typedKeyUpperCase));
                    if (typedKeyUpperCase == 'R' || typedKeyUpperCase == '3') {
                        wayIndices.add(0, currentIndex);
                    }
                    RemoveWayAfterSelection(wayIndices, typedKeyUpperCase);
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    Logging.debug("ESC");
                    nextIndex = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
            }
        });
    }

    private void removeKeyListenerAndTemporaryLayer(KeyListener keyListener) {
        MainApplication.getMap().mapView.removeKeyListener(keyListener);
        MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
    }

    private class MendRelationAddLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
            paintVisitor.drawVariants();
        }
    }

    private class MendRelationRemoveLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
            paintVisitor.drawOptionsToRemoveWays();
        }
    }

}
