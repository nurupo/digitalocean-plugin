package com.dubture.jenkins.digitalocean;

/**
 * Created by nurupo on 2/28/16.
 */
public class Container {
    private final String name; // regex

    public Container(Cloud cloud, DropletTemplate dropletTemplate, ContainerTemplate containerTemplate) {
        this.name = Name.generateContainerName(cloud.getDisplayName(), dropletTemplate.getName(), containerTemplate.getName());
    }

    public String getName() {
        return name;
    }
}
