
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2008, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.dcs;

import org.apache.log4j.Level;
import org.mortbay.log.Logger;

/**
 * A Log4j implementation of Jetty logger.
 */
public class Log4jJettyLog implements org.mortbay.log.Logger
{
    private org.apache.log4j.Logger log;

    public Log4jJettyLog()
    {
        log = org.apache.log4j.Logger.getLogger("jetty");
    }

    public Log4jJettyLog(org.apache.log4j.Logger log)
    {
        this.log = log;
    }

    public Logger getLogger(String name)
    {
        return new Log4jJettyLog(org.apache.log4j.Logger.getLogger(name));
    }

    public boolean isDebugEnabled()
    {
        return log.isDebugEnabled();
    }

    public void setDebugEnabled(boolean debug)
    {
        if (debug)
        {
            log.setLevel(Level.DEBUG);
        }

        // No good way to undo when debug == false?
    }

    public void debug(String text, Throwable throwable)
    {
        log.debug(text, throwable);
    }

    public void debug(String text, Object arg1, Object arg2)
    {
        log.debug(merge(text, arg1, arg2));
    }

    public void info(String text, Object arg1, Object arg2)
    {
        log.info(merge(text, arg1, arg2));
    }

    public void warn(String text, Throwable throwable)
    {
        log.warn(text, throwable);
    }

    public void warn(String text, Object arg1, Object arg2)
    {
        log.warn(merge(text, arg1, arg2));
    }

    private String merge(String text, Object o1, Object o2)
    {
        final StringBuilder builder = new StringBuilder();
        if (text != null)
        {
            builder.append("text");
        }
        if (o1 != null)
        {
            builder.append(", ");
            builder.append(o1.toString());
        }
        if (o2 != null)
        {
            builder.append(", ");
            builder.append(o2.toString());
        }

        return builder.toString();
    }
}
