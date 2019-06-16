// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.NodePositionComparator;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.validation.PaintVisitor;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
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
      new Color(169, 169, 169, 210),//GREY 8
      new Color(239, 167, 222,220)
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
  private static Color NEXT_WAY_COLOR = RAINBOW_COLOR_PALETTE[9];

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
  Way lastForWay;
  Way lastBackWay;
  List<Integer> extraWaysToBeDeleted = null;
  Node currentNode = null;
  Node pseudocurrentNode = null;
  boolean noLinkToPreviousWay = true;
  int currentIndex;
  int downloadCounter;
  int cnt =0;
  int brokenidx =0;
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
  HashMap<Node, Integer> Isthere = new HashMap<>();
  HashMap<Way, Integer> IsWaythere = new HashMap<>();
  List<WayConnectionType> links;
  WayConnectionType link;
  WayConnectionType prelink;
  Node brokenNode;
  List<List<Way>> directroutes;
  NodePositionComparator dist = new NodePositionComparator();
  WayConnectionTypeCalculator connectionTypeCalculator = new WayConnectionTypeCalculator();

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
    members.removeIf(m -> !m.isWay());
    links = connectionTypeCalculator.updateLinks(members);
    if (halt ==false) {
      updateStates();
      getListOfAllWays();
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

    if (aroundGaps) {
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

  // System.out.println("I am Download");
}

private List<Way> getListOfAllWays() {
	// TODO Auto-generated method stub
	List<Way> ways = new ArrayList<>();
	for (RelationMember r:members) {
		if(r.isWay()) {
			waysAlreadyPresent.put(r.getWay(),1);
			ways.add(r.getWay());
			// System.out.println("hello");
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

////////calling the nextway /////////
//this function has to iterate over all members of relation table doesn't matter they are broken or not
//so after every filtering this function has to be called with currentIndex+1
public void callNextWay(int idx){
	Logging.debug("Index + " + idx);
	downloadCounter++;
	if(idx<members.size() && members.get(idx).isWay()) {
		if (currentNode == null)
            noLinkToPreviousWay = true;

    int nexidx = getNextWayIndex(idx);

    if(nexidx>= members.size()) {
			deleteExtraWays();
      makeNodesZeros();
			return;
		}

    link = links.get(nexidx);
    prelink = links.get(idx);

    if(prelink.isOnewayLoopBackwardPart ){
      lastBackWay = members.get(idx).getWay();
    }
    if(prelink.isOnewayLoopForwardPart ){
      lastForWay = members.get(idx).getWay();
    }

    Way way = members.get(idx).getWay();
    if(IsWaythere.get(way)==null){
    for(Node nod:way.getNodes()){
      if(Isthere.get(nod)==null || Isthere.get(nod)==0){
        Isthere.put(nod,1);
      }
      else{
        Isthere.put(nod,Isthere.get(nod)+1);
      }
 	   }
     IsWaythere.put(way,1);
   }
     System.out.println(way.firstNode().getUniqueId() + " ---- " + Isthere.get(way.firstNode()));
     System.out.println(way.lastNode().getUniqueId() + " ---- " + Isthere.get(way.lastNode()));
    this.nextWay = members.get(nexidx).getWay();

    Node node = checkVaildityOfWays(way,nexidx);

		if (abort || nextIndex) {
              nextIndex = false;
              return;
          } else {
              nextIndex = true;
         }
		if(noLinkToPreviousWay) {
			if(node==null) {
				currentWay = way;
				nextIndex = false;
				findNextWayBeforeDownload(way,way.firstNode(),way.lastNode());
			}
			else {
				noLinkToPreviousWay = false;
				previousWay = way;
				currentNode = getOtherNode(nextWay,node);
				nextIndex = false;
				downloadAreaAroundWay(way);
			}
		}
		else {
			if(node==null) {
        if(members.get(currentIndex).getRole().equals("forward") && prelink.isOnewayLoopBackwardPart){
          node = way.lastNode();
        }
        else if(members.get(currentIndex).getRole().equals("backward") && prelink.isOnewayLoopForwardPart){
          node = way.lastNode();
        }
        else {
          if(currentIndex-1>=0){
            Way prevw = members.get(currentIndex-1).getWay();
            if(prevw.firstNode().equals(way.lastNode()) || prevw.lastNode().equals(way.lastNode())){
              node = way.lastNode();
            }
            else{
              node = way.firstNode();
            }
            System.out.println("I am in with node "+node.getUniqueId());
          }
        }
		       if(link.isOnewayLoopBackwardPart){
		          previousWay = way;
		          nextIndex = false;
              if(Isthere.get(way.firstNode())!=null && Isthere.get(way.firstNode())!=0){
                node = way.firstNode();
              }
              else{
                node = way.lastNode();
              }
		          downloadAreaAroundWay(way);
		         }
		        else{
              currentNode = getOtherNode(way,node);
		  				currentWay = way;
		  				nextIndex = false;
		  				findNextWayBeforeDownload(way,currentNode);
		         }
					}
					else {
						previousWay = way;
						currentNode = getOtherNode(nextWay,node);
						nextIndex = false;
						downloadAreaAroundWay(way);
					}
		}

	}
	if(abort)
		return;

	if(idx>= members.size()-1) {
		deleteExtraWays();
    makeNodesZeros();
	}else if(nextIndex) {
		callNextWay(++currentIndex);
	}

}
boolean checkOneWaySatisfiability(Way way, Node node) {
    String[] acceptedTags = new String[] {"yes", "designated" };

    if((link.isOnewayLoopBackwardPart && relation.hasTag("route", "bicycle"))||prelink.isOnewayLoopBackwardPart){
              // System.out.println("yo bro whats going on...");
              return true;
            }

    if ((way.hasTag("oneway:bicycle", acceptedTags))
            && way.lastNode().equals(node) && relation.hasTag("route", "bicycle"))
        return false;


    if (!isNonSplitRoundAbout(way) && way.hasTag("junction", "roundabout")) {
        if (way.lastNode().equals(node))
            return false;
    }

    if (RouteUtils.isOnewayForBicycles(way) == 0)
        return true;
    else if (RouteUtils.isOnewayForBicycles(way) == 1 && way.lastNode().equals(node))
        return false;
    else if (RouteUtils.isOnewayForBicycles(way) == -1 && way.firstNode().equals(node))
        return false;

    return true;
}
private Node checkVaildityOfWays(Way way, int nexidx) {
	// TODO Auto-generated method stub
	boolean nexWayDelete = false;
	Node node = null;
	nextIndex = false;
	notice = null;
	final NodePair commonEndNodes = WayUtils.findCommonFirstLastNodes(nextWay, way);
    if (commonEndNodes.getA() != null && commonEndNodes.getB() != null) {
        nexWayDelete = true;
        notice = "Multiple common nodes found between current and next way";
    } else if (commonEndNodes.getA() != null) {
        node = commonEndNodes.getA();
    } else if (commonEndNodes.getB() != null) {
        node = commonEndNodes.getB();
    } else {
        // the nodes can be one of the intermediate nodes
        for (Node n : nextWay.getNodes()) {
            if (way.getNodes().contains(n)) {
                node = n;
                currentNode = n;
            }
        }
    }
    if(node!=null && isRestricted(nextWay,way,node)) {
    	nexWayDelete = true;
    }
    if(isNonSplitRoundAbout(way)) {
        nexWayDelete = false;
        for (Node n : way.getNodes()) {
            if (nextWay.firstNode().equals(n) || nextWay.lastNode().equals(n)) {
                node = n;
                currentNode = n;
            }
        }
    }

    if (isNonSplitRoundAbout(nextWay)) {
        nexWayDelete = false;
    }

    if (node != null && !checkOneWaySatisfiability(nextWay, node)) {
      // System.out.println(link.isOnewayLoopBackwardPart);
        nexWayDelete = true;
        notice = "bicycle travels against oneway restriction";
    }

    if (nexWayDelete) {
        currentWay = way;
        nextIndex = true;
        removeWay(nexidx);
        return null;
    }

    nextIndex = false;
    return node;

}

private void findNextWayBeforeDownload(Way way, Node node1, Node node2) {
    nextIndex = false;
    AutoScaleAction.zoomTo(Collections.singletonList(way));
    downloadAreaAroundWay(way, node1, node2);
}

private void findNextWayBeforeDownload(Way way, Node currNode) {
	// TODO Auto-generated method stub
	nextIndex = false;
    DataSet ds = MainApplication.getLayerManager().getEditDataSet();
    ds.setSelected(way);
    AutoScaleAction.zoomTo(Collections.singletonList(way));
    downloadAreaAroundWay(way, currNode, null);
}

private void downloadAreaAroundWay(Way way) {
	// TODO Auto-generated method stub
	if(abort)
		return;
	if(downloadCounter >200 && onFly) {
		downloadCounter=0;

    DownloadOsmTask task = new DownloadOsmTask();
    BoundsUtils.createBoundsWithPadding(way.getBBox(), .1).ifPresent(area -> {
        Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

        MainApplication.worker.submit(() -> {
            try {
                NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (2)");
                if (currentIndex >= members.size() - 1) {
                    deleteExtraWays();
                } else {
                  // System.out.println("Hello printing the current index:" + currentIndex);
                  callNextWay(++currentIndex);
                }
            } catch (InterruptedException | ExecutionException e1) {
                Logging.error(e1);
            }
        });
    });
	}else {
		if(currentIndex>=members.size()-1) {
			deleteExtraWays();
		}
		else {
			callNextWay(++currentIndex);
		}
	}
}

void downloadAreaAroundWay(Way way, Node node1, Node node2) {
    if (abort)
        return;

    if ((downloadCounter > 160 || way.isOutsideDownloadArea() || way.isNew()) && onFly) {
        downloadCounter = 0;

        DownloadOsmTask task = new DownloadOsmTask();

        BoundsUtils.createBoundsWithPadding(way.getBBox(), .4).ifPresent(area -> {
            Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (1)");
                    findNextWayAfterDownload(way, node1, node2);
                } catch (InterruptedException | ExecutionException e1) {
                    Logging.error(e1);
                }
            });
        });
    } else {
        findNextWayAfterDownload(way, node1, node2);
    }
}

void downloadAreaAroundWay(Way way, Way prevWay, List<Way> ways) {
    if (abort)
        return;

    if ((downloadCounter > 160 || way.isOutsideDownloadArea() || way.isNew()) && onFly) {
        downloadCounter = 0;

        DownloadOsmTask task = new DownloadOsmTask();

        BoundsUtils.createBoundsWithPadding(way.getBBox(), .2).ifPresent(area -> {
            Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (3)");
                    goToNextWay(way, prevWay, ways);
                } catch (InterruptedException | ExecutionException e1) {
                    Logging.error(e1);
                }
            });
        });
    } else {
        goToNextWay(way, prevWay, ways);
    }
}

