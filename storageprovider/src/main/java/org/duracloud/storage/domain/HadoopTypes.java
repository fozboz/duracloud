/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.storage.domain;

/**
 * This class contains constants for running hadoop jobs.
 *
 * @author Andrew Woods
 *         Date: Sep 28, 2010
 */
public class HadoopTypes {
    public static final String RUN_HADOOP_TASK_NAME = "run-hadoop-job";
    public static final String STOP_JOB_TASK_NAME = "stop-hadoop-job";
    public static final String DESCRIBE_JOB_TASK_NAME = "describe-hadoop-job";

    public enum TASK_PARAMS {
        JOB_TYPE,
        WORKSPACE_ID,
        BOOTSTRAP_CONTENT_ID,
        JAR_CONTENT_ID,
        SOURCE_SPACE_ID,
        DEST_SPACE_ID,
        INSTANCE_TYPE,
        NUM_INSTANCES,
        MAPPERS_PER_INSTANCE,
        // image conversion params
        DEST_FORMAT,
        COLOR_SPACE,
        NAME_PREFIX,
        NAME_SUFFIX;
    }

    public enum TASK_OUTPUTS {
        JOB_FLOW_ID,
        RESULTS;
    }

    public enum INSTANCES {
        SMALL("Small Instance", "m1.small"),
        LARGE("Large Instance", "m1.large"),
        XLARGE("Extra Large Instance", "m1.xlarge");

        private String desc;
        private String id;

        INSTANCES(String desc, String id) {
            this.desc = desc;
            this.id = id;
        }

        public String getDescription() {
            return desc;
        }

        public String getId() {
            return id;
        }
    }

}
