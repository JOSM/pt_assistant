package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isVersionTwoPTRoute;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
 * Adds or removes the tab for the linear relation
 */
public class LineRelationTabManager extends AbstractTabManager {
    public LineRelationTabManager(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
    }

    @Override
    protected TabAndDisplay getTabToShow(Relation relation) {
        if (isVersionTwoPTRoute(relation)) {
            Optional<Relation> master = relation.getReferrers().stream().filter(PublicTransportLinePanel::isRouteMaster).map(it -> (Relation) it).findFirst();
            return new AbstractTabManager.TabAndDisplay() {
                @Override
                public boolean shouldDisplay() {
                    return true;
                }

                @Override
                public JPanel getTabContent() {
                    return new PublicTransportLinePanel(new LineRelationsProvider() {
                        @Override
                        public Optional<Relation> getMasterRelation() {
                            return master;
                        }

                        @Override
                        public List<LineRelation> getRelations() {
                            return master.map(m -> getRouteRelations(m)
                                .map(it -> new LineRelation(it,  relation.equals(it)))
                                .collect(Collectors.toList()))
                                .orElseGet(() -> Arrays.asList(new LineRelation(relation, true)));
                        }

                        @Override
                        public Relation getCurrentRelation() {
                            return relation;
                        }

                        @Override
                        public Component createHeadlinePanel() {
                            return master.<Component>map(RouteMasterHeadlinePanel::new)
                                .orElseGet(() -> new JLabel(tr("This route is not in a route master relation.")));
                        }
                    });
                }

                @Override
                public String getTitle() {
                    return tr("Route");
                }
            };

        } else if (PublicTransportLinePanel.isRouteMaster(relation)) {
            return new TabAndDisplay() {
                @Override
                public boolean shouldDisplay() {
                    return true;
                }

                @Override
                public JPanel getTabContent() {
                    return new PublicTransportLinePanel(new LineRelationsProvider() {
                        @Override
                        public Optional<Relation> getMasterRelation() {
                            return Optional.of(relation);
                        }

                        @Override
                        public List<LineRelation> getRelations() {
                            return getRouteRelations(relation)
                                .map(it -> new LineRelation(it, true))
                                .collect(Collectors.toList());
                        }

                        @Override
                        public Relation getCurrentRelation() {
                            return relation;
                        }

                        @Override
                        public Component createHeadlinePanel() {
                            return new RouteMasterHeadlinePanel(relation);
                        }
                    });
                }

                @Override
                public String getTitle() {
                    return tr("Routes");
                }
            };
        } else if (StopUtils.isStopArea(relation)) {
            return new TabAndDisplay() {
                @Override
                public boolean shouldDisplay() {
                    return true;
                }

                @Override
                public JPanel getTabContent() {
                    return new PublicTransportLinePanel(new LineRelationsProvider() {
                        @Override
                        public Optional<Relation> getMasterRelation() {
                            return Optional.empty();
                        }

                        @Override
                        public List<LineRelation> getRelations() {
                            // All the trams / busses stopping on any of our stops.
                            // TODO: Platforms?
                            List<Relation> routeRelations = getRouteRelations(relation.getMembers()
                                .stream()
                                .filter(it -> OSMTags.STOP_ROLE.equals(it.getRole()))
                                .flatMap(it -> it.getMember().getReferrers().stream()))
                                .collect(Collectors.toList());
                            // If we have many routes, show fewer stops.
                            int count = routeRelations.size() > 10 ? 1 : routeRelations.size() > 4 ? 3 : 5;
                            return routeRelations
                                .stream()
                                .map(it -> new LineRelationAroundStop(it, true,
                                    stop -> stop.getReferrers().contains(relation), count))
                                .collect(Collectors.toList());
                        }

                        @Override
                        public Relation getCurrentRelation() {
                            return relation;
                        }

                        @Override
                        public Component createHeadlinePanel() {
                            String title = tr("Routes stopping at {0}", relation.get("name"));
                            return new UnBoldLabel("<html><h2>" + UnBoldLabel.safeHtml(title) + "</h2></html>");
                        }
                    });
                }

                @Override
                public String getTitle() {
                    return tr("Routes");
                }
            };
        } else {
            return new TabAndDisplay() {
                @Override
                public boolean shouldDisplay() {
                    return false;
                }

                @Override
                public JPanel getTabContent() {
                    throw new UnsupportedOperationException("No routes tab");
                }

                @Override
                public String getTitle() {
                    return null;
                }
            };
        }
    }

    private static Stream<Relation> getRouteRelations(Relation master) {
        return getRouteRelations(master.getMembers().stream().map(RelationMember::getMember));
    }

    private static Stream<Relation> getRouteRelations(Stream<OsmPrimitive> primitives) {
        return primitives
            .filter(it -> it.getType() == OsmPrimitiveType.RELATION)
            .map(it -> (Relation) it)
            .filter(RouteUtils::isVersionTwoPTRoute);
    }

}
