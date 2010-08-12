/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.hadoop.base;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;

import java.io.IOException;

/**
 * This is the main point of entry for the hadoop file processing application.
 *
 * @author: Bill Branan
 * Date: Aug 5, 2010
 */
public class JobRunner {

    private static String inputPath;
    private static String outputPath;

    /**
     * Main method that sets up file processing job.
     */
    public static void main(String[] args) throws Exception {

        CommandLine cmd = processArgs(args);

        inputPath = cmd.getOptionValue("input");
        inputPath = appendTrailingSlash(inputPath);

        outputPath = cmd.getOptionValue("output");
        outputPath = appendTrailingSlash(outputPath);

        runJob();
    }

    /*
     * Construct and run the job.
     */
    private static void runJob() throws IOException, java.text.ParseException {
        JobBuilder jobBuilder = getJobBuilder();
        JobConf jobConf = jobBuilder.getJobConf();
        JobClient.runJob(jobConf);
    }

    /**
     * Creates a job builder which is responsible for creating a hadoop job
     * which can be run.
     * <p/>
     * This method can be overridden to provide an alternate job builder.
     */
    protected static JobBuilder getJobBuilder() {
        return new JobBuilder(inputPath, outputPath);
    }

    /**
     * Process the command line arguments
     */
    protected static CommandLine processArgs(String[] args) {
        Options options = createOptions();
        CommandLine cmd = null;
        
        try {
            cmd = new PosixParser().parse(options, args);
        } catch (ParseException ex) {
            System.out.println("Error parsing the options");
            printHelpText(options);
        }

        if (!cmd.hasOption("input") || !cmd.hasOption("output")) {
            printHelpText(options);
        }

        return cmd;
    }

    private static String appendTrailingSlash(String inputPath) {
        if (!inputPath.endsWith("/")) {
            inputPath += "/";
        }
        return inputPath;
    }

    private static void printHelpText(Options options) {
        String helpText = "Program arguments must include values for" +
                          "both input and output paths";
        (new HelpFormatter()).printHelp(helpText, options);
        throw new RuntimeException(helpText);
    }

    private static Options createOptions() {
        Options options = new Options();

        String inputDesc = "Path to the files that you wish to process " +
                           "e.g. s3n://<mybucket>/inputfiles/";
        Option inputOption = new Option("i", "input", true, inputDesc);
        inputOption.setRequired(true);
        options.addOption(inputOption);

        String outputDesc = "Path to the location where output files should " +
                            "be written e.g. s3n://<mybucket>/outputfiles/";
        Option outputOption = new Option("o", "output", true, outputDesc);
        outputOption.setRequired(true);
        options.addOption(outputOption);

        return options;
    }

}
