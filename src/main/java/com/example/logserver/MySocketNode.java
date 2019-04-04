package com.example.logserver;

/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.server.HardenedLoggingEventInputStream;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketAddress;

// Contributors: Moses Hohman <mmhohman@rainbow.uchicago.edu>

/**
 * Read {@link ILoggingEvent} objects sent from a remote client using Sockets
 * (TCP). These logging events are logged according to local policy, as if they
 * were generated locally.
 *
 * <p>
 * For example, the socket node might decide to log events to a local file and
 * also resent them to a second socket node.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 *
 * @since 0.8.4
 */
public class MySocketNode implements Runnable {

    Socket socket;
    LoggerContext context;
    HardenedLoggingEventInputStream hardenedLoggingEventInputStream;
    SocketAddress remoteSocketAddress;

    Logger logger;
    boolean closed = false;
    MySimpleSocketServer socketServer;

    public MySocketNode(MySimpleSocketServer socketServer, Socket socket, LoggerContext context) {
        this.socketServer = socketServer;
        this.socket = socket;
        remoteSocketAddress = socket.getRemoteSocketAddress();
        this.context = context;
        logger = context.getLogger(MySocketNode.class);
    }

    // public
    // void finalize() {
    // System.err.println("-------------------------Finalize called");
    // System.err.flush();
    // }

    public void run() {

        try {
            hardenedLoggingEventInputStream = new HardenedLoggingEventInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (Exception e) {
            logger.error("Could not open ObjectInputStream to " + socket, e);
            closed = true;
        }

        ILoggingEvent event;
        Logger remoteLogger=null;

        try {
            while (!closed) {
                // read an event from the wire
                event = (ILoggingEvent) hardenedLoggingEventInputStream.readObject();
                // get a logger from the hierarchy. The name of the logger is taken to
                // be the name contained in the event.
                String appLogContextName=event.getLoggerContextVO().getName() ;
                String[] contextArr=appLogContextName.split("/");
                String loggerName=AppLogUtil.appLogMap.get(contextArr[0]);
                String host=contextArr[1];

                if(!StringUtils.isEmpty(loggerName)){
                    remoteLogger=context.getLogger(loggerName);
                }
                if(remoteLogger==null){//找不到对应的logger
                    continue;
                }

                // apply the logger-level filter
                if (remoteLogger.isEnabledFor(event.getLevel())) {
                    // finally log the event as if was generated locally
                    remoteLogger.callAppenders(event);
                }
            }
        } catch (java.io.EOFException e) {
            logger.info("Caught java.io.EOFException closing connection.");
        } catch (java.net.SocketException e) {
            logger.info("Caught java.net.SocketException closing connection.");
        } catch (IOException e) {
            logger.info("Caught java.io.IOException: " + e);
            logger.info("Closing connection.");
        } catch (Exception e) {
            logger.error("Unexpected exception. Closing connection.", e);
        }

        socketServer.socketNodeClosing(this);
        close();
    }

    void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (hardenedLoggingEventInputStream != null) {
            try {
                hardenedLoggingEventInputStream.close();
            } catch (IOException e) {
                logger.warn("Could not close connection.", e);
            } finally {
                hardenedLoggingEventInputStream = null;
            }
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + remoteSocketAddress.toString();
    }
}

