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

import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.*;
import hudson.Extension;
import hudson.RelativePath;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A {@link DropletTemplate} represents the configuration values for creating a new slave via a DigitalOcean droplet.
 *
 * <p>Holds things like Image ID, sizeId and region used for the specific droplet.
 *
 * <p>The {@link DropletTemplate#provision(String, String, String)} method
 * is the main entry point to create a new droplet via the DigitalOcean API when a new slave needs to be provisioned.
 *
 * @author robert.gruendler@dubture.com
 */
@SuppressWarnings("unused")
public class DropletTemplate implements Describable<DropletTemplate> {

    private final String name;

    /**
     * The Image to be used for the droplet.
     */
    private final String imageId;

    /**
     * The specified droplet sizeId.
     */
    private final String sizeId;

    /**
     * The region for the droplet.
     */
    private final String regionId;

    private final String username;

    /**
     * The SSH key to be added to the new droplet.
     */
    private final String sshKeyId;

    /**
     * The SSH private key associated with the selected SSH key
     */
    private final String privateKey;

    private final int sshPort;

    //private final String labelString;

    private final int idleTerminationInMinutes;

    private final int instanceCap;

    /**
     * User-supplied data for configuring a droplet
     */
    private final String userData;

    /**
     * Setup script for preparing the new slave. Differs from userData in that Jenkins runs this script,
     * as opposed to the DigitalOcean provisioning process.
     */
    private final String initScript;

    private final int dockerStartingSshPort;

    private final int dockerInstanceCap;

    /**
     * The maximum number of executors that this slave will run.
     */
    //private final int numExecutors;

    //private final String labels;

    //private final String workspacePath;

    //private transient Set<LabelAtom> labelSet;

    private static final Logger LOGGER = Logger.getLogger(DropletTemplate.class.getName());

    /**
     * Data is injected from the global Jenkins configuration via jelly.
     * @param imageId an image slug e.g. "debian-8-x64", or an integer e.g. of a backup, such as "12345678"
     * @param sizeId the image size e.g. "512mb" or "1gb"
     * @param regionId the region e.g. "nyc1"
     * @param idleTerminationInMinutes how long to wait before destroying a slave
     * @param userData user data for DigitalOcean to apply when building the slave
     * @param initScript setup script to configure the slave
     */
    @DataBoundConstructor
    public DropletTemplate(String name, String imageId, String sizeId, String regionId, String username, String sshKeyId,
                           String privateKey, int sshPort, int idleTerminationInMinutes, int instanceCap, String userData,
                           String initScript, int dockerStartingSshPort, int dockerInstanceCap) {

        LOGGER.log(Level.INFO, "Creating DropletTemplate with imageId = {0}, sizeId = {1}, regionId = {2}",
                new Object[] { imageId, sizeId, regionId});

        this.name = name;
        this.imageId = imageId;
        this.sizeId = sizeId;
        this.regionId = regionId;
        this.username = username;
        this.sshKeyId = sshKeyId;
        this.privateKey = privateKey;
        this.sshPort = sshPort;
        this.idleTerminationInMinutes = idleTerminationInMinutes;
        this.instanceCap = instanceCap;
        this.userData = userData;
        this.initScript = initScript;
        this.dockerStartingSshPort = dockerStartingSshPort;
        this.dockerInstanceCap = dockerInstanceCap;
    }

    public boolean isInstanceCapReached(String authToken, String cloudName) throws RequestUnsuccessfulException, DigitalOceanException {
        if (instanceCap == 0) {
            return false;
        }
        LOGGER.log(Level.INFO, "slave limit check");

        int count = 0;
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node n : nodes) {
            if (DropletName.isDropletInstanceOfSlave(n.getDisplayName(), cloudName, name)) {
                count++;
            }
        }

        if (count >= instanceCap) {
            return true;
        }

        count = 0;
        List<Droplet> availableDroplets = DigitalOcean.getDroplets(authToken);
        for (Droplet droplet : availableDroplets) {
            if ((droplet.isActive() || droplet.isNew())) {
                if (DropletName.isDropletInstanceOfSlave(droplet.getName(), cloudName, name)) {
                    count++;
                }
            }
        }

        return count >= instanceCap;
    }

    public Slave provision(String dropletName, String cloudName, String authToken)
            throws IOException, RequestUnsuccessfulException, Descriptor.FormException {

        LOGGER.log(Level.INFO, "Provisioning slave...");

        try {
            LOGGER.log(Level.INFO, "Starting to provision digital ocean droplet using image: " + imageId + " region: " + regionId + ", sizeId: " + sizeId);

            if (isInstanceCapReached(authToken, cloudName)) {
                throw new AssertionError();
            }

            // create a new droplet
            Droplet droplet = new Droplet();
            droplet.setName(dropletName);
            droplet.setSize(sizeId);
            droplet.setRegion(new Region(regionId));
            droplet.setImage(DigitalOcean.newImage(imageId));
            droplet.setKeys(newArrayList(new Key(Integer.parseInt(sshKeyId))));

            if (!(userData == null || userData.trim().isEmpty())) {
                droplet.setUserData(userData);
            }

            LOGGER.log(Level.INFO, "Creating slave with new droplet " + dropletName);

            DigitalOceanClient apiClient = new DigitalOceanClient(authToken);
            Droplet createdDroplet = apiClient.createDroplet(droplet);

            return newSlave(cloudName, createdDroplet, privateKey);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw new AssertionError();
        }
    }

    /**
     * Create a new {@link Slave} from the given {@link Droplet}
     * @param droplet the droplet being created
     * @param privateKey the RSA private key being used
     * @return the provisioned {@link Slave}
     * @throws IOException
     * @throws Descriptor.FormException
     */
    private Slave newSlave(String cloudName, Droplet droplet, String privateKey) throws IOException, Descriptor.FormException {
        LOGGER.log(Level.INFO, "Creating new slave...");
        return new Slave(
                cloudName,
                droplet.getName(),
                "Computer running on DigitalOcean with name: " + droplet.getName(),
                droplet.getId(),
                privateKey,
                username,
                workspacePath,
                sshPort,
                numExecutors,
                idleTerminationInMinutes,
                Node.Mode.NORMAL,
                labels,
                new ComputerLauncher(),
                new RetentionStrategy(),
                Collections.<NodeProperty<?>>emptyList(),
                Util.fixNull(initScript),
                ""
        );
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DropletTemplate> {

        @Override
        public String getDisplayName() {
            return null;
        }

        public FormValidation doCheckName(@QueryParameter final String name) {
            return new SpecializedFormValidationAsserter(name)
                    .isSet()
                    .isCondition(
                        new FormValidationAsserter.Condition() {
                            @Override
                            public boolean evaluate() {
                                return DropletName.isValidSlaveName(name);
                            }
                        }, Kind.ERROR, "Must consist of A-Z, a-z, 0-9 and . symbols")
                    .result();
        }

        public FormValidation doCheckUsername(@QueryParameter String username) {
            return new SpecializedFormValidationAsserter(username)
                    .isSet()
                    .result();
        }

        public FormValidation doCheckWorkspacePath(@QueryParameter String workspacePath) {
            return new SpecializedFormValidationAsserter(workspacePath)
                    .isSet()
                    .result();
        }

        public FormValidation doCheckSshPort(@QueryParameter String sshPort) {
            return new SpecializedFormValidationAsserter(sshPort)
                    .isPositiveLong()
                    .result();
        }

        public FormValidation doCheckNumExecutors(@QueryParameter String numExecutors) {
            return new SpecializedFormValidationAsserter(numExecutors)
                    .isPositiveLong()
                    .result();
        }

        public FormValidation doCheckIdleTerminationInMinutes(@QueryParameter String idleTerminationInMinutes) {
            return new SpecializedFormValidationAsserter(idleTerminationInMinutes)
                    .isLong()
                    .result();
        }

        public FormValidation doCheckInstanceCap(@QueryParameter String instanceCap) {
            return new SpecializedFormValidationAsserter(instanceCap)
                    .isNonNegativeLong()
                    .result();
        }

        public FormValidation doCheckSizeId(@RelativePath("..") @QueryParameter String authToken) {
            return Cloud.DescriptorImpl.doCheckAuthToken(authToken);
        }

        public FormValidation doCheckImageId(@RelativePath("..") @QueryParameter String authToken) {
            return Cloud.DescriptorImpl.doCheckAuthToken(authToken);
        }

        public FormValidation doCheckRegionId(@RelativePath("..") @QueryParameter String authToken) {
            FormValidation form = Cloud.DescriptorImpl.doCheckAuthToken(authToken);
            if (form.kind != Kind.OK) {
                return form;
            }

            return FormValidation.warning("If you are creating a droplet off a snapshot, make sure that the snapshot " +
                    "is available in the selected region, otherwise the droplet creation will fail");
        }

        public ListBoxModel doFillSizeIdItems(@RelativePath("..") @QueryParameter String authToken) throws Exception {
            List<Size> availableSizes = DigitalOcean.getAvailableSizes(authToken);
            ListBoxModel model = new ListBoxModel();

            for (Size size : availableSizes) {
                model.add(DigitalOcean.buildSizeLabel(size), size.getSlug());
            }

            return model;
        }

        public ListBoxModel doFillImageIdItems(@RelativePath("..") @QueryParameter String authToken) throws Exception {
            SortedMap<String, Image> availableImages = DigitalOcean.getAvailableImages(authToken);
            ListBoxModel model = new ListBoxModel();

            for (Map.Entry<String, Image> entry : availableImages.entrySet()) {
                final Image image = entry.getValue();

                // For non-snapshots, use the image ID instead of the slug (which isn't available anyway)
                // so that we can build images based upon backups.
                final String value = DigitalOcean.getImageIdentifier(image);

                model.add(entry.getKey(), value);
            }

            return model;
        }

        public ListBoxModel doFillRegionIdItems(@RelativePath("..") @QueryParameter String authToken) throws Exception {
            List<Region> availableSizes = DigitalOcean.getAvailableRegions(authToken);
            ListBoxModel model = new ListBoxModel();

            for (Region region : availableSizes) {
                model.add(region.getName(), region.getSlug());
            }

            return model;
        }

        public FormValidation doCheckPrivateKey(@QueryParameter String privateKey) {
            return new SpecializedFormValidationAsserter(privateKey)
                    .isSet()
                    .isSshPrivateKey()
                    .result();
        }

        public FormValidation doChecklabelString(@QueryParameter String labelString) {
            return new SpecializedFormValidationAsserter(labelString)
                    .isSet()
                    .result();
        }

        public FormValidation doCheckSshKeyId(@RelativePath("..") @QueryParameter String authToken) {
            return Cloud.DescriptorImpl.doCheckAuthToken(authToken);
        }

        public ListBoxModel doFillSshKeyIdItems(@RelativePath("..") @QueryParameter String authToken) throws RequestUnsuccessfulException, DigitalOceanException {
            List<Key> availableSizes = DigitalOcean.getAvailableKeys(authToken);
            ListBoxModel model = new ListBoxModel();

            for (Key image : availableSizes) {
                model.add(image.getName(), image.getId().toString());
            }

            return model;
        }
    }

    @SuppressWarnings("unchecked")
    public Descriptor<DropletTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    public String getName() {
        return name;
    }

    public String getImageId() {
        return imageId;
    }

    public String getSizeId() {
        return sizeId;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getUsername() {
        return username;
    }

    public String getSshKeyId() {
        return sshKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public int getSshPort() {
        return sshPort;
    }

    public int getIdleTerminationInMinutes() {
        return idleTerminationInMinutes;
    }

    public int getInstanceCap() {
        return instanceCap;
    }

    public String getUserData() {
        return userData;
    }

    public String getInitScript() {
        return initScript;
    }

    public int getDockerStartingSshPort() {
        return dockerStartingSshPort;
    }

    public int getDockerInstanceCap() {
        return dockerInstanceCap;
    }

}
