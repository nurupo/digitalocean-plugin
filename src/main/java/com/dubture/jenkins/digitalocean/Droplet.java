package com.dubture.jenkins.digitalocean;

import hudson.model.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Droplet {
    private final String name; // regex
    private final Cloud cloud; // for api keys and such
    private final DropletTemplate template; // for all of the info and container templates
    private List<Container> activeContainers;

    public Droplet(Cloud cloud, DropletTemplate dropletTemplate) {
        this.name = Name.generateDropletName(cloud.getDisplayName(), dropletTemplate.getName());
        this.cloud = cloud;
        this.template = dropletTemplate;

        this.activeContainers = Collections.<Container>emptyList();
    }

    private int countActiveContainers(ContainerTemplate containerTemplate) {
        int count = 0;

        for (Container c : activeContainers) {
            if (Name.isContainerOfContainerTemplate(c.getName(), cloud.getDisplayName(), template.getName(), containerTemplate.getName())) {
                count ++;
            }
        }

        return count;
    }

    public boolean canProvisionContainer(Label label) {
        // check if instance cap of containers this Droplet can have running is reached
        if (template.getContainerInstanceCap() != 0 && template.getContainerInstanceCap() <= activeContainers.size()) {
            return false;
        }

        // ok, it's not reached yet, we can run a container

        // check if any of Container Templates of the Droplet Template associated with this Droplet actually have the requested label
        // and the instance cap of containers created off this Container Template is not reached

        //List<ContainerTemplate> candidateContrainerTemplates = new ArrayList<ContainerTemplate>();

        for (ContainerTemplate ct : template.getContainerTemplates()) {
            if (!ct.hasLabel(label)) {
                continue;
            }

            if (ct.getInstanceCap() != 0 && ct.getInstanceCap() <= countActiveContainers(ct)) {
                continue;
            }

            return true;
            //candidateContrainerTemplates.add(ct);
        }

        /*if (!candidateContrainerTemplates.isEmpty()) {
            return true;
        }*/

        return false;
    }

    public String getName() {
        return name;
    }

}
