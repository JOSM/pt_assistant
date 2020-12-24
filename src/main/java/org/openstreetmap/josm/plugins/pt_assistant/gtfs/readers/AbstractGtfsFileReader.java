package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opencsv.CSVParserBuilder;

/**
 * Reads those special CSV files
 */
public abstract class AbstractGtfsFileReader {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * The field / column names in the file
     */
    private String[] fieldNames = null;

    private final List<String[]> lines = new ArrayList<>();

    protected static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DATE_FORMAT);
    }

    protected static Color parseColor(String colorOrEmpty) {
        return colorOrEmpty.isEmpty()
            ? null
            : new Color(0xff000000 | Integer.parseInt(colorOrEmpty, 16));
    }

    protected abstract String getFileName();

    public void addLine(String[] parsedLine) throws IllegalArgumentException {
        if (fieldNames == null) {
            fieldNames = parsedLine;
        } else {
            if (fieldNames.length != parsedLine.length) {
                throw new GtfsReadException(getFileName(), (lines.size() + 2), "Line does not have the required field count");
            }
            lines.add(parsedLine);
        }
    }

    protected String[] parseCsv(String line) {
        try {
            // Cannot re-use this parser - it is stateful
            String[] strings = new CSVParserBuilder()
                .withSeparator(',')
                .build().parseLine(line);
            for (int i = 0; i < strings.length; i++) {
                // If we don't do this, the files will take up lots and lots of memory,
                // since they often contain the same long string many times.
                strings[i] = strings[i].intern();
            }
            return strings;
        } catch (IOException e) {
            throw new GtfsReadException(getFileName(), (lines.size() + 2), "Error parsing CSV", e);
        }
    }

    protected boolean isFilePresent(FileSystem zipFs) {
        return Files.isRegularFile(getFsPath(zipFs));
    }

    public void readFrom(FileSystem zipFs) throws IOException {
        try {
            Files.lines(getFsPath(zipFs), StandardCharsets.UTF_8)
                .parallel()
                .map(line -> {
                    // Java cannot handle BOM
                    if (line.codePointAt(0) == 0xfeff) {
                        return line.substring(1);
                    } else {
                        return line;
                    }
                })
                // most time when reading is spend doing decoding of CSV and string interning
                .map(this::parseCsv)
                .forEachOrdered(this::addLine);
        } catch (GtfsReadException e) {
            throw new IOException("Error reading file in " + zipFs, e);
        }
    }

    private Path getFsPath(FileSystem zipFs) {
        return zipFs.getPath(getFileName());
    }

    public long getDataLinesCount() {
        return lines.size();
    }


    protected <T> Stream<T> streamLinesAs(Function<LineAccess, T> producer) {
        return streamLinesAs(__ -> true, producer);
    }

    protected <T> Stream<T> streamLinesAs(Predicate<LineAccess> filter, Function<LineAccess, T> producer) {
        return IntStream.range(0, lines.size())
            .parallel() // < Files are very large
            .<Optional<T>>mapToObj(line -> {
                try {
                    LineAccess access = fieldName -> fieldName.apply(lines.get(line));
                    if (filter.test(access)) {
                        return Optional.of(producer.apply(access));
                    } else {
                        return Optional.empty();
                    }
                } catch (Throwable t) {
                    throw new GtfsReadException(getFileName(), line + 2,
                        "Error converting file", t);
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    public FieldAccessor getOptionalAccessor(String fieldName, String defaultValue) {
        int index = Arrays.asList(fieldNames).indexOf(fieldName);
        if (index < 0) {
            return new DefaultFieldAccessor(defaultValue, fieldName);
        } else {
            return new IndexFieldAccessorWithDefault(index, fieldName, defaultValue);
        }
    }

    public FieldAccessor getRequiredAccessor(String fieldName) {
        int index = Arrays.asList(fieldNames).indexOf(fieldName);
        if (index < 0) {
            throw new GtfsReadException(getFileName(), 1, "There is no such field " + fieldName
                + ". Available fields are: " + String.join(", ", fieldNames));
        }
        return new IndexFieldAccessor(index, fieldName);
    }

    interface LineAccess {
        String get(FieldAccessor fieldName);
    }

    interface FieldAccessor {
        String apply(String[] strings);
        String getFieldName();
    }

    private static class IndexFieldAccessor implements FieldAccessor {
        private final int index;
        private final String fieldName;

        public IndexFieldAccessor(int index, String fieldName) {
            this.index = index;
            this.fieldName = fieldName;
        }

        @Override
        public String apply(String[] strings) {
            return strings[index];
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }
    }

    private static class IndexFieldAccessorWithDefault extends IndexFieldAccessor {

        private final String defaultValue;

        public IndexFieldAccessorWithDefault(int index, String fieldName, String defaultValue) {
            super(index, fieldName);
            this.defaultValue = defaultValue;
        }

        @Override
        public String apply(String[] strings) {
            String value = super.apply(strings);
            if (value.isEmpty()) {
                return defaultValue;
            } else {
                return value;
            }
        }

    }

    private static class DefaultFieldAccessor implements FieldAccessor {
        private final String defaultValue;
        private final String fieldName;

        public DefaultFieldAccessor(String defaultValue, String fieldName) {
            this.defaultValue = defaultValue;
            this.fieldName = fieldName;
        }

        @Override
        public String apply(String[] strings) {
            return defaultValue;
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }
    }
}
