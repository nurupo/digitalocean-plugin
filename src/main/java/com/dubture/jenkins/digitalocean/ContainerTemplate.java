package com.dubture.jenkins.digitalocean;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Set;

public class ContainerTemplate implements Describable<ContainerTemplate> {

    private final String name;
    private final String labels;
    private final String image;
    private final String sshPrivateKey;
    private final int    sshPort;
    private final String username;
    private final String workspacePath;
    private final int    instanceCap;
    private final String initScript;

    private final transient Set<LabelAtom> labelSet;

    @DataBoundConstructor
    public ContainerTemplate(String name, String labels, String image, String sshPrivateKey, int sshPort,
                             String username, String workspacePath, int instanceCap, String initScript) {
        this.name          = name;
        this.labels        = labels;
        this.image         = image;
        this.sshPrivateKey = sshPrivateKey;
        this.sshPort       = sshPort;
        this.username      = username;
        this.workspacePath = workspacePath;
        this.instanceCap   = instanceCap;
        this.initScript    = initScript;

        labelSet = Label.parse(labels);
    }

    @Override
    public Descriptor<ContainerTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    public String getName() {
        return name;
    }

    public String getLabels() {
        return labels;
    }

    public String getImage() {
        return image;
    }

    public String getSshPrivateKey() {
        return sshPrivateKey;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getUsername() {
        return username;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public int getInstanceCap() {
        return instanceCap;
    }

    public String getInitScript() {
        return initScript;
    }


    @Extension
    public static final class DescriptorImpl extends Descriptor<ContainerTemplate> {

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
                            }, FormValidation.Kind.ERROR, "Must consist of A-Z, a-z, 0-9 and . symbols")
                    .result();
        }

        public FormValidation doCheckLabels(@QueryParameter String labels) {
            return new SpecializedFormValidationAsserter(labels)
                    .isSet()
                    .result();
        }

        public FormValidation doCheckImage(@QueryParameter String image) {
            return new SpecializedFormValidationAsserter(image)
                    .isSet()
                    .result();
        }

        public FormValidation doCheckSshPrivateKey(@QueryParameter String sshPrivateKey) {
            return new SpecializedFormValidationAsserter(sshPrivateKey)
                    .isSet()
                    .isSshPrivateKey()
                    .result();
        }

        public FormValidation doCheckSshPort(@QueryParameter String sshPort) {
            return new SpecializedFormValidationAsserter(sshPort)
                    .isPositiveLong()
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

        public FormValidation doCheckInstanceCap(@QueryParameter String instanceCap) {
            return new SpecializedFormValidationAsserter(instanceCap)
                    .isNonNegativeLong()
                    .result();
        }
    }
}
