// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.customizepublictransportstop;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * OSM object tags
 *
 * @author Rodion Scherbakov
 */
public final class OSMTags {
    public static final String TERTIARY_LINK_TAG_VALUE = "tertiary_link";
    public static final String SECONDARY_LINK_TAG_VALUE = "secondary_link";
    public static final String PRIMARY_LINK_TAG_VALUE = "primary_link";
    public static final String TRUNK_LINK_TAG_VALUE = "trunk_link";
    public static final String ROAD_TAG_VALUE = "road";
    public static final String BUS_GUIDEWAY_TAG_VALUE = "bus_guideway";
    public static final String SERVICE_TAG_VALUE = "service";
    public static final String RESIDENTIAL_TAG_VALUE = "residential";
    public static final String UNCLASSIFIED_TAG_VALUE = "unclassified";
    public static final String TERTIARY_TAG_VALUE = "tertiary";
    public static final String SECONDARY_TAG_VALUE = "secondary";
    public static final String PRIMARY_TAG_VALUE = "primary";
    public static final String TRUNK_TAG_VALUE = "trunk";

    public static final Collection<String> TAG_VALUES_HIGHWAY = Collections.unmodifiableCollection(Arrays.asList(
        TRUNK_TAG_VALUE,
        PRIMARY_TAG_VALUE,
        SECONDARY_TAG_VALUE,
        TERTIARY_TAG_VALUE,
        UNCLASSIFIED_TAG_VALUE,
        RESIDENTIAL_TAG_VALUE,
        SERVICE_TAG_VALUE,
        BUS_GUIDEWAY_TAG_VALUE,
        ROAD_TAG_VALUE,
        TRUNK_LINK_TAG_VALUE,
        PRIMARY_LINK_TAG_VALUE,
        SECONDARY_LINK_TAG_VALUE,
        TERTIARY_LINK_TAG_VALUE
    ));

    public static final String TRAM_TAG_VALUE = "tram";
    public static final String USAGE_TAG = "usage";
    public static final String MAIN_TAG_VALUE = "main";
    public static final String RAIL_TAG_VALUE = "rail";

    public static final String AREA_TAG = "area";
    public static final String COVERED_TAG = "covered";
    public static final String SHELTER_TAG = "shelter";
    public static final String BENCH_TAG = "bench";
    public static final String TRAIN_TAG = "train";
    public static final String STOP_POSITION_TAG_VALUE = "stop_position";
    public static final String STATION_TAG_VALUE = "station";
    public static final String HALT_TAG_VALUE = "halt";
    public static final String YES_TAG_VALUE = "yes";
    public static final String RAILWAY_TAG = "railway";
    public static final String TRAM_STOP_TAG_VALUE = "tram_stop";
    public static final String TRAM_TAG = "tram";
    public static final String SHARE_TAXI_TAG = "share_taxi";
    public static final String TROLLEYBUS_TAG = "trolleybus";
    public static final String BUS_TAG = "bus";
    public static final String NETWORK_TAG = "network";
    public static final String OPERATOR_TAG = "operator";
    public static final String NAME_EN_TAG = "name:en";
    public static final String NAME_TAG = "name";
    public static final String HIGHWAY_TAG = "highway";
    public static final String AMENITY_TAG = "amenity";
    public static final String BUS_STATION_TAG_VALUE = "bus_station";

    public static final String BUS_STOP_TAG_VALUE = "bus_stop";
    public static final String PUBLIC_TRANSPORT_TAG = "public_transport";
    public static final String STOP_AREA_TAG_VALUE = "stop_area";
    public static final String STOP_ROLE = "stop";
    public static final String PLATFORM_ROLE = "platform";
    public static final String PLATFORM_TAG_VALUE = "platform";
    public static final String SERVICE_TAG = "service";

    public static final String CITY_NETWORK_TAG_VALUE = "city";
    public static final String COMMUTER_NETWORK_TAG_VALUE = "commuter";
    public static final String LOCAL_NETWORK_TAG_VALUE = "local";
    public static final String REGIONAL_NETWORK_TAG_VALUE = "regional";
    public static final String LONG_DISTANCE_NETWORK_TAG_VALUE = "long_distance";
    public static final String HIGH_SPEED_NETWORK_TAG_VALUE = "high_speed";

    /** See <a href="https://wiki.openstreetmap.org/wiki/Key:type">Key:type</a>. */
    public static final String KEY_RELATION_TYPE = "type";
    /** See <a href="https://wiki.openstreetmap.org/wiki/Key:route">Key:route</a>. */
    public static final String KEY_ROUTE = "route";
    /** See <a href="https://wiki.openstreetmap.org/wiki/Key:route_master">Key:route_master</a>. */
    public static final String KEY_ROUTE_MASTER = "route_master";
    /** See <a href="https://wiki.openstreetmap.org/wiki/Tag:type=route_master">Tag:type=route_master</a>. */
    public static final String VALUE_TYPE_ROUTE_MASTER = KEY_ROUTE_MASTER;

    private OSMTags() {
        // Hide default constructor
    }
}
