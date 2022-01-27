package hu.unideb.inf.gtfs2net;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static PrintWriter pw;

    public static void main(String args[]) throws IOException {
        PrintWriter pw;
        try {
            File file = new File("../gtfs2");
            String[] directories = file.list((File current, String name) -> new File(current, name).isDirectory());
            //directories = new String[]{"nyc"};
            for (String directory : directories) {
                String sourcePath = "../gtfs2/" + directory;
                System.out.println("Processing: " + sourcePath);
                for (int r = 0; r <= 150; r += 150) {
                    Map<String, GTFSTools.Stop> stops = GTFSTools.readStops(sourcePath);
                    GTFSTools.registerCloseStopsAsOne(stops, r);
                    GTFSTools.readStopTimes(sourcePath, stops);
                    GTFSTools.printStopsAsNetworkToFile(stops, "gtfs2/"+ directory + "_" + r + ".txt");

                    pw = new PrintWriter(new FileOutputStream("gtfs2/nodenum_"+directory+".txt", true));
                    System.out.println(r + ", " + Files.lines(Path.of("gtfs2/"+ directory + "_" + r + ".txt"), StandardCharsets.UTF_8).count());
                    pw.println(r + ", " + Files.lines(Path.of("gtfs2/"+ directory + "_" + r + ".txt"), StandardCharsets.UTF_8).count());
                    pw.close();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private static void printStops(Map<String, GTFSTools.Stop> stops) {
        try {
            pw = new PrintWriter(new FileOutputStream("log_nyc_test.txt", false));
            for (GTFSTools.Stop stop : stops.values()) {
                pw.println(stop.stopID + "," + stop.stopName +"," + stop.isStation + "," + stop.hasParentStation() + "," + stop.parentStation+ "," + stop.lat+ "," + stop.lon + "," + stop.neighbors.toString().replaceAll(",", ";"));
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

