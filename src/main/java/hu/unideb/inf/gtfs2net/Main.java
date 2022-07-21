package hu.unideb.inf.gtfs2net;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String... args) {
        Options options = new Options();

        Option gtfs = new Option("i", "input", true, "gtfs input(s) (required)");
        gtfs.setRequired(true);
        options.addOption(gtfs);

        Option type = new Option("t", "type", true, "gtfs input type {zip|dir|dirs} (zip by default, optional)");
        type.setRequired(false);
        options.addOption(type);

        Option radius = new Option("r", "radius", true, "node merging radius (default 150)");
        radius.setRequired(false);
        options.addOption(radius);

        Option step = new Option("s", "step", true, "node merging radius step (default 150)");
        step.setRequired(false);
        options.addOption(step);

        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves it purpose

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("gtfs2net", options);
            System.exit(1);
        }

        String gtfsPath = cmd.getOptionValue('i');
        String gtfsType = cmd.getOptionValue('t');
        if (gtfsType==null||!gtfsType.equals("dir")&&!gtfsType.equals("dirs")) gtfsType="zip";
        int r;
        try{
            r=Integer.parseInt(cmd.getOptionValue('r'));
            if (r<0) throw new Exception();
        }catch (Exception e){
            r=150;
        }
        int rs;
        try{
            rs=Integer.parseInt(cmd.getOptionValue('r'));
            if ((rs<0)||(rs>r)) throw new Exception();
        }catch (Exception e){
            rs=150;
        }

        System.out.println("Processing " + gtfsPath + " as " + gtfsType +". r=" + r + ", s=" + rs);

        GTFSTools.ProcessConfig pc = new GTFSTools.ProcessConfig.ProcessConfigBuilder(gtfsPath).withRadius(r).withRadiusStep(rs).build();
        //GTFSTools.processFolder(pc);
    }
}