private Way findNextWayAfterDownload(Way way, Node node1, Node node2) {
	// TODO Auto-generated method stub
	currentWay = way;
	if(abort)
		return null;
	List<Way> parentWays =findNextWay(way,node1);

  if(node2!=null) {
		parentWays.addAll(findNextWay(way,node2));
	}
	directroutes = getDirectRouteBetweenWays(currentWay,nextWay);
	if(directroutes == null || directroutes.size()==0) {
		showOption0 =false;
	}
	else {
		showOption0 = true;
	}
	 if(directroutes!=null  && directroutes.size() > 0 && !shorterRoutes && parentWays.size() > 0 && notice == null) {
	 	displayFixVariantsWithOverlappingWays(directroutes);
     return null ;
	 }
	if(parentWays.size()==1) {
		goToNextWay(parentWays.get(0),way,new ArrayList<>());
	}else if(parentWays.size()>1) {
		nextIndex = false;
    System.out.println("parents more than one: " + way.getUniqueId() );
    System.out.println("iteration on first node " +Isthere.get(way.firstNode()));
    System.out.println("iteration on last node "+ Isthere.get(way.lastNode()));
		displayFixVariants(parentWays);
	}
	else {
		nextIndex = true;
        if (currentIndex >= members.size() - 1) {
            deleteExtraWays();
            makeNodesZeros();
        } else {
            callNextWay(++currentIndex);
            return null;
        }
	}
	return null;
}

private List<List<Way>> getDirectRouteBetweenWays(Way current, Way next) {
  System.out.println("I am inside DirectRouteBetweenWays by currentway " + current.getUniqueId());
	//trying to find the other route relations which can connect these ways
    List<List<Way>> list = new ArrayList<>();
    List<Relation> r1;
    List<Relation> r2;
    try {
        r1 = new ArrayList<>(Utils.filteredCollection(current.getReferrers(), Relation.class));
        r2 = new ArrayList<>(Utils.filteredCollection(next.getReferrers(), Relation.class));
    } catch (Exception e) {
        return list;
    }

    if (r1 == null || r2 == null)
        return list;

    List<Relation> rel = new ArrayList<>();
    //checking whether relations you are getting are bicycle routes or not
    String value = relation.get("route");

    for (Relation R1 : r1) {
        if (r2.contains(R1) && value.equals(R1.get("route")))
            rel.add(R1);
    }
    rel.remove(relation);

    for (Relation r : rel) {
        List<Way> lst = searchWayFromOtherRelations(r, current, next,false);
        boolean alreadyPresent = false;
        if (lst != null) {
            for (List<Way> l : list) {
                if (l.containsAll(lst))
                    alreadyPresent = true;
            }
            if (!alreadyPresent)
                list.add(lst);
        }
        lst = searchWayFromOtherRelations(r, current, next,true);

        if (lst != null) {
          // System.out.println("My list is not null "+ lst.size());
            alreadyPresent = false;
            for (List<Way> l : list) {
                if (l.containsAll(lst))
                    alreadyPresent = true;
            }
            if (!alreadyPresent)
                list.add(lst);
        }
    }

    return list;
}

List<Way> searchWayFromOtherRelations(Relation r, Way current, Way next,boolean reverse) {
    List<RelationMember> member = r.getMembers();
    List<Way> lst = new ArrayList<>();
    boolean canAdd = false;
    Way prev = null;
    int flag=0;
    for (int i = 0; i < member.size(); i++) {
        if (member.get(i).isWay()) {
            Way w = member.get(i).getWay();
            if(!reverse){
              if (w.equals(current)) {
                  lst.clear();
                  canAdd = true;
                  prev = w;
              } else if (w.equals(next) && lst.size() > 0) {
                  return lst;
              } else if (canAdd) {
                  if (findNumberOfCommonNode(w, prev) != 0) {
                      lst.add(w);
                      prev = w;
                  } else {
                      break;
                  }
              }
            }
            else{
              if (w.equals(next)) {
                  lst.clear();
                  canAdd = true;
                  prev = w;
              } else if (w.equals(current) && lst.size() > 0) {
                  Collections.reverse(lst);
                  return lst;
              } else if (canAdd) {
                  // not valid in reverse if it is oneway or part of roundabout
                  if (findNumberOfCommonNode(w, prev) != 0 && RouteUtils.isOnewayForBicycles(w) == 0
                      && !isSplitRoundAbout(w)) {
                      lst.add(w);
                      prev = w;
                  } else {
                      break;
                  }
              }
          }
        }
    }
    return null;
}

