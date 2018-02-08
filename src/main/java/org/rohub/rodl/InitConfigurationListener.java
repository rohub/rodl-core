
package org.rohub.rodl;

/*-
 * #%L
 * ROHUB
 * %%
 * Copyright (C) 2010 - 2018 PSNC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;
import org.rohub.rodl.monitoring.MonitoringScheduler;

/**
 * Initialize RODL on startup.
 * 
 * @author piotrekhol
 * 
 */
public class InitConfigurationListener implements ServletContextListener {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(InitConfigurationListener.class);


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ApplicationProperties.load(sce.getServletContext().getContextPath());
        try {
            MonitoringScheduler.getInstance().start();
        } catch (SchedulerException e) {
            LOGGER.error("Can't start the RO monitoring scheduler", e);
        }
        
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            MonitoringScheduler.getInstance().stop();
        } catch (SchedulerException e) {
            LOGGER.error("Can't stop the RO monitoring scheduler", e);
        }
    }

}
