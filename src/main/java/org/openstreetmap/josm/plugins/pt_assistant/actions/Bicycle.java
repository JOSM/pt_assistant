// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mend the relations by going through each way, sorting them and proposing
 * fixes for the gaps that are found
 *
 * @author Ashish Singh
 */
public class Bicycle extends AbstractRelationEditorAction{
  private static final DownloadParams DEFAULT_DOWNLOAD_PARAMS = new DownloadParams();

////////////////Color Datastore///////////////////

  private static final Color[] RAINBOW_COLOR_PALETTE = {
      new Color(0, 255, 0, 150),//GREEN 0
      new Color(255, 0, 0, 150),//RED 1
      new Color(0, 0, 255, 150),//BLUE 2
      new Color(255, 255, 0, 150),//YELLOW 3
      new Color(0, 255, 255, 150),//SKYBLUE 4
      new Color(255, 127,0, 150),//ORANGE 5
      new Color(148, 0, 211, 150),//VIOLET 6
      new Color(255, 255, 255, 150),//WHITE 7
      new Color(169, 169, 169, 210)//GREY 8
  };

  ////////////////Mapping options with colors////////////

  private static final Map<Character, Color> CHARACTER_COLOR_MAP = new HashMap<>();
  static {
      CHARACTER_COLOR_MAP.put('A',RAINBOW_COLOR_PALETTE[0]);
      CHARACTER_COLOR_MAP.put('B', RAINBOW_COLOR_PALETTE[1]);
      CHARACTER_COLOR_MAP.put('C', RAINBOW_COLOR_PALETTE[2]);
      CHARACTER_COLOR_MAP.put('D', RAINBOW_COLOR_PALETTE[3]);
      CHARACTER_COLOR_MAP.put('E', RAINBOW_COLOR_PALETTE[4]);
      CHARACTER_COLOR_MAP.put('1', RAINBOW_COLOR_PALETTE[0]);
      CHARACTER_COLOR_MAP.put('2', RAINBOW_COLOR_PALETTE[1]);
      CHARACTER_COLOR_MAP.put('3', RAINBOW_COLOR_PALETTE[2]);
      CHARACTER_COLOR_MAP.put('4', RAINBOW_COLOR_PALETTE[3]);
      CHARACTER_COLOR_MAP.put('5', RAINBOW_COLOR_PALETTE[4]);
  }

  //////////////assigning current edge with white color and next color with grey///////////

  private static Color CURRENT_WAY_COLOR = RAINBOW_COLOR_PALETTE[7];
  private static Color NEXT_WAY_COLOR = RAINBOW_COLOR_PALETTE[8];

  private static final String I18N_ADD_ONEWAY_BICYCLE_NO_TO_WAY = I18n.marktr("Add oneway:bicycle=no to way");
  private static final String I18N_CLOSE_OPTIONS = I18n.marktr("Close the options");
  private static final String I18N_NOT_REMOVE_WAYS = I18n.marktr("Do not remove ways");
  private static final String I18N_REMOVE_CURRENT_EDGE = I18n.marktr("Remove current edge (white)");
  private static final String I18N_REMOVE_WAYS = I18n.marktr("Remove ways");
  private static final String I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY = I18n.marktr("Remove ways along with previous way");
  private static final String I18N_SKIP = I18n.marktr("Skip");
  private static final String I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS = I18n.marktr("solutions based on other route relations");
  private static final String I18N_TURN_BY_TURN_NEXT_INTERSECTION = I18n.marktr("turn-by-turn at next intersection");

  ////////////////////////Assigning Variables///////////////
  Relation relation = null;
  MemberTableModel memberTableModel = null;
  GenericRelationEditor editor = null;
  HashMap<Way, Integer> waysAlreadyPresent = null;
  List<RelationMember> members = null;
  Way previousWay;
  Way currentWay;
  Way nextWay;
  List<Integer> extraWaysToBeDeleted = null;
  Node currentNode = null;
  boolean noLinkToPreviousWay = true;
  int currentIndex;
  int downloadCounter;
  boolean nextIndex = true;
  boolean setEnable = true;
  boolean firstCall = true;
  boolean halt = false;
  boolean abort = false;
  boolean shorterRoutes = false;
  boolean showOption0 = false;
  boolean onFly = false;
  boolean aroundGaps = false;
  boolean aroundStops = false;
  HashMap<Way, Character> wayColoring;
  HashMap<Character, List<Way>> wayListColoring;
  AbstractMapViewPaintable temporaryLayer = null;
  String notice = null;

  /////////////Editor Access To Bicycle Routing Helper//////////////

