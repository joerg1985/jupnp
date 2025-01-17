/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.transport.impl;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation based on a single shared (receive/send) UDP <code>MulticastSocket</code>.
 * <p>
 * Although we do not receive multicast datagrams with this service, sending multicast
 * datagrams with a configuration time-to-live requires a <code>MulticastSocket</code>.
 * </p>
 * <p>
 * Thread-safety is guaranteed through synchronization of methods of this service and
 * by the thread-safe underlying socket.
 * </p>
 * 
 * @author Christian Bauer
 * @author Kai Kreuzer - added configurable port for search responses
 * @author Jochen Hiller - add more diagnostic information in case of an general communication exception
 */
public class DatagramIOImpl implements DatagramIO<DatagramIOConfigurationImpl> {

    private Logger log = LoggerFactory.getLogger(DatagramIOImpl.class);

    /*
     * Implementation notes for unicast/multicast UDP:
     * 
     * http://forums.sun.com/thread.jspa?threadID=771852
     * http://mail.openjdk.java.net/pipermail/net-dev/2008-December/000497.html
     * https://jira.jboss.org/jira/browse/JGRP-978
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4701650
     * 
     */

    protected final DatagramIOConfigurationImpl configuration;

    protected Router router;
    protected DatagramProcessor datagramProcessor;

    protected InetSocketAddress localAddress;
    protected MulticastSocket socket; // For sending unicast & multicast, and reveiving unicast

    public DatagramIOImpl(DatagramIOConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public DatagramIOConfigurationImpl getConfiguration() {
        return configuration;
    }

    public synchronized void init(InetAddress bindAddress, int bindPort, Router router,
            DatagramProcessor datagramProcessor) throws InitializationException {

        this.router = router;
        this.datagramProcessor = datagramProcessor;

        try {

            // TODO: UPNP VIOLATION: The spec does not prohibit using the 1900 port here again, however, the
            // Netgear ReadyNAS miniDLNA implementation will no longer answer if it has to send search response
            // back via UDP unicast to port 1900... so we use an ephemeral port
            log.debug("Creating bound socket (for datagram input/output) on: {}:{}", bindAddress, bindPort);
            localAddress = new InetSocketAddress(bindAddress, bindPort);
            socket = new MulticastSocket(localAddress);
            socket.setTimeToLive(configuration.getTimeToLive());
            socket.setReceiveBufferSize(262144); // Keep a backlog of incoming datagrams if we are not fast enough
        } catch (Exception ex) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName(), ex);
        }
    }

    public synchronized void stop() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void run() {
        log.debug("Entering blocking receiving loop, listening for UDP datagrams on: {}:{}", socket.getLocalAddress(),
                socket.getPort());

        while (true) {

            try {
                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                DatagramPacket datagram = new DatagramPacket(buf, buf.length);

                socket.receive(datagram);

                log.debug("UDP datagram received from: {}:{} on: {}", datagram.getAddress().getHostAddress(),
                        datagram.getPort(), localAddress);

                router.received(datagramProcessor.read(localAddress.getAddress(), datagram));

            } catch (SocketException ex) {
                log.debug("Socket closed");
                break;
            } catch (UnsupportedDataException ex) {
                log.info("Could not read datagram: {}", ex.getMessage());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            if (!socket.isClosed()) {
                log.debug("Closing unicast socket");
                socket.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void send(OutgoingDatagramMessage message) {
        log.debug("Sending message from address: {}", localAddress);

        DatagramPacket packet = datagramProcessor.write(message);

        log.debug("Sending UDP datagram packet to: {}:{}", message.getDestinationAddress(),
                message.getDestinationPort());

        send(packet);
    }

    public synchronized void send(DatagramPacket datagram) {
        log.debug("Sending message from address: {}", localAddress);

        try {
            socket.send(datagram);
        } catch (SocketException ex) {
            log.debug("Socket closed, aborting datagram send to: {}", datagram.getAddress());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Exception sending datagram to: {}", datagram.getAddress(), ex);
            log.error("  Details: datagram.socketAddress={}, length={}, offset={}, data.bytes={}",
                    datagram.getSocketAddress(), datagram.getLength(), datagram.getOffset(), datagram.getData().length);
            try {
                log.error(
                        "  Details: socket={}, closed={}, bound={}, inetAddress={}, "
                                + "remoteSocketAddress={}, networkInterface={}",
                        socket.toString(), socket.isClosed(), socket.isBound(), socket.getInetAddress(),
                        socket.getRemoteSocketAddress(), socket.getNetworkInterface());
            } catch (SocketException ex2) {
                log.error("  Details: could not get network interface", ex2);
            }
        }
    }
}