void goToNextWay(Way way, Way prevWay, List<Way> wayList) {
    List<List<Way>> lst = new ArrayList<>();
    System.out.println("gotonextway preway "+ prevWay.getUniqueId());
    System.out.println("gotonextway curr"+ way.getUniqueId());
    previousWay = prevWay;
    Node node1 = null;
    for (Node n : way.getNodes()) {
        if (prevWay.getNodes().contains(n)) {
            node1 = n;
            break;
        }
    }

    if (node1 == null) {
        lst.add(wayList);
        displayFixVariantsWithOverlappingWays(lst);
        return;
    }

    // check if the way equals the next way, if so then don't add any new ways to the list
    if (way == nextWay) {
        lst.add(wayList);
        displayFixVariantsWithOverlappingWays(lst);
        return;
    }

    Node node = getOtherNode(way, node1);
    // System.out.println("goToNextWay node new " + node.getUniqueId());
    wayList.add(way);
    List<Way> parents = node.getParentWays();
    parents.remove(way);

    // if the ways directly touch the next way
    if (way.isFirstLastNode(nextWay.firstNode()) || way.isFirstLastNode(nextWay.lastNode())) {
        lst.add(wayList);
        displayFixVariantsWithOverlappingWays(lst);
        return;
    }

    // if next way turns out to be a roundabout
    if (nextWay.containsNode(node) && nextWay.hasTag("junction", "roundabout")) {
        lst.add(wayList);
        displayFixVariantsWithOverlappingWays(lst);
        return;
    }

    // remove all the invalid ways from the parent ways
    parents = removeInvalidWaysFromParentWays(parents, node, way);

    if (parents.size() == 1) {
        // if (already the way exists in the ways to be added
        if (wayList.contains(parents.get(0))) {
            lst.add(wayList);
            displayFixVariantsWithOverlappingWays(lst);
            return;
        }
        downloadAreaAroundWay(parents.get(0), way, wayList);
        return;
    } else if (parents.size() > 1) {
        // keep the most probable option s option A
        Way minWay = parents.get(0);
        double minLength = findDistanceBetweenWays(minWay, nextWay, node);
        for (int k = 1; k < parents.size(); k++) {
            double length = findDistanceBetweenWays(parents.get(k), nextWay, node);
            if (minLength > length) {
                minLength = length;
                minWay = parents.get(k);
            }
        }
        parents.remove(minWay);
        parents.add(0, minWay);

        // add all the list of ways to list of list of ways
        for (int i = 0; i < parents.size(); i++) {
            List<Way> wl = new ArrayList<>(wayList);
            wl.add(parents.get(i));
            lst.add(wl);
        }

        displayFixVariantsWithOverlappingWays(lst);
        return;
    } else {
        lst.add(wayList);
        displayFixVariantsWithOverlappingWays(lst);
        return;
    }
}

private List<Way> findNextWay(Way way, Node node) {
	// TODO Auto-generated method stub
	List<Way> parentWays = node.getParentWays();
  parentWays = removeInvalidWaysFromParentWays(parentWays, node, way);

  // if the way is a roundabout but it has not find any suitable option for next
    // way, look at parents of all nodes
	if(way.hasTag("junction","roundabout") && parentWays.size()==0) {
		for(Node n: way.getNodes()) {
			parentWays.addAll(removeInvalidWaysFromParentWaysOfRoundabouts(n.getParentWays(),n,way));
		}
	}
	Way frontWay = parentWays.stream().filter(it->checkIfWayConnectsToNextWay(it,0,node)).findFirst().orElse(null);

	if (frontWay == null && parentWays.size() > 0) {
        // System.out.println("I am inside of null front way check me out");
        Way minWay = parentWays.get(0);
        double minLength = findDistanceBetweenWays(minWay, nextWay, node);
        for (int i = 1; i < parentWays.size(); i++) {
            double length = findDistanceBetweenWays(parentWays.get(i), nextWay, node);
            if (minLength > length) {
                minLength = length;
                minWay = parentWays.get(i);
            }
        }
        frontWay = minWay;
    }
    // System.out.println("checking frontway " + frontWay.getUniqueId());
    if (frontWay != null) {
        parentWays.remove(frontWay);
        parentWays.add(0, frontWay);
    }

    return parentWays;
}

List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way) {
    parentWays.remove(way);
    if (abort)
        return null;
    List<Way> waysToBeRemoved = new ArrayList<>();
    // check if any of the way is joining with its intermediate nodes
    List<Way> waysToBeAdded = new ArrayList<>();
    for (Way w : parentWays) {
        if (node != null && !w.isFirstLastNode(node)) {
          // System.out.println("Ouch");
            Way w1 = new Way();
            Way w2 = new Way();

            List<Node> lst1 = new ArrayList<>();
            List<Node> lst2 = new ArrayList<>();
            boolean firsthalf = true;

            for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
                if (firsthalf) {
                    lst1.add(nodePair.a);
                    lst1.add(nodePair.b);
                    if (nodePair.b.equals(node))
                        firsthalf = false;
                } else {
                    lst2.add(nodePair.a);
                    lst2.add(nodePair.b);
                }
            }

            w1.setNodes(lst1);
            w2.setNodes(lst2);

            w1.setKeys(w.getKeys());
            w2.setKeys(w.getKeys());

            if (!w.hasTag("junction", "roundabout")) {
                waysToBeRemoved.add(w);
                waysToBeAdded.add(w1);
                waysToBeAdded.add(w2);
            }
            // if(IsWaythere.get(w))
        }
    }

    // check if one of the way's intermediate node equals the first or last node of next way,
    // if so then break it(finally split in method getNextWayAfterSelection if the way is chosen)
    for (Way w : parentWays) {
        Node nextWayNode = null;
        if (w.getNodes().contains(nextWay.firstNode()) && !w.isFirstLastNode(nextWay.firstNode())) {
            nextWayNode = nextWay.firstNode();
        } else if (w.getNodes().contains(nextWay.lastNode()) && !w.isFirstLastNode(nextWay.lastNode())) {
            nextWayNode = nextWay.lastNode();
        }

        if (nextWayNode != null) {
          // System.out.println("wooow");
            Way w1 = new Way();
            Way w2 = new Way();

            List<Node> lst1 = new ArrayList<>();
            List<Node> lst2 = new ArrayList<>();
            boolean firsthalf = true;

            for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
                if (firsthalf) {
                    lst1.add(nodePair.a);
                    lst1.add(nodePair.b);
                    if (nodePair.b.equals(nextWayNode))
                        firsthalf = false;
                } else {
                    lst2.add(nodePair.a);
                    lst2.add(nodePair.b);
                }
            }

            w1.setNodes(lst1);
            w2.setNodes(lst2);

            w1.setKeys(w.getKeys());
            w2.setKeys(w.getKeys());

            if (!w.hasTag("junction", "roundabout")) {
                waysToBeRemoved.add(w);
                if (w1.containsNode(node))
                    waysToBeAdded.add(w1);
                if (w2.containsNode(node))
                    waysToBeAdded.add(w2);
            }
        }
    }
    parentWays.addAll(waysToBeAdded);
    // one way direction doesn't match
    parentWays.stream()
        .filter(it -> WayUtils.isOneWay(it) && !checkOneWaySatisfiability(it, node))
        .forEach(waysToBeRemoved::add);

    parentWays.removeAll(waysToBeRemoved);
    // System.out.println("inval:"+waysToBeRemoved.size());
    waysToBeRemoved.clear();

    // check if both nodes of the ways are common, then remove
    for (Way w : parentWays) {
        if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) != 1 && !w.hasTag("junction", "roundabout")) {
            waysToBeRemoved.add(w);
        }
    }
    // parentWays.stream()
    //     .filter(it -> IsWaythere.get(it)!=null)
    //     .forEach(waysToBeRemoved::add);
    // for(int i=0;i<waysToBeRemoved.size();i++){
    //   System.out.println("invalidparentId1:"+waysToBeRemoved.get(i).getUniqueId());
    // }

    // check if any of them belong to roundabout, if yes then show ways accordingly
    parentWays.stream()
        .filter(it -> it.hasTag("junction", "roundabout") && WayUtils.findNumberOfCommonFirstLastNodes(way, it) == 1 && it.lastNode().equals(node))
        .forEach(waysToBeRemoved::add);


    // for(int i=0;i<waysToBeRemoved.size();i++){
    //   System.out.println("invalidparentId2:"+waysToBeRemoved.get(i).getUniqueId());
    // }
    // check mode of transport, also check if there is no loop
    if (relation.hasTag("route", "bicycle")) {
        parentWays.stream().filter(it -> !WayUtils.isSuitableForBicycle(it)).forEach(waysToBeRemoved::add);
    }
    // for(int i=0;i<waysToBeRemoved.size();i++){
    //   System.out.println("invalidparentId3test:"+waysToBeRemoved.get(i).getUniqueId());
    // }
    parentWays.stream().filter(it -> it.equals(previousWay)).forEach(waysToBeRemoved::add);

    parentWays.removeAll(waysToBeRemoved);
    waysToBeRemoved.clear();

    // check restrictions
    parentWays.stream().filter(it -> isRestricted(it, way, node)).forEach(waysToBeRemoved::add);


    for(int i=0;i<waysToBeRemoved.size();i++){
      System.out.println("invalidparentId4:"+waysToBeRemoved.get(i).getUniqueId());
    }
    for(Way w:parentWays){
      if(IsWaythere.get(w)!=null){
          waysToBeRemoved.add(w);
      }
    }
    parentWays.removeAll(waysToBeRemoved);
    return parentWays;
}