  public Bicycle(IRelationEditorActionAccess editorAccess){
    super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
    putValue(SHORT_DESCRIPTION, tr("Routing Helper"));
    new ImageProvider("dialogs/relation", "Bicycle.svg").getResource().attachImageIcon(this, true);
    updateEnabledState();
    editor = (GenericRelationEditor) editorAccess.getEditor();
    memberTableModel = editorAccess.getMemberTableModel();
    this.relation = editor.getRelation();
    editor.addWindowListener(new WindowEventHandler());
  }

  ///////////// bicycle icon enable or disable //////////////

  @Override
  protected void updateEnabledState() {
      final Relation curRel = relation;
      setEnabled(
          curRel != null && setEnable &&
          (
              (curRel.hasTag("route", "bicycle"))
          )
      );
  }

  ///////// Action performed /////////////////
  // on clicking the icon ,action will be performed call the initialization and downloading the area
  //first check relation members are complete or incomplete if its complete then no problem otherwise
  // download member first

  @Override
  public void actionPerformed(ActionEvent e) {
      if (relation.hasIncompleteMembers()) {
          downloadIncompleteRelations();
          new Notification(tr("Downloading incomplete relation members. Kindly wait till download gets over."))
                  .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(3600).show();
      } else {
          init();
      }
  }
  /////////download all incomplete relations from member table////////

  private void downloadIncompleteRelations() {

      List<Relation> parents = Collections.singletonList(relation);

      Future<?> future = MainApplication.worker
              .submit(new DownloadRelationMemberTask(parents,
                      Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
                              .buildSetOfIncompleteMembers(new ArrayList<>(parents)), OsmPrimitive.class),
                      MainApplication.getLayerManager().getEditLayer()));

      MainApplication.worker.submit(() -> {
          try {
              NotificationUtils.downloadWithNotifications(future, tr("Incomplete relations"));
              init();
          } catch (InterruptedException | ExecutionException e1) {
              Logging.error(e1);
          }
      });
  }
/////////////on action call init()/////////////

  public void init(){
    savestateoneditor();
    sortBelowidx(relation.getMembers(),0);
    members = editor.getRelation().getMembers();
    if (halt ==false) {
      updateStates();
      makepanelanddownloadArea();
    }
    else{
      halt = false;
      callNextWay(currentIndex);
    }
  }


////////Download Area //////////////
public void downloadEntireArea(){
	if(abort)
		return;
	DownloadOsmTask task = new DownloadOsmTask();
    List<Way> wayList = getListOfAllWays();

    if (wayList.isEmpty()) {
        callNextWay(currentIndex);
        return;
    }

    String query = getQueryforBicycle();
    Logging.debug(query);

    if (aroundStops || aroundGaps) {
        BoundsUtils.createBoundsWithPadding(wayList, .1).ifPresent(area -> {
            final Future<?> future = task.download(
                new OverpassDownloadReader(area, OverpassDownloadReader.OVERPASS_SERVER.get(), query),
                DEFAULT_DOWNLOAD_PARAMS,
                area,
                null
            );

            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Entire area"));
                    callNextWay(currentIndex);
                } catch (InterruptedException | ExecutionException e1) {
                    Logging.error(e1);
                }
            });
        });
    } else {
        callNextWay(currentIndex);
    }

  System.out.println("I am Download");
}

private List<Way> getListOfAllWays() {
	// TODO Auto-generated method stub
	List<Way>ways = new ArrayList<>();
	for (RelationMember r:members) {
		if(r.isWay()) {
			waysAlreadyPresent.put(r.getWay(),1);
			ways.add(r.getWay());
			System.out.println("hello");
		}
	}
	return ways;
}

private String getQueryforBicycle() {
	// TODO Auto-generated method stub
	final StringBuilder str = new StringBuilder("[timeout:100];\n(\n");
    final String wayFormatterString = "   way(%.6f,%.6f,%.6f,%.6f)\n";
    final String str3 = "[\"highway\"][\"highway\"!=\"motorway\"];\n";

    final List<Node> nodeList = aroundGaps ? getBrokenNodes() : new ArrayList<>();
    if (aroundStops) {
        nodeList.addAll(members.stream().filter(RelationMember::isNode).map(RelationMember::getNode).collect(Collectors.toList()));
    }

    for (final Node n : nodeList) {
        final double maxLat = n.getBBox().getTopLeftLat() + 0.001;
        final double minLat = n.getBBox().getBottomRightLat() - 0.001;
        final double maxLon = n.getBBox().getBottomRightLon() + 0.001;
        final double minLon = n.getBBox().getTopLeftLon() - 0.001;
        str.append(String.format(wayFormatterString, minLat, minLon, maxLat, maxLon))
            .append(str3);

    }
    return str.append(");\n(._;<;);\n(._;>;);\nout meta;").toString();
}

