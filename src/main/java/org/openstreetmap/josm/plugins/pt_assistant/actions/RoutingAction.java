// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

public class RoutingAction extends AbstractRelationEditorAction{
	static Relation relation = null;
	MemberTableModel memberTableModel = null;
	GenericRelationEditor editor = null;
	List<RelationMember> members = null;
	boolean setEnable = true;
	boolean firstCall = true;
	boolean halt = false;
	static boolean abort = false;

  public RoutingAction(IRelationEditorActionAccess editorAccess){
    super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
    putValue(SHORT_DESCRIPTION, tr("Routing Helper"));
    new ImageProvider("dialogs/relation", "Bicycle.svg").getResource().attachImageIcon(this, true);
    updateEnabledState();
    editor = (GenericRelationEditor) editorAccess.getEditor();
    memberTableModel = editorAccess.getMemberTableModel();
    this.relation = editor.getRelation();
    System.out.println("yoooooooooo");
    editor.addWindowListener(new WindowEventHandler());
  }
  @Override
  protected void updateEnabledState() {
      final Relation curRel = relation;
      setEnabled(
          curRel != null && setEnable &&
          (
              (curRel.hasTag("route", "bus") && curRel.hasTag("public_transport:version", "2")) ||
              (RouteUtils.isPTRoute(curRel) && !curRel.hasTag("route", "bus")) || (curRel.hasTag("route", "bicycle"))
          )
      );
  }
  @Override
  public void actionPerformed(ActionEvent e) {
       if (relation.hasIncompleteMembers()) {
           downloadIncompleteRelations();
           new Notification(tr("Downloading incomplete relation members. Kindly wait till download gets over."))
                   .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(3600).show();
       } else {
    if(relation.hasTag("route","bicycle")) {
    	System.out.println("I got bicycle access");
        Bicycle bike = new Bicycle(editorAccess);
        bike.init();
       }
    else {
    	System.out.println("I got ptassistant access");
        MendRelationAction pt_transport = new MendRelationAction(editorAccess);
        pt_transport.initialise();
    }
  }
  }
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
              if(relation.hasTag("route","bicycle")) {
              	System.out.println("I got bicycle access");
                  Bicycle bike = new Bicycle(editorAccess);
                  bike.init();
                 }
              else {
              	System.out.println("I got ptassistant access");
                  MendRelationAction pt_transport = new MendRelationAction(editorAccess);
                  pt_transport.initialise();
              }
          } catch (InterruptedException | ExecutionException e1) {
              Logging.error(e1);
          }
      });
  }
  class WindowEventHandler extends WindowAdapter {
  }
}