List<Way> removeInvalidWaysFromParentWaysOfRoundabouts(List<Way> parents, Node node, Way way) {
    List<Way> parentWays = parents;
    parentWays.remove(way);
    if (abort)
        return null;
    List<Way> waysToBeRemoved = new ArrayList<>();

    // one way direction doesn't match
    for (Way w : parentWays) {
        if (w.isOneway() != 0) {
            if (!checkOneWaySatisfiability(w, node)) {
                waysToBeRemoved.add(w);
            }
        }
    }
// check if any of the way is joining with its intermediate nodes
    // check if any of them belong to roundabout, if yes then show ways accordingly
    for (Way w : parentWays) {
        if (w.hasTag("junction", "roundabout")) {
            if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) == 1) {
                if (w.lastNode().equals(node)) {
                    waysToBeRemoved.add(w);
                }
            }
        }
    }

    // check mode of transport, also check if there is no loop
    for (Way w : parentWays) {
        if (!WayUtils.isSuitableForBicycle(w)) {
            waysToBeRemoved.add(w);
        }

        if (w.equals(previousWay)) {
            waysToBeRemoved.add(w);
        }
    }

    parentWays.removeAll(waysToBeRemoved);
    return parentWays;
}

boolean checkIfWayConnectsToNextWay(Way way, int count, Node node) {

    if (count < 80) {
        if (way.equals(nextWay))
            return true;
        // System.out.println("way ids for checks are : " + way.getUniqueId() );
        // check if way;s intermediate node is next way's first or last node
        if (way.getNodes().contains(nextWay.firstNode()) || way.getNodes().contains(nextWay.lastNode()))
            return true;
        node = getOtherNode(way, node);
        List<Way> parents = node.getParentWays();
        // System.out.println("node Id is: "+ node.getUniqueId() + "Parent size is " + parents.size());
        if (parents.size() != 1)
            return false;
        else
            way = parents.get(0);

        count += 1;
        if (checkIfWayConnectsToNextWay(way, count, node))
            return true;
    }
    return false;
}

double findDistanceBetweenWays(Way way, Way nextWay, Node node) {
    Node otherNode = getOtherNode(way, node);
    double Lat = (nextWay.firstNode().lat() + nextWay.lastNode().lat()) / 2;
    double Lon = (nextWay.firstNode().lon() + nextWay.lastNode().lon()) / 2;
    double ans =(otherNode.lat() - Lat) * (otherNode.lat() - Lat) + (otherNode.lon() - Lon) * (otherNode.lon() - Lon);
    // System.out.println("way id: " +way.getUniqueId()+" distance " + ans);
    return (otherNode.lat() - Lat) * (otherNode.lat() - Lat) + (otherNode.lon() - Lon) * (otherNode.lon() - Lon);
}


private void removeWay(int nexidx) {
	// TODO Auto-generated method stub
	List<Integer> Int = new ArrayList<>();
	List<Way> lst = new ArrayList<>();
	Way way = members.get(nexidx).getWay();
	Int.add(nexidx);
	lst.add(way);
	if(WayUtils.isOneWay(way)) {
		while(true) {
			int k = getNextWayIndex(nexidx);

			if(k==-1 || k>=members.size()) {
				break;
			}

			Way nway = members.get(k).getWay();

			if(!WayUtils.isOneWay(nway)) {
				break;
			}

			if (WayUtils.findNumberOfCommonFirstLastNodes(nway, way) == 0) {
				break;
			}
			nexidx =k;
			lst.add(nway);
			Int.add(k);
		}
	}
	DataSet ds = MainApplication.getLayerManager().getEditDataSet();
    ds.setSelected(lst);

    downloadAreaBeforeRemovalOption(lst, Int);

}

void downloadAreaBeforeRemovalOption(List<Way> wayList, List<Integer> Int) {
    if (abort)
        return;

    if (!onFly) {
        displayWaysToRemove(Int);
    }

    downloadCounter = 0;

    DownloadOsmTask task = new DownloadOsmTask();

    BoundsUtils.createBoundsWithPadding(wayList, .4).ifPresent( area -> {
        Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

        MainApplication.worker.submit(() -> {
            try {
                NotificationUtils.downloadWithNotifications(future, tr("Area before removal"));
                displayWaysToRemove(Int);
            } catch (InterruptedException | ExecutionException e1) {
                Logging.error(e1);
            }
        });
    });
}

