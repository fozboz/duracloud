/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.duradmin.spaces.controller;

import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.s3task.streaming.DisableStreamingTaskRunner;
import org.duracloud.s3task.streaming.EnableStreamingTaskRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * @author Daniel Bernstein Date: April 2, 2012
 */
@Controller
@RequestMapping(value = "/spaces/mediastreamer")
public class MediaStreamingTaskController {

    protected static final String STREAMING_ENABLED_KEY = "streamingEnabled";
    protected final Logger log =
        LoggerFactory.getLogger(MediaStreamingTaskController.class);

    private ContentStoreManager contentStoreManager;

    @Autowired
    public MediaStreamingTaskController(ContentStoreManager contentStoreManager){
        this.contentStoreManager = contentStoreManager;
    }
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView post(@RequestParam(required = true) String storeId,
                             @RequestParam(required = true) String spaceId,
                             @RequestParam(required = true) boolean enable)
        throws Exception {
        try{
            
            String action =
                enable
                    ? EnableStreamingTaskRunner.TASK_NAME
                    : DisableStreamingTaskRunner.TASK_NAME;

            ContentStore store = this.contentStoreManager.getContentStore(storeId);
            
            store.performTask(action, spaceId);
            
            log.info("successfully "
                + (enable ? "enabled" : "disabled")
                + " the stream service for space (" + spaceId
                + ") on storage provider (" + storeId + ")");
            ModelAndView mav =
                new ModelAndView("jsonView", STREAMING_ENABLED_KEY, enable);
            return mav;
            
        }catch(Exception ex){
            throw new DuraCloudRuntimeException(ex);
        }
    }
 
}