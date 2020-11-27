package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.gui.linear.UnBoldLabel.safeHtml;

import java.awt.BorderLayout;
import java.text.MessageFormat;
import java.util.Objects;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.Relation;

public class RouteMasterHeadlinePanel extends JPanel {
    public RouteMasterHeadlinePanel(Relation masterRelation) {
        setLayout(new BorderLayout());
        String color = masterRelation.get("colour");
            if (color == null) {
                color = "#888888";
            }
        String headline = MessageFormat.format("<font bgcolor=\"{0}\">{1}</font> {2}", safeHtml(color),
            safeHtml(masterRelation.get("ref")), safeHtml(masterRelation.get("name")));
        String infos = safeHtml(masterRelation.get("operator")) + " " + safeHtml(masterRelation.get("network"));
        String routeMasterText = MessageFormat.format("<html><h2>{0}</h2><div>{1}</div></html>", headline, infos);
        add(new UnBoldLabel(routeMasterText));
    }
}