void displayWaysToRemove(List<Integer> wayIndices) {

    // find the letters of the fix variants:
    char alphabet = 'A';
    boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
    if (numeric)
        alphabet = '1';
    wayColoring = new HashMap<>();
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

    if (notice.equals("vehicle travels against oneway restriction")) {
        if (numeric) {
            allowedCharacters.add('4');
        } else {
            allowedCharacters.add('C');
        }
    }

    // remove any existing temporary layer
    removeTemporarylayers();

    if (abort)
        return;

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
            if (abort) {
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

void displayFixVariants(List<Way> fixVariants) {
    // find the letters of the fix variants:
    char alphabet = 'A';
    boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
    wayColoring = new HashMap<>();
    final List<Character> allowedCharacters = new ArrayList<>();
    // System.out.println("sdddddddddddddddddddd");
    System.out.println("cuuuuuuuurentway : "+ currentWay.getUniqueId());
    System.out.println("nexxxxxxtway : "+ nextWay.getUniqueId());
    if (numeric) {
        alphabet = '1';
        allowedCharacters.add('7');
        if (showOption0)
            allowedCharacters.add('0');
        allowedCharacters.add('9');
    } else {
        allowedCharacters.add('S');
        if (showOption0)
            allowedCharacters.add('W');
        allowedCharacters.add('Q');
    }

    for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
        // if(IsWaythere.get(fixVariants.get(i))==null){
          allowedCharacters.add(alphabet);
          wayColoring.put(fixVariants.get(i), alphabet);
          alphabet++;
        // }
    }

    // remove any existing temporary layer
    removeTemporarylayers();

    if (abort)
        return;

    // zoom to problem:
    AutoScaleAction.zoomTo(fixVariants);
    // display the fix variants:
    temporaryLayer = new MendRelationAddLayer();
    MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);
    // System.out.println("hollllllllllllaaaaaaaaa");
    // // add the key listener:
    MainApplication.getMap().mapView.requestFocus();
    // for(Way x:fixVariants){
    //   System.out.println("my fix variant is : " +x.getUniqueId());
    // }
    MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
//      System.out.println("hiiiiiiiiiiiiiiiiii");
        @Override
        public void keyPressed(KeyEvent e) {
            downloadCounter = 0;
            if (abort) {
                removeKeyListenerAndTemporaryLayer(this);
                return;
            }
            Character typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
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

void displayFixVariantsWithOverlappingWays(List<List<Way>> fixVariants) {
    // find the letters of the fix variants:
    char alphabet = 'A';
    boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
    wayListColoring = new HashMap<>();
    final List<Character> allowedCharacters = new ArrayList<>();

    if (numeric) {
        alphabet = '1';
        allowedCharacters.add('7');
        if (showOption0)
            allowedCharacters.add('0');
        allowedCharacters.add('9');
    } else {
        allowedCharacters.add('S');
        if (showOption0)
            allowedCharacters.add('W');
        allowedCharacters.add('Q');
    }

    for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
      // if(IsWaythere.get(fixVariants.get(i))==null){
        allowedCharacters.add(alphabet);
        wayListColoring.put(alphabet, fixVariants.get(i));
        alphabet++;
      // }
    }

    // remove any existing temporary layer
    removeTemporarylayers();

    if (abort)
        return;

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
            downloadCounter = 0;
            if (abort) {
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

void getNextWayAfterSelection(List<Way> ways) {
  int flag=0;
    if (ways != null) {
        /*
         * check if the selected way is not a complete way but rather a part of a parent
         * way, then split the actual way (the partial way was created in method
         * removeViolatingWaysFromParentWays but here we are finally splitting the
         * actual way and adding to the relation) here there can be 3 cases - 1) if the
         * current node is the node splitting a certain way 2) if next way's first node
         * is splitting the way 3) if next way's last node is splitting the way
         */
        Logging.debug("Number of ways " + ways.size());
        int ind = currentIndex;
        Way prev = currentWay;
        for (int i = 0; i < ways.size(); i++) {
            Way w = ways.get(i);
            Way w1 = null;
            List<Node> breakNode = null;
            boolean brk = false;
            if (w.isNew()) {
                if (prev != null) {
                    List<Way> par = new ArrayList<>(prev.firstNode().getParentWays());
                    par.addAll(prev.lastNode().getParentWays());
                    for (Way v : par) {
                        if (v.getNodes().containsAll(w.getNodes())) {
                            if (w.equals(v)) {
                                System.out.println(1);
                                addNewWays(Collections.singletonList(v), ind);
                                prev = v;
                                ind++;
                                brk = true;
                                break;
                            } else {
                                List<Node> temp = new ArrayList<>();
                                if (!v.isFirstLastNode(w.firstNode()))
                                    temp.add(w.firstNode());
                                if (!v.isFirstLastNode(w.lastNode()))
                                    temp.add(w.lastNode());
                                if (temp.size() != 0) {
                                    w1 = v;
                                    breakNode = new ArrayList<>(temp);
                                    break;
                                }
                            }
                        }
                    }
                }

                // check if the new way is part of one of the parentWay of the nextWay's first
                // node
                for (Way v : nextWay.firstNode().getParentWays()) {
                    if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
                        if (!w.equals(v) && !v.isFirstLastNode(nextWay.firstNode())) {
                            w1 = v;
                            breakNode = Collections.singletonList(nextWay.firstNode());
                            break;
                        } else if (w.equals(v)) {
                          System.out.println(2);
                            addNewWays(Collections.singletonList(v), ind);
                            prev = v;
                            ind++;
                            brk = true;
                            break;
                        }
                    }
                }

                // check if the new way is part of one of the parentWay of the nextWay's first
                for (Way v : nextWay.lastNode().getParentWays()) {
                    if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
                        if (!w.equals(v) && !v.isFirstLastNode(nextWay.lastNode())) {
                            w1 = v;
                            breakNode = Collections.singletonList(nextWay.lastNode());
                            break;
                        } else if (w.equals(v)) {
                          System.out.println(3);
                            addNewWays(Collections.singletonList(v), ind);
                            ind++;
                            prev = v;
                            brk = true;
                            break;
                        }
                    }
                }

                if (w1 != null && !brk) {
                    SplitWayCommand result = SplitWayCommand.split(w1, breakNode, Collections.emptyList());
                    if (result != null) {
                        UndoRedoHandler.getInstance().add(result);
                        if (result.getOriginalWay().getNodes().contains(w.firstNode())
                                && result.getOriginalWay().getNodes().contains(w.lastNode()))
                            w = result.getOriginalWay();
                        else
                            w = result.getNewWays().get(0);
                        System.out.println(4);
                        addNewWays(Collections.singletonList(w), ind);
                        prev = w;
                        ind++;
                    }

                } else if (!brk) {
                    Logging.debug("none");
                }
            } else {
              System.out.println(5);
             // System.out.println("printing current index: " + currentIndex + "and after what index member will be added "+ ind);
                addNewWays(Collections.singletonList(w), ind);
                prev = w;
                ind++;
                if(findNextWay(w,currentNode).size()>=2){
                  break;
                }
            }
        }
        Way way = members.get(currentIndex).getWay();
        Way nextw = members.get(currentIndex + 1).getWay();
        Node n = WayUtils.findCommonFirstLastNode(nextw, way, currentNode).orElse(null);
        System.out.println("current Way: " + way.getUniqueId() + "nextWay: "+nextw.getUniqueId()+ "currentNode: "+currentNode.getUniqueId());

        // System.out.println("current Way: " + way.getUniqueId() + "nextWay: "+nextWay.getUniqueId()+ "currentNode: "+currentNode.getUniqueId());
        if(n !=null){
          currentNode = getOtherNode(nextw, n);
          // System.out.println("when adding way common id " + n.getUniqueId() );
        }
        else{
          Node node1 = currentWay.firstNode();
          Node node2 = currentWay.lastNode();
          if(nextw.firstNode().equals(node1) || nextw.lastNode().equals(node1)){
            currentNode = getOtherNode(nextw, node1);
          }
          else{
            currentNode = getOtherNode(nextw, node2);
          }
        }
        System.out.println("check that have you traversed common node or not "+ currentNode.getUniqueId() + "value: "+Isthere.get(currentNode));
        if(Isthere.get(currentNode)!=null && Isthere.get(currentNode)>=3){
          currentIndex++;
          if(currentIndex <= members.size()-1){
            System.out.println("currentNode will be "+ currentNode.getUniqueId());
            assignRolesafterloop(currentNode);
            flag=1;
          }
          else{
            deleteExtraWays();
          }
        }
        savestateoneditor();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Logging.error(e);
        }
    } else {
        currentNode = null;
    }
    previousWay = currentWay;
    if(flag==1){
      fixgapAfterlooping(brokenidx);
      flag=0;
    }
    else if (currentIndex < members.size() - 1) {
        callNextWay(++currentIndex);
    } else{
      deleteExtraWays();
      makeNodesZeros();
    }
}

void addNewWays(List<Way> ways, int i) {
    try {
        List<RelationMember> c = new ArrayList<>();
        String s="";
        if(prelink.isOnewayLoopBackwardPart){
          s="forward";
        }
        System.out.println("way size " + ways.size());
        int[] idx = new int[1];
        idx[0]=i+1;
        Way w = ways.get(0);
        s=assignRoles(w);
        for (int k = 0; k < ways.size(); k++) {
            c.add(new RelationMember(s, ways.get(k)));
            // check if the way that is getting added is already present or not
            if (!waysAlreadyPresent.containsKey(ways.get(k)) && IsWaythere.get(ways.get(k))==null) {
                waysAlreadyPresent.put(ways.get(k), 1);
                // System.out.println("I am adding " + ways.get(k).getUniqueId());
               for(Node node:ways.get(k).getNodes()){
                  if(Isthere.get(node)==null || Isthere.get(node)==0){
                    Isthere.put(node,1);
                  }
                  else{
                    Isthere.put(node,Isthere.get(node)+1);
                  }
                }
                IsWaythere.put(ways.get(k),1);
            }
            else {
                deleteWayAfterIndex(ways.get(k), i);
            }
        }
        memberTableModel.addMembersAfterIdx(ways, i);
        memberTableModel.updateRole(idx,s);
        members.addAll(i + 1, c);

        currentNode = getOtherNode(w,currentNode);
         System.out.println("currentIndex is "+ currentIndex +" Way id is "+ members.get(currentIndex).getWay().getUniqueId());
        links = connectionTypeCalculator.updateLinks(members);
    } catch (Exception e) {
        Logging.error(e);
    }
}
String assignRoles(Way w){
  int flag =0,flag2=0;
  String s ="";
  if(lastForWay != null && lastBackWay !=null) {
  // System.out.println("last traversed backward edge: "+lastBackWay.getUniqueId());
  // System.out.println("last traversed forward edge: "+lastForWay.getUniqueId());
  // System.out.println("Current Node: "+currentNode.getUniqueId());
  System.out.println("assigning roles uselessly");
  NodePair pair = WayUtils.findCommonFirstLastNodes(lastForWay,lastBackWay);
  int num = WayUtils.findNumberOfCommonFirstLastNodes(lastBackWay, lastForWay);
  Node nod;
  if(pair.getA()==null && pair.getB()!=null){
    if(pair.getB().equals(currentNode)){
      flag=1;
    }
  }
  else if(pair.getB()==null && pair.getA()!=null){
    if(pair.getA().equals(currentNode)){
      flag=1;
    }
  }
  else if(pair.getB()!=null && pair.getA()!=null){
    if(pair.getA().equals(currentNode) || pair.getB().equals(currentNode)){
      flag =1;
    }
  }
}
  if (flag==1){
    s="";
  }
  else if(prelink.isOnewayLoopBackwardPart && currentNode.equals(w.firstNode())){
   s="backward";
 }
 else if(prelink.isOnewayLoopBackwardPart && currentNode.equals(w.lastNode())){
   s="forward";
 }
 else if(prelink.isOnewayLoopForwardPart && currentNode.equals(w.firstNode())){
   s="forward";
 }
 else if(prelink.isOnewayLoopForwardPart && currentNode.equals(w.lastNode())){
   s="backward";
 }
 else{
   s="";
 }
 return s;
}
void assignRolesafterloop(Node jointNode){
  System.out.println("assigning roles in loop");
    int idx = currentIndex;
    int[] idxlst = new int[1];
    String[] roles = new String[20];
    Node node1 ;
    Node node2 ;
    Way w = members.get(idx).getWay();
    Node node = null;
    String s ="";
    if(w.firstNode().equals(jointNode)){
      node =w.lastNode();
      s="backward";
    }
    else{
      s="forward";
      node =w.firstNode();
    }
    idxlst[0]=idx;
    idx--;
//    System.out.println("node id: "+ node.getUniqueId()+"distance between them " +dist.compare(node,node1));
    Way minWay = w;
    double minLength = findDistanceBetweenWays(w, nextWay, jointNode);
    memberTableModel.updateRole(idxlst,s);
    while(true){
      w = members.get(idx).getWay();
      if(w.firstNode().equals(node)){
        node =w.lastNode();
        node1 = w.firstNode();
        s="backward";
        // roles[idx]=s;
      }
      else{
        node =w.firstNode();
        node1 = w.lastNode();
        s="forward";
        // roles[idx]=s;
      }
      idxlst[0]=idx;
      double length = findDistanceBetweenWays(w, nextWay, node1);
      if (minLength > length) {
        minLength = length;
        minWay = w;
      }
      memberTableModel.updateRole(idxlst,s);
      // System.out.println("node id: "+ node.getUniqueId()+"distance between them " +dist.compare(node,node1));
      if(w.firstNode().equals(jointNode) || w.lastNode().equals(jointNode)){
        break;
      }
      idx--;
    }
    currentNode = jointNode;
    brokenidx = idx;
    sortBelowidx(relation.getMembers(),0);
}

