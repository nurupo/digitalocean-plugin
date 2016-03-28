package com.dubture.jenkins.digitalocean;

/**
 * Created by nurupo on 2/28/16.
 */
public class Container {
    private final String name; // regex

    private ContainerTemplate template;
    private Droplet droplet;

    public Container(Droplet droplet, ContainerTemplate containerTemplate) {
        this.name = Name.generateContainerName(cloud.getDisplayName(), dropletTemplate.getName(), containerTemplate.getName());
        this.template = containerTemplate;
        this.droplet = droplet;
    }

    public String getName() {
        return name;
    }

    public ContainerTemplate getTemplate() {
        return template;
    }
}
