package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Agency;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.GtfsRouteType;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Route;

public class RouteFileReader extends AbstractGtfsFileReader {
    @Override
    protected String getFileName() {
        return "routes.txt";
    }

    public Map<String, Route> getRoutes(Map<String, Agency> agencies) {
        FieldAccessor routeId = getRequiredAccessor("route_id");
        FieldAccessor agencyId = getOptionalAccessor("agency_id", "");
        FieldAccessor shortName = getRequiredAccessor("route_short_name");
        FieldAccessor longName = getRequiredAccessor("route_long_name");
        FieldAccessor desc = getOptionalAccessor("route_desc", "");
        FieldAccessor type = getRequiredAccessor("route_type");
        FieldAccessor color = getOptionalAccessor("route_color", "");
        FieldAccessor textColor = getOptionalAccessor("route_text_color", "");
        FieldAccessor sortOrder = getOptionalAccessor("route_sort_order", "0");
        return streamLinesAs(lineAccess -> {
            String myAgencyId = lineAccess.get(agencyId);
            Agency agency = agencies.get(myAgencyId);
            if (agency == null) {
                throw new IllegalStateException("Expected to find agency " + myAgencyId + " but only got " + agencies.keySet());
            }
            return new Route(lineAccess.get(routeId),
                agency,
                lineAccess.get(shortName),
                lineAccess.get(longName),
                lineAccess.get(desc),
                GtfsRouteType.getById(lineAccess.get(type)),
                parseColor(lineAccess.get(color)),
                parseColor(lineAccess.get(textColor)),
                Integer.parseInt(lineAccess.get(sortOrder))
            );
        }).collect(Collectors.toMap(Route::getId, Function.identity()));
    }

    public static Map<String, Route> read(FileSystem zip, Map<String, Agency> agencies) throws IOException {
        RouteFileReader routes = new RouteFileReader();
        routes.readFrom(zip);
        return routes.getRoutes(agencies);
    }
}