void fixgapAfterlooping(int idx){
  Way w = members.get(idx).getWay();
  currentWay =w;
  int flag = 0;
  Way minWay = w;
  double minLength = findDistanceBetweenWays(w, nextWay, currentNode);
  while(idx<=currentIndex){
    w = members.get(idx).getWay();
    List<Way> parentWays =findNextWay(w,w.lastNode());
  	if(w.firstNode()!=null) {
  		parentWays.addAll(findNextWay(w,w.firstNode()));
  	}
    for(int i=0;i<parentWays.size();i++){
        if(IsWaythere.get(parentWays.get(i))==null){
            Node node =getOtherNode(parentWays.get(i),null);
            double dist = findDistanceBetweenWays(parentWays.get(i),nextWay,node);
            if(dist<minLength){
              minLength = dist;
              minWay = w;
            }
        }
    }
    idx++;
  }
  w=minWay;
  System.out.println("my nearest way is "+w.getUniqueId());
  downloadAreaAroundWay(w,w.lastNode(),w.firstNode());
}

void deleteWayAfterIndex(Way way, int index) {
    for (int i = index + 1; i < members.size(); i++) {
        if (members.get(i).isWay() && members.get(i).getWay().equals(way)) {
            Way prev = null;
            Way next = null;
            boolean del = true;
            if (i > 0 && members.get(i - 1).isWay())
                prev = members.get(i - 1).getWay();
            if (i < members.size() - 1 && members.get(i + 1).isWay())
                next = members.get(i + 1).getWay();
            // if the next index where the same way comes is well connected with its prev
            // and next way then don't delete it in that index
            if (prev != null && next != null) {
                if (WayUtils.findNumberOfCommonFirstLastNodes(prev, way) != 0
                        && WayUtils.findNumberOfCommonFirstLastNodes(way, nextWay) != 0) {
                    del = false;
                }
            }
            if (del) {
                int[] x = {i };
                Way w= members.get(i).getWay();
                // for
                memberTableModel.remove(x);
                members.remove(i);

                break;
            }
        }
    }
    links = connectionTypeCalculator.updateLinks(members);
}

List<Way> findCurrentEdge() {
    List<Way> lst = new ArrayList<>();
    lst.add(currentWay);
    int j = currentIndex;
    Way curr = currentWay;
    while (true) {
        int i = getPreviousWayIndex(j);
        if (i == -1)
            break;

        Way prevWay = members.get(i).getWay();

        if (prevWay == null)
            break;

        if (
            !WayUtils.findCommonFirstLastNode(curr, prevWay)
                .filter(node -> node.getParentWays().size() <= 2)
                .isPresent()
        ) {
            break;
        }

        lst.add(prevWay);
        curr = prevWay;
        j = i;
    }
    return lst;
}

void removeCurrentEdge() {
    List<Integer> lst = new ArrayList<>();
    lst.add(currentIndex);
    int j = currentIndex;
    Way curr = currentWay;
    Node n = null;
    if(IsWaythere.get(curr)!=null){
      IsWaythere.put(curr,null);
    }
    for(Node node:curr.getNodes()){
      Isthere.put(node,Isthere.get(node)-1);
    }
    // while (true) {
    //     int i = getPreviousWayIndex(j);
    //     if (i == -1)
    //         break;
    //
    //     Way prevWay = members.get(i).getWay();
    //
    //     if (prevWay == null)
    //         break;
    //     if (
    //         !WayUtils.findCommonFirstLastNode(curr, prevWay)
    //             .filter(node -> node.getParentWays().size() <= 2)
    //             .isPresent()
    //     ) {
    //         break;
    //     }
    //     if(IsWaythere.get(prevWay)!=null){
    //       IsWaythere.put(prevWay,null);
    //     }
    //     for(Node node:prevWay.getNodes()){
    //       Isthere.put(node,Isthere.get(node)-1);
    //     }
    //     System.out.println("edge going to be remove is "+ prevWay.getUniqueId());
    //     lst.add(i);
    //     curr = prevWay;
    //     j = i;
    // }

    int prevInd = getPreviousWayIndex(j);

    Collections.reverse(lst);
    int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
    memberTableModel.remove(ind);
    for (int i = 0; i < ind.length; i++) {
        members.remove(ind[i] - i);
    }

    savestateoneditor();

    if (prevInd >= 0) {
      currentIndex = prevInd;
      Way w = members.get(currentIndex).getWay();
      if(currentIndex-1>=0){
        Way prevw = members.get(currentIndex-1).getWay();
          if(prevw.firstNode().equals(w.lastNode()) || prevw.lastNode().equals(w.lastNode())){
            n = w.lastNode();
          }
          else{
            n = w.firstNode();
          }
        }
        currentNode = n;

        IsWaythere.put(w,null);
        for(Node node:w.getNodes()){
          Isthere.put(node,Isthere.get(node)-1);
        }
        System.out.println("currentNode yaar: " + currentNode);
        callNextWay(currentIndex);
    } else {
        notice = null;
        deleteExtraWays();
    }
}

private void removeKeyListenerAndTemporaryLayer(KeyListener keyListener) {
    MainApplication.getMap().mapView.removeKeyListener(keyListener);
    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
}

void RemoveWayAfterSelection(List<Integer> wayIndices, Character chr) {
    if (chr == 'A' || chr == '1') {
        // remove all the ways
        int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
        for (int i = 0; i < lst.length; i++) {
            Way way = members.get(i).getWay();
            if(IsWaythere.get(way)!=null){
              IsWaythere.put(way,null);
            }
            for(Node node: way.getNodes()){
              Isthere.put(node,Isthere.get(node)-1);
            }
        }
        // System.out.println("hiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
        memberTableModel.remove(lst);
        for (int i = 0; i < lst.length; i++) {
            members.remove(lst[i] - i);
        }
        // OK.actionPerformed(null);
        savestateoneditor();
        if (currentIndex < members.size() - 1) {
            notice = null;
            callNextWay(currentIndex);
        } else {
            notice = null;
            deleteExtraWays();
        }
    } else if (chr == 'B' || chr == '2') {
        if (currentIndex < members.size() - 1) {
            notice = null;
            currentIndex = wayIndices.get(wayIndices.size() - 1);
            callNextWay(currentIndex);
        } else {
            notice = null;
            deleteExtraWays();
        }
    } else if (chr == 'C' || chr == '4') {
        List<Command> cmdlst = new ArrayList<>();
        int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
        for (int i = 0; i < lst.length; i++) {
            Way w = members.get(lst[i]).getWay();
            TagMap newKeys = w.getKeys();
            newKeys.put("oneway", "bicycle=no");
            cmdlst.add(new ChangePropertyCommand(Collections.singleton(w), newKeys));
        }
        UndoRedoHandler.getInstance().add(new SequenceCommand("Add tags", cmdlst));
        // OK.actionPerformed(null);
        savestateoneditor();
        if (currentIndex < members.size() - 1) {
            notice = null;
            callNextWay(currentIndex);
        } else {
            notice = null;
            deleteExtraWays();
        }
    }
    if (chr == 'R' || chr == '3') {
        // calculate the previous index
        int prevIndex = -1;
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (members.get(i).isWay()) {
                prevIndex = i;
                break;
            }
        }
        // remove all the ways
        int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(lst);
        for (int i = 0; i < lst.length; i++) {
            members.remove(lst[i] - i);
        }
        // OK.actionPerformed(null);
        savestateoneditor();
        if (prevIndex != -1) {
            notice = null;
            callNextWay(prevIndex);
        } else {
            notice = null;
            deleteExtraWays();
        }
    }
}



