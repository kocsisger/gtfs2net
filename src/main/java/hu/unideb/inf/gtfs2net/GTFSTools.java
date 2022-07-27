package hu.unideb.inf.gtfs2net;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GTFSTools {
    private static Logger logger = Logger.getLogger("GTFSTools.class");

    static {
        logger.setLevel(Level.FINE);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.WARNING);
    }

    public static void setVerboseConsoleLogLevel() {
        Logger.getLogger("").getHandlers()[0].setLevel(Level.INFO);
    }

    @Data
    @AllArgsConstructor
    public static class Stop {
        String stopID;
        Set<String> neighbors;
        boolean isStation;
        String parentStation;
        int stopIDInt;
        String stopName;
        double lat;
        double lon;

        public boolean hasParentStation() {
            return !parentStation.isBlank();
        }
    }

    public static String getStopAsIntString(Stop stop, Map<String, Stop> stops) {
        String result = "" + stop.stopIDInt + " -> ";
        for (String n : stop.getNeighbors()) {
            result += stops.get(n).stopIDInt + " ";
        }
        return result;
    }

    private static int stopID_int = 0;

    public static class ProcessConfig {
        private final String inputFolderPath;
        private final String outputFolderPath;
        private final int radius;
        private final int radiusStep;

        private ProcessConfig(ProcessConfigBuilder pcb) {
            this.inputFolderPath = pcb.inputFolderPath;
            this.outputFolderPath = pcb.outputFolderPath;
            this.radius = pcb.radius;
            this.radiusStep = pcb.radiusStep;
        }

        public static class ProcessConfigBuilder {
            private final String inputFolderPath;
            private String outputFolderPath="./gtfs2netOutput";
            private int radius = 0;
            private int radiusStep = 1;

            public ProcessConfigBuilder(String inputFolderPath) {
                this.inputFolderPath = inputFolderPath;
            }

            public ProcessConfigBuilder withRadius(int radius) {
                this.radius = radius;
                return this;
            }

            public ProcessConfigBuilder withRadiusStep(int radiusStep) {
                this.radiusStep = radiusStep;
                return this;
            }

            public ProcessConfigBuilder withoutputFolderPath(String outputFolderPath) {
                this.outputFolderPath = outputFolderPath;
                return this;
            }

            public ProcessConfig build() {
                ProcessConfig pc = new ProcessConfig(this);
                return pc;
            }
        }
    }


    public static void processFolders(String folderPath) {
        GTFSTools.ProcessConfig pc = new GTFSTools.ProcessConfig.ProcessConfigBuilder(folderPath)
                .withRadius(150)
                .withRadiusStep(150)
                .build();
        processFolders(pc);
    }

    public static void processFolders(ProcessConfig pc) {
        File file = new File(pc.inputFolderPath);
        String[] directories = file.list((File current, String name) -> new File(current, name).isDirectory());
        if (directories == null) return;
        for (String directory : directories) {
            logger.log(Level.INFO, "Processing: " + pc.inputFolderPath + "/" + directory);
            GTFSTools.ProcessConfig pc2 = new GTFSTools.ProcessConfig.ProcessConfigBuilder(pc.inputFolderPath + "/" + directory)
                    .withRadius(pc.radius)
                    .withRadiusStep(pc.radiusStep)
                    .withoutputFolderPath(pc.outputFolderPath)
                    .build();
            processGTFSFolder(pc2);
        }
    }

    public static void processGTFSFolder(ProcessConfig pc) {
        logger.log(Level.INFO,"Input: " + pc.inputFolderPath + "\nOutput: " + pc.outputFolderPath);
        PrintWriter pw;
        try {
            File outputFolder = new File(pc.outputFolderPath);
            if (!outputFolder.exists()) outputFolder.mkdirs();
        } catch(Exception e) {
            logger.log(Level.WARNING, "Error while creating or reading output folder: " + pc.outputFolderPath);
            logger.log(Level.INFO, e.getMessage() + e.getStackTrace());
        }
        try {
            for (int r = 0; r <= pc.radius; r += pc.radiusStep) {
                Map<String, GTFSTools.Stop> stops = readStops(pc.inputFolderPath);
                registerCloseStopsAsOne(stops, r);
                readStopTimes(pc.inputFolderPath, stops);
                String filenamePrefix = pc.inputFolderPath.substring(pc.inputFolderPath.lastIndexOf('/')+1);
                printStopsAsNetworkToFile(stops, pc.outputFolderPath + "/" + filenamePrefix + "_" + r + ".txt");
                pw = new PrintWriter(new FileOutputStream(pc.outputFolderPath + "/nodenum_" + filenamePrefix + ".txt", true));
                logger.log(Level.INFO,"r: " + r + ", nodenum: " + Files.lines(Path.of(pc.outputFolderPath + "/" + filenamePrefix + "_" + r + ".txt"), StandardCharsets.UTF_8).count());
                pw.println(r + ", " + Files.lines(Path.of(pc.outputFolderPath + "/" + filenamePrefix + "_" + r + ".txt"), StandardCharsets.UTF_8).count());
                pw.close();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while processing " + pc.inputFolderPath + "... Skipping to the next one.");
            logger.log(Level.FINE, e.getMessage() + e.getStackTrace().toString());
        }
    }

    public static Map<String, Stop> readStops(String filename) throws FileNotFoundException {
        stopID_int = 0;
        filename += "/stops.txt";
        Map<String, Stop> records = new HashMap<>();
        Scanner fsc = new Scanner(new File(filename), "UTF-8");
        String complex_regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; //the complex regex is used to ignore commas in quotation marks
        String[] columns = fsc.nextLine().split(complex_regex, -1);
        String[] fields;
        int id_idx, station_idx, parentStation_idx, name_idx, lat_idx, lon_idx;
        id_idx = Arrays.asList(columns).indexOf("stop_id");
        station_idx = Arrays.asList(columns).indexOf("location_type");
        parentStation_idx = Arrays.asList(columns).indexOf("parent_station");
        name_idx = Arrays.asList(columns).indexOf("stop_name");
        lat_idx = Arrays.asList(columns).indexOf("stop_lat");
        lon_idx = Arrays.asList(columns).indexOf("stop_lon");

        while (fsc.hasNextLine()) {
            String line = fsc.nextLine();
            fields = (line + ",--limit--").split(complex_regex, -1); //--limit-- is used since split doesn't read the empty strings at the end othervise
            boolean station = false;
            String parentStation = "";
            try {
                station = fields[station_idx].equals("1");
                parentStation = fields[parentStation_idx];
            } catch (ArrayIndexOutOfBoundsException ex) {
                logger.log(Level.ALL, "Exception: " + ex.getMessage());
                station = false;
                parentStation = "";
            }

            try {
                records.put(fields[id_idx], new Stop(fields[id_idx], new HashSet<>(), station, parentStation, stopID_int++, fields[name_idx], Double.parseDouble(fields[lat_idx]), Double.parseDouble(fields[lon_idx])));
            } catch (NumberFormatException ex) {
                logger.log(Level.ALL, "Exception: " + ex.getMessage());
                records.put(fields[id_idx], new Stop(fields[id_idx], new HashSet<>(), station, parentStation, stopID_int++, fields[name_idx], Double.parseDouble(fields[lat_idx].replaceAll("\"", "")), Double.parseDouble(fields[lon_idx].replaceAll("\"", ""))));
            }
        }
        fsc.close();
        return records;
    }

    public static void readStopTimes(String filename, Map<String, Stop> stops) throws FileNotFoundException {
        filename += "/stop_times.txt";
        Scanner fsc = new Scanner(new File(filename), "UTF-8");
        String complex_regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; //the complex regex is used to ignore commas in quotation marks
        String[] columns = fsc.nextLine().split(complex_regex, -1);
        String[] fields;
        int id_idx, seq_idx, prev_seq = Integer.MAX_VALUE, act_seq;
        String prevStop = "";
        String actStop = "";
        id_idx = Arrays.asList(columns).indexOf("stop_id");
        seq_idx = Arrays.asList(columns).indexOf("stop_sequence");
        while (fsc.hasNextLine()) {
            String line = fsc.nextLine();
            fields = (line + ",--limit--").split(complex_regex, -1); //--limit-- is used since split doesn't read the empty strings at the end othervise
            try {
                act_seq = Integer.parseInt(fields[seq_idx]);
            } catch (NumberFormatException numberFormatException) {
                //System.err.println(numberFormatException.getMessage());
                logger.log(Level.ALL, "Exception: " + numberFormatException.getMessage());
                act_seq = Integer.parseInt(fields[seq_idx].replaceAll("\"", ""));
            }
            actStop = fields[id_idx];
            if (act_seq > prev_seq) {
                try {
                    //If the act stop has a parent station use the parent station
                    if (stops.get(actStop).hasParentStation()) actStop = stops.get(actStop).parentStation;
                    stops.get(prevStop).getNeighbors().add(actStop);
                } catch (NullPointerException npe) {
                    //System.err.println(act_seq + " " + prev_seq);
                    logger.log(Level.ALL, "Exception: " + act_seq + " " + prev_seq);
                }
            }
            prev_seq = act_seq;
            prevStop = actStop;
        }
        fsc.close();
    }

    public static void printStops(Map<String, Stop> stops) {
        for (String stopID : stops.keySet()) {
            if (stops.get(stopID).isStation()) {
                System.out.print(stopID + " - " + stops.get(stopID).getNeighbors() + " || ");
                System.out.println(GTFSTools.getStopAsIntString(stops.get(stopID), stops));
            }
        }
    }

    public static void printStopsAsNetworkToFile(Map<String, Stop> stops, String filename) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(filename);
        for (Stop from : stops.values()) {
            //If from has a parent station then in the file that station is to be presented.
            //The connections of "from" has been added to the parent of "from" in the method readStopTimes
            if (from.hasParentStation()) continue;

            for (String to : from.getNeighbors()) {
                pw.println(from.stopIDInt + "," + stops.get(to).stopIDInt + "," + from.getStopName() + "," + stops.get(to).getStopName());
            }
        }
        pw.close();
    }

    private static double distance(Stop stop1, Stop stop2) {  // generally used geo measurement function
        double lat1, lon1, lat2, lon2;
        lat1 = stop1.getLat();
        lon1 = stop1.getLon();
        lat2 = stop2.getLat();
        lon2 = stop2.getLon();
        var R = 6378.137; // Radius of earth in KM
        var dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        var dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        var d = R * c;
        return d * 1000; // meters
    }

    private static double lonDistance(Stop stop1, Stop stop2) {
        double lat1, lon1, lat2, lon2;
        lat1 = 0;
        lon1 = stop1.getLon();
        lat2 = 0;
        lon2 = stop2.getLon();
        var R = 6378.137; // Radius of earth in KM
        var dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        var dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        var d = R * c;
        return d * 1000; // meters
    }

    public static ArrayList<Stop> getStopsAsSortedList(Map<String, Stop> stops) {
        ArrayList listResult = new ArrayList();
        listResult.addAll(stops.values());
        listResult.sort((s1, s2) -> (int) Math.signum(((Stop) s1).getLon() - ((Stop) s2).getLon()));

        return listResult;
    }

    public static void registerCloseStopsAsOne(Map<String, Stop> stops, double radius) {
        int virtualParentID = 0;
        ArrayList<Stop> stopsList = getStopsAsSortedList(stops);
        ArrayList<Boolean> stopsProcessed = new ArrayList<>();
        for (int i = 0; i < stopsList.size(); i++) {
            stopsProcessed.add(false);
        }

        for (int actIdx = 0; actIdx < stopsList.size(); actIdx++) {
            if (stopsProcessed.get(actIdx)) continue;
            Set<Stop> closeStops = findCloseStopsOf(actIdx, stopsList, stopsProcessed, radius);
            closeStops.add(stopsList.get(actIdx));

            Stop virtualParent = null;

            for (Stop stop : closeStops) {
                try {
                    if (stop.hasParentStation()) {
                        if (stops.get(stop.parentStation).stopName.contains("VirtualStation")) {
                            virtualParent = stops.get(stop.parentStation);
                            break;
                        }
                    }
                } catch (NullPointerException exception) {
                    logger.log(Level.ALL, "Exception: " + exception.getMessage() + "\n" + stop.parentStation);
                }
            }

            if (virtualParent == null) {
                virtualParentID++;
                virtualParent = new Stop("Virt" + virtualParentID, new HashSet<>(), true, "", stopID_int++, "VirtualStation" + virtualParentID, 0, 0);
                stops.put(virtualParent.getStopName(), virtualParent);
            }
            for (Stop stop : closeStops) {
                stop.setParentStation(virtualParent.getStopName());
            }
        }
    }

    private static Set<Stop> findCloseStopsOf(int actIdx, ArrayList<Stop> stops, ArrayList<Boolean> stopsProcessed, double radius) {
        Set<Stop> closeStops = new HashSet<>();

        int bottom, top;
        bottom = actIdx;
        while (true) {
            bottom--;
            if ((bottom < 0) || (lonDistance(stops.get(actIdx), stops.get(bottom)) > radius)) break;
        }
        top = actIdx;
        while (true) {
            top++;
            if ((top > stops.size() - 1) || (lonDistance(stops.get(actIdx), stops.get(top)) > radius)) break;
        }
        bottom++;
        top--;

        for (int i = bottom; i <= top; i++) {
            if ((!stopsProcessed.get(i)) && (distance(stops.get(actIdx), stops.get(i)) <= radius)) {
                closeStops.add(stops.get(i));
                stopsProcessed.set(i, true);
            }
        }
        Set<Stop> stopsToCheck = new HashSet<>();
        stopsToCheck.addAll(closeStops);
        Set<Stop> newCloseStops = new HashSet<>();

        while (true) {
            for (Stop stop : stopsToCheck) {
                int nextIdx = stops.indexOf(stop);
                newCloseStops.addAll(findCloseStopsOf(nextIdx, stops, stopsProcessed, radius));
            }
            if (newCloseStops.isEmpty()) break;
            closeStops.addAll(newCloseStops);
            stopsToCheck.clear();
            stopsToCheck.addAll(newCloseStops);
            newCloseStops.clear();
        }

        return closeStops;
    }
}