private List<Node> getBrokenNodes() {
	// TODO Auto-generated method stub
	List<Node> lst = new ArrayList<>();
	int idx=0;
	for(RelationMember m:members ) {
		if(m.isWay()) {
			int nexidx = getNextWayIndex(idx);
			if(nexidx < members.size()) {
				Way cur = m.getWay();
				Way nex = members.get(nexidx).getWay();
				if(findNumberOfCommonNode(nex,cur)!=1) {
					lst.add(cur.firstNode());
					lst.add(cur.lastNode());
				}
			}
		}
		idx++;
	}
	return lst;
}
private int findNumberOfCommonNode(Way cur, Way pre) {
	// TODO Auto-generated method stub
	  int count = 0;
      for (Node n1 : cur.getNodes()) {
          for (Node n2 : pre.getNodes()) {
              if (n1.equals(n2))
                  count++;
          }
      }
      return count;
}

////////calling the nextway/////////

public void callNextWay(int cur){
  System.out.println("I am calling nextway");
}

////////update states//////////

public void updateStates(){
  downloadCounter = 0;
  firstCall = false;
  waysAlreadyPresent = new HashMap<>();
  extraWaysToBeDeleted = new ArrayList<>();
  setEnable = false;
  previousWay = null;
  currentWay = null;
  nextWay = null;
  noLinkToPreviousWay = true;
  nextIndex = true;
  shorterRoutes = false;
  showOption0 = false;
  currentIndex = 0;
}

/////////make panel////////////
public void makepanelanddownloadArea(){
  final JPanel panel = new JPanel(new GridBagLayout());
  panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
  final JCheckBox button1 = new JCheckBox("Around Stops");
  final JCheckBox button2 = new JCheckBox("Around Gaps");
  final JCheckBox button3 = new JCheckBox("On the fly");
  button2.setSelected(true);
  button3.setSelected(true);
  panel.add(new JLabel(tr("How would you want the download to take place?")), GBC.eol().fill(GBC.HORIZONTAL));
  panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
  panel.add(button1, GBC.eol().fill(GBC.HORIZONTAL));
  panel.add(button2, GBC.eol().fill(GBC.HORIZONTAL));
  panel.add(button3, GBC.eol().fill(GBC.HORIZONTAL));

  int i = JOptionPane.showConfirmDialog(null, panel);
  if (i == JOptionPane.OK_OPTION) {
    if (button1.isSelected()) {
      aroundStops = true;
    } else if (button2.isSelected()) {
      aroundGaps = true;
    } else if (button3.isSelected()) {
      onFly = true;
    }
    downloadEntireArea();
  }
}

int getNextWayIndex(int idx) {
    int j = members.size();

    for (j = idx + 1; j < members.size(); j++) {
        if (members.get(j).isWay())
            break;
    }
    return j;
}

int getPreviousWayIndex(int idx) {
    int j;

    for (j = idx - 1; j >= 0; j--) {
        if (members.get(j).isWay())
            return j;
    }
    return -1;
}

//////////saving state on editor//////////////////
  public void savestateoneditor(){
    editor.apply();
  }

////////////////////sorting//////////////
  public void sortBelowidx( List<RelationMember>members,int idx){
    RelationSorter relsorter = new RelationSorter();
    final List<RelationMember> subList = members.subList(Math.max(0, idx), members.size());
    final List<RelationMember> sorted = relsorter.sortMembers(subList);
    subList.clear();
    subList.addAll(sorted);
    memberTableModel.fireTableDataChanged();
  }
  void removeTemporarylayers() {
      List<MapViewPaintable> tempLayers = MainApplication.getMap().mapView.getTemporaryLayers();
      for (int i = 0; i < tempLayers.size(); i++) {
          MainApplication.getMap().mapView.removeTemporaryLayer(tempLayers.get(i));
      }
  }

  void defaultStates() {
      currentIndex = 0;
      currentNode = null;
      previousWay = null;
      noLinkToPreviousWay = true;
      nextIndex = true;
      extraWaysToBeDeleted = new ArrayList<>();
      halt = false;
  }

  public void stop() {
      defaultStates();
      nextIndex = false;
      abort = true;
      removeTemporarylayers();
  }
  class WindowEventHandler extends WindowAdapter {
      @Override
      public void windowClosing(WindowEvent e) {
          editor.cancel();
          Logging.debug("close");
          stop();
      }
  }
}