private boolean isNonSplitRoundAbout(final Way way) {
    return way.hasTag("junction", "roundabout") && way.firstNode().equals(way.lastNode());
}

private boolean isSplitRoundAbout(final Way way) {
    return way.hasTag("junction", "roundabout") && !way.firstNode().equals(way.lastNode());
}

boolean isRestricted(Way currentWay, Way previousWay, Node commonNode) {
    Set<Relation> parentSet = OsmPrimitive.getParentRelations(previousWay.getNodes());
    if (parentSet == null || parentSet.isEmpty())
        return false;
    List<Relation> parentRelation = new ArrayList<>(parentSet);

    String[] restrictions = new String[] {"restriction", "restriction:bicycle"};

    parentRelation.removeIf(rel -> {
        if (rel.hasKey("except")) {
            String[] val = rel.get("except").split(";");
            for (String s : val) {
                if (relation.hasTag("route", s))
                    return true;
            }
        }

        if (!rel.hasTag("type", restrictions))
            return true;
        else if (rel.hasTag("type", "restriction") && rel.hasKey("restriction"))
            return false;
        else {
            boolean remove = true;
            String routeValue = relation.get("route");
            for (String s : restrictions) {
                String sub = s.substring(12);
                if (routeValue.equals(sub) && rel.hasTag("type", s))
                    remove = false;
                else if (routeValue.equals(sub) && rel.hasKey("restriction:" + sub))
                    remove = false;
            }
            return remove;
        }
    });

    // check for "only" kind of restrictions
    for (Relation r : parentRelation) {
        Collection<RelationMember> prevMemberList = r.getMembersFor(Collections.singletonList(previousWay));
        Collection<RelationMember> commonNodeList = r.getMembersFor(Collections.singletonList(commonNode));
        // commonNode is not the node involved in the restriction relation then just continue
        if (prevMemberList.isEmpty() || commonNodeList.isEmpty())
            continue;

        final String prevRole = prevMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);

        if (prevRole.equals("from")) {
            String[] acceptedTags = {
                "only_right_turn", "only_left_turn", "only_u_turn", "only_straight_on", "only_entry", "only_exit"
            };
            for (String s : restrictions) {
                // if we have any "only" type restrictions then the current way should be in the
                // relation else it is restricted
                if (r.hasTag(s, acceptedTags)) {
                    if (r.getMembersFor(Collections.singletonList(currentWay)).isEmpty()) {
                        for (String str : acceptedTags) {
                            if (r.hasTag(s, str))
                                notice = str + " restriction violated";
                        }
                        return true;
                    }
                }
            }
        }
    }

    for (Relation r : parentRelation) {
        Collection<RelationMember> curMemberList = r.getMembersFor(Collections.singletonList(currentWay));
        Collection<RelationMember> prevMemberList = r.getMembersFor(Collections.singletonList(previousWay));

        if (curMemberList.isEmpty() || prevMemberList.isEmpty())
            continue;

        final String curRole = curMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);
        final String prevRole = prevMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);

        if ("to".equals(curRole) && "from".equals(prevRole)) {
            final String[] acceptedTags = {
                "no_right_turn", "no_left_turn", "no_u_turn", "no_straight_on", "no_entry", "no_exit"
            };
            for (String s : restrictions) {
                if (r.hasTag(s, acceptedTags)) {
                    for (String str : acceptedTags) {
                        if (r.hasTag(s, str))
                            notice = str + " restriction violated";
                    }
                    return true;
                }
            }
        }
    }

    return false;
}

private Node getOtherNode(Way way, Node node) {
	// TODO Auto-generated method stub
	if(way.firstNode().equals(node)) {
		return way.lastNode();
	}else {
		return way.firstNode();
	}
}

////////update states//////////

