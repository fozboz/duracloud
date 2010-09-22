/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.s3task.streaming;

import com.amazonaws.services.s3.AmazonS3Client;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.storage.provider.TaskRunner;
import org.jets3t.service.CloudFrontService;
import org.jets3t.service.CloudFrontServiceException;
import org.jets3t.service.model.cloudfront.StreamingDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Bill Branan
 * Date: Jun 1, 2010
 */
public abstract class BaseStreamingTaskRunner implements TaskRunner {

    protected S3StorageProvider s3Provider;
    protected AmazonS3Client s3Client;
    protected CloudFrontService cfService;

    public abstract String getName();

    public abstract String performTask(String taskParameters);

    /*
     * Extracts the spaceId value from the provided task parameters
     */
    protected String getSpaceId(String taskParameters) {
        if(taskParameters != null && !taskParameters.equals("")) {
            return taskParameters;
        } else {
            throw new RuntimeException("A Space ID must be provided");
        }
    }

    /*
     * Determines if a streaming distribution already exists for a given bucket
     */
    protected StreamingDistribution getExistingDistribution(String bucketName)
        throws CloudFrontServiceException {

        StreamingDistribution[] distributions =
            cfService.listStreamingDistributions();

        if(distributions != null) {
            for(StreamingDistribution dist : distributions) {
                if(bucketName.equals(dist.getOriginAsBucketName())) {
                    return dist;
                }
            }
        }

        return null;
    }

    /*
     * Determines if a streaming distribution already exists for a given bucket
     */
    protected List<StreamingDistribution> getAllExistingDistributions(String bucketName)
        throws CloudFrontServiceException {

        StreamingDistribution[] distributions =
            cfService.listStreamingDistributions();

        List<StreamingDistribution> distList =
            new ArrayList<StreamingDistribution>();

        for(StreamingDistribution dist : distributions) {
            if(bucketName.equals(dist.getOriginAsBucketName())) {
                distList.add(dist);
            }
        }

        return distList;
    }
}
