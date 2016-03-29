/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 robert.gruendler@dubture.com
 *               2016 Maxim Biro <nurupo.contributions@gmail.com>
 *
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
 */

package com.dubture.jenkins.digitalocean;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.SlaveComputer;
import hudson.util.TimeUnit2;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ComputerLauncher extends hudson.slaves.ComputerLauncher {

    @Override
    public void launch(SlaveComputer _computer, TaskListener listener) {
        Computer computer = (Computer)_computer;
        PrintStream logger = listener.getLogger();

        Date startDate = new Date();
        logger.println("Start time: " + getUtcDateString(startDate));

        final Connection ssh = computer.getContainer().connect();

        if (ssh == null) {
            removeNode(computer);
        } else {
            if (!installAgent(computer, ssh, logger)) {
                ssh.close();
                removeNode(computer);
            }
        }

        Date endDate = new Date();
        logger.println("Done setting up at: " + getUtcDateString(endDate));
        logger.println("Done in " + TimeUnit2.MILLISECONDS.toSeconds(endDate.getTime() - startDate.getTime()) + " seconds");
    }

    private static boolean installAgent(Computer computer, final Connection ssh, PrintStream logger) {
        try {
            final SCPClient scp = ssh.createSCPClient();

            logger.println("Copying slave.jar");
            scp.put(Jenkins.getInstance().getJnlpJars("slave.jar").readFully(), "slave.jar", "/tmp");
            String launchString = "java -jar /tmp/slave.jar";
            logger.println("Launching slave agent: " + launchString);
            final Session sess = ssh.openSession();
            sess.execCommand(launchString);
            computer.setChannel(sess.getStdout(), sess.getStdin(), logger, new Channel.Listener() {
                @Override
                public void onClosed(Channel channel, IOException cause) {
                    sess.close();
                    ssh.close();
                }
            });
        } catch (Exception e) {
            // TODO: write exception message into system log instead of UI log
            return false;
        }

        return true;
    }

    private static void removeNode(Computer computer) {
        try {
            Jenkins.getInstance().removeNode(computer.getNode());
        } catch (Exception e) {
            // TODO: write exception message into system log instead of UI log
        }
    }

    private static String getUtcDateString(Date date) {
        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(new Date());
    }
}
