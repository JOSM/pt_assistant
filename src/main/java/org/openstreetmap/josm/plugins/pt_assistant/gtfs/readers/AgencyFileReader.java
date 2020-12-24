package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Agency;

public class AgencyFileReader extends AbstractGtfsFileReader {

    @Override
    protected String getFileName() {
        return "agency.txt";
    }

    public Map<String, Agency> getAll() {
        FieldAccessor agency = getDataLinesCount() == 1
                ? getRequiredAccessor("agency_id")
                : getOptionalAccessor("agency_id", "");
        FieldAccessor agencyName = getRequiredAccessor("agency_name");
        FieldAccessor agencyUrl = getRequiredAccessor("agency_url");
        return
            streamLinesAs(lineAccess -> new Agency(
                lineAccess.get(agency),
                lineAccess.get(agencyName),
                lineAccess.get(agencyUrl)
            )).collect(Collectors.toMap(Agency::getId, Function.identity()));
    }

    public static Map<String, Agency> read(FileSystem zip) throws IOException {
        AgencyFileReader agencies = new AgencyFileReader();
        agencies.readFrom(zip);
        return agencies.getAll();
    }
}