private void deleteExtraWays() {
	// TODO Auto-generated method stub
	int [] ints = extraWaysToBeDeleted.stream().mapToInt(Integer::intValue).toArray();
	memberTableModel.remove(ints);
	setEnable = true;
	setEnabled(setEnable);
	halt = false;
}

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
    // links = connectionTypeCalculator.updateLinks(members);
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

  void makeNodesZeros(){
    for(int i=0;i<members.size();i++){
      Way n = members.get(i).getWay();
      Node f = n.firstNode();
      Node l = n.lastNode();
      Isthere.put(f,0);
      Isthere.put(l,0);
      IsWaythere.put(n,null);
    }
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

  private class MendRelationAddMultipleLayer extends AbstractMapViewPaintable {

      @Override
      public void paint(Graphics2D g, MapView mv, Bounds bbox) {
          MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
          paintVisitor.drawMultipleVariants(wayListColoring);
      }
  }

  class MendRelationPaintVisitor extends PaintVisitor {
      /** The graphics */
      private final Graphics g;
      /** The MapView */
      private final MapView mv;
      private HashMap<Way, List<Character>> waysColoring;

      MendRelationPaintVisitor(Graphics2D g, MapView mv) {
          super(g, mv);
          this.g = g;
          this.mv = mv;
      }

      /*
       * Functions in this class are directly taken from PTAssistantPaintVisitor with
       * some slight modification
       */

      void drawVariants() {
          drawFixVariantsWithParallelLines(true);

          Color[] colors = {new Color(0, 255, 150), new Color(255, 0, 0, 150), new Color(0, 0, 255, 150),
                  new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

          double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
          double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

          boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
          Character chr = 'A';
          if (numeric)
              chr = '1';

          if (showOption0 && numeric) {
              if (!shorterRoutes)
                  drawFixVariantLetter("0 : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX, letterY, 25);
              else
                  drawFixVariantLetter("0 : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX, letterY,
                          25);
              letterY = letterY + 60;
          } else if (showOption0) {
              if (!shorterRoutes)
                  drawFixVariantLetter("W : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX, letterY, 25);
              else
                  drawFixVariantLetter("W : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX, letterY,
                          25);
              letterY = letterY + 60;
          }

          for (int i = 0; i < 5; i++) {
              if (wayColoring.containsValue(chr)) {
                  drawFixVariantLetter(chr.toString(), colors[i], letterX, letterY, 35);
                  letterY = letterY + 60;
              }
              chr++;
          }

          // display the "Esc", "Skip" label:
          drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 25);
          letterY = letterY + 60;
          if (numeric) {
              drawFixVariantLetter("7 : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("9 : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
          } else {
              drawFixVariantLetter("S : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("Q : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
          }
      }

      void drawOptionsToRemoveWays() {
          drawFixVariantsWithParallelLines(false);
          boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

          double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
          double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

          if (notice != null) {
              drawFixVariantLetter("Error:  " + notice, Color.WHITE, letterX, letterY, 25);
              letterY = letterY + 60;
          }
          if (numeric) {
              drawFixVariantLetter("1 : " + tr(I18N_REMOVE_WAYS), RAINBOW_COLOR_PALETTE[0], letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("2 : " + tr(I18N_NOT_REMOVE_WAYS), RAINBOW_COLOR_PALETTE[1], letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("3 : " + tr(I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY), RAINBOW_COLOR_PALETTE[4], letterX, letterY, 25);
              letterY = letterY + 60;
              if (notice.equals("vehicle travels against oneway restriction")) {
                  drawFixVariantLetter("4 : " + tr(I18N_ADD_ONEWAY_BICYCLE_NO_TO_WAY), RAINBOW_COLOR_PALETTE[3], letterX, letterY, 25);
              }
          } else {
              drawFixVariantLetter("A : " + tr(I18N_REMOVE_WAYS), RAINBOW_COLOR_PALETTE[0], letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("B : " + tr(I18N_NOT_REMOVE_WAYS), RAINBOW_COLOR_PALETTE[1], letterX, letterY, 25);
              letterY = letterY + 60;
              if (notice.equals("vehicle travels against oneway restriction")) {
                  drawFixVariantLetter("C : " + tr(I18N_ADD_ONEWAY_BICYCLE_NO_TO_WAY), RAINBOW_COLOR_PALETTE[3], letterX, letterY, 25);
                  letterY = letterY + 60;
              }
              drawFixVariantLetter("R : " + tr(I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY), RAINBOW_COLOR_PALETTE[4], letterX, letterY, 25);
          }

          letterY = letterY + 60;
          drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 30);
      }

      void drawMultipleVariants(HashMap<Character, List<Way>> fixVariants) {
          waysColoring = new HashMap<>();
          addFixVariants(fixVariants);
          drawFixVariantsWithParallelLines(waysColoring);

          int colorIndex = 0;

          double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
          double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

          boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

          if (showOption0 && numeric) {
              if (!shorterRoutes)
                  drawFixVariantLetter("0 : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX, letterY, 25);
              else
                  drawFixVariantLetter("0 : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX, letterY,
                          25);
              letterY = letterY + 60;
          } else if (showOption0) {
              if (!shorterRoutes)
                  drawFixVariantLetter("W : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX, letterY, 25);
              else
                  drawFixVariantLetter("W : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX, letterY,
                          25);
              letterY = letterY + 60;
          }

          for (Entry<Character, List<Way>> entry : fixVariants.entrySet()) {
              Character c = entry.getKey();
              if (fixVariants.get(c) != null) {
                  drawFixVariantLetter(c.toString(), RAINBOW_COLOR_PALETTE[colorIndex % 5], letterX, letterY, 35);
                  colorIndex++;
                  letterY = letterY + 60;
              }
          }

          // display the "Esc", "Skip" label:
          drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 25);
          letterY = letterY + 60;
          if (numeric) {
              drawFixVariantLetter("7 : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("9 : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
          } else {
              drawFixVariantLetter("S : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
              letterY = letterY + 60;
              drawFixVariantLetter("Q : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
          }

      }

      protected void drawFixVariantsWithParallelLines(final boolean drawNextWay) {
          wayColoring.entrySet().stream()
              // Create pairs of a color and an associated pair of nodes
              .flatMap( entry ->
                  entry.getKey().getNodePairs(false).stream()
                      .map(it -> Pair.create(CHARACTER_COLOR_MAP.get(entry.getValue()), it))
              )
              // Grouping by color: groups stream into a map, each map entry has a color and all associated pairs of nodes
              .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b, Collectors.toList())))
              .forEach((color, nodePairs) -> drawSegmentsWithParallelLines(nodePairs, color));
          drawSegmentsWithParallelLines(currentWay.getNodePairs(false), CURRENT_WAY_COLOR);
          if (drawNextWay) {
              drawSegmentsWithParallelLines(nextWay.getNodePairs(false), NEXT_WAY_COLOR);
          }
      }

      protected void drawFixVariantsWithParallelLines(Map<Way, List<Character>> waysColoring) {
          for (final Entry<Way, List<Character>> entry : waysColoring.entrySet()) {
              final List<Color> wayColors = entry.getValue().stream().map(CHARACTER_COLOR_MAP::get).collect(Collectors.toList());
              for (final Pair<Node, Node> nodePair : entry.getKey().getNodePairs(false)) {
                  drawSegmentWithParallelLines(nodePair.a, nodePair.b, wayColors);
              }
          }

          drawSegmentsWithParallelLines(
              findCurrentEdge().stream().flatMap(it -> it.getNodePairs(false).stream()).collect(Collectors.toList()),
              CURRENT_WAY_COLOR
          );

          drawSegmentsWithParallelLines(nextWay.getNodePairs(false), NEXT_WAY_COLOR);

      }

      /**
       * Convenience method for {@link #drawSegmentWithParallelLines(Node, Node, List)}.
       */
      private void drawSegmentsWithParallelLines(List<Pair<Node, Node>> nodePairs, final Color color) {
          final List<Color> colorList = Collections.singletonList(color);
          nodePairs.forEach(it -> drawSegmentWithParallelLines(it.a, it.b, colorList));
      }

      void drawSegmentWithParallelLines(Node n1, Node n2, List<Color> colors) {
          if (!n1.isDrawable() || !n2.isDrawable() || !isSegmentVisible(n1, n2)) {
              return;
          }

          Point p1 = mv.getPoint(n1);
          Point p2 = mv.getPoint(n2);
          double t = Math.atan2((double) p2.x - p1.x, (double) p2.y - p1.y);
          double cosT = 9 * Math.cos(t);
          double sinT = 9 * Math.sin(t);
          double heightCosT = 9 * Math.cos(t);
          double heightSinT = 9 * Math.sin(t);

          double prevPointX = p1.x;
          double prevPointY = p1.y;
          double nextPointX = p1.x + heightSinT;
          double nextPointY = p1.y + heightCosT;

          Color currentColor = colors.get(0);
          int i = 0;
          g.setColor(currentColor);
          g.fillOval(p1.x - 9, p1.y - 9, 18, 18);

          if (colors.size() == 1) {
              int[] xPoints = {(int) (p1.x + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT), (int) (p1.x - cosT) };
              int[] yPoints = {(int) (p1.y - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT), (int) (p1.y + sinT) };
              g.setColor(currentColor);
              g.fillPolygon(xPoints, yPoints, 4);
          } else if (colors.size() > 1) {
              boolean iterate = true;
              while (iterate) {
                  currentColor = colors.get(i % colors.size());

                  int[] xPoints = {(int) (prevPointX + cosT), (int) (nextPointX + cosT), (int) (nextPointX - cosT),
                          (int) (prevPointX - cosT) };
                  int[] yPoints = {(int) (prevPointY - sinT), (int) (nextPointY - sinT), (int) (nextPointY + sinT),
                          (int) (prevPointY + sinT) };
                  g.setColor(currentColor);
                  g.fillPolygon(xPoints, yPoints, 4);

                  prevPointX = prevPointX + heightSinT;
                  prevPointY = prevPointY + heightCosT;
                  nextPointX = nextPointX + heightSinT;
                  nextPointY = nextPointY + heightCosT;
                  i++;
                  if ((p1.x < p2.x && nextPointX >= p2.x) || (p1.x >= p2.x && nextPointX <= p2.x)) {
                      iterate = false;
                  }
              }

              int[] lastXPoints = {(int) (prevPointX + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT),
                      (int) (prevPointX - cosT) };
              int[] lastYPoints = {(int) (prevPointY - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT),
                      (int) (prevPointY + sinT) };
              g.setColor(currentColor);
              g.fillPolygon(lastXPoints, lastYPoints, 4);
          }

          g.setColor(currentColor);
          g.fillOval(p2.x - 9, p2.y - 9, 18, 18);
      }

      void addFixVariants(HashMap<Character, List<Way>> fixVariants) {
          for (Entry<Character, List<Way>> entry : fixVariants.entrySet()) {
              Character currentFixVariantLetter = entry.getKey();
              List<Way> fixVariant = entry.getValue();
              for (Way way : fixVariant) {
                  if (waysColoring.containsKey(way)) {
                      if (!waysColoring.get(way).contains(currentFixVariantLetter)) {
                          waysColoring.get(way).add(currentFixVariantLetter);
                      }
                  } else {
                      List<Character> letterList = new ArrayList<>();
                      letterList.add(currentFixVariantLetter);
                      waysColoring.put(way, letterList);
                  }
              }
          }
      }

      /**
       * Visualizes the letters for each fix variant
       * @param letter letter to draw
       * @param color text color
       * @param letterX X coordinate
       * @param letterY Y coordinate
       * @param size font size
       */
      private void drawFixVariantLetter(String letter, Color color, double letterX, double letterY, int size) {
          g.setColor(color);
          Font stringFont = new Font("SansSerif", Font.PLAIN, size);
          g.setFont(stringFont);
          try {
              g.drawString(letter, (int) letterX, (int) letterY);
              g.drawString(letter, (int) letterX, (int) letterY);
          } catch (NullPointerException ex) {
              // do nothing
              Logging.trace(ex);
          }

      }
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
