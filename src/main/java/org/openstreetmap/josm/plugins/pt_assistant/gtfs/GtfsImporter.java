package org.openstreetmap.josm.plugins.pt_assistant.gtfs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.gui.GtfsLayer;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.GtfsReadException;

public class GtfsImporter extends FileImporter {
    /**
     * Constructs a new {@code FileImporter}.
     */
    public GtfsImporter() {
        super(new ExtensionFileFilter("zip", "zip", tr("GTFS file (*.zip)")));
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        try {
            FileSystem zip = FileSystems.newFileSystem(file.toPath(), null);
            GtfsFile gtfsFile = new GtfsFile(file.getName(), zip);
            MainApplication.getLayerManager().addLayer(new GtfsLayer(gtfsFile));
        } catch (GtfsReadException e) {
            throw new IllegalDataException(tr("The GTFS file could not be parsed.", e));
        }
    }
}
