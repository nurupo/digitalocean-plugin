package com.dubture.jenkins.digitalocean;

import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.Key;
import com.myjeeva.digitalocean.pojo.Network;
import com.myjeeva.digitalocean.pojo.Region;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import hudson.Util;
import hudson.model.Label;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.newArrayList;

public class Droplet {
    private final String name; // regex
    private final Cloud cloud; // for api keys and such
    private final DropletTemplate template; // for all of the info and container templates
    private List<Container> activeContainers;

    public enum State {
        NotCreated,
        Creating,
        Created,
        Initialized,
        Destroying,
        Destroyed,
        Failed
    }

    private State state;

    private int dropletId;

    String ipv4;

    private static final Logger LOGGER = Logger.getLogger(Droplet.class.getName());

    public Droplet(Cloud cloud, DropletTemplate dropletTemplate) {
        this.name = Name.generateDropletName(cloud.getDisplayName(), dropletTemplate.getName());
        this.cloud = cloud;
        this.template = dropletTemplate;
        this.state = State.NotCreated;

        this.activeContainers = Collections.<Container>emptyList();
    }

    /**
     * Asks Digital Ocean to create a Droplet using the information provided in `template`.
     *
     * State transition:
     *     Success:
     *         NotCreated --> Creating
     *     Failure:
     *         NotCreated --> Failure
     *
     * Sets `dropletId` on success.
     */
    private void create() {
        if (state != State.NotCreated) {
            return;
        }

        com.myjeeva.digitalocean.pojo.Droplet droplet = new com.myjeeva.digitalocean.pojo.Droplet();
        droplet.setName(getName());
        droplet.setSize(template.getSizeId());
        droplet.setRegion(new Region(template.getRegionId()));
        droplet.setImage(DigitalOcean.newImage(template.getImageId()));
        droplet.setKeys(newArrayList(new Key(Integer.parseInt(template.getSshKeyId()))));

        if (!(template.getUserData() == null || template.getUserData().trim().isEmpty())) {
            droplet.setUserData(template.getUserData());
        }

        try {
            DigitalOceanClient apiClient = new DigitalOceanClient(cloud.getAuthToken());
            com.myjeeva.digitalocean.pojo.Droplet createdDroplet = apiClient.createDroplet(droplet);

            dropletId = createdDroplet.getId();
            state = State.Creating;
        } catch (Exception e) {
            state = State.Failed;
            LOGGER.log(Level.SEVERE, "Couldn't create Droplet");
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Waits until the Droplet is fully created by Digital Ocean.
     *
     * @param timeoutMillis How long to wait for the Droplet to be created.
     * @param sleepMillis How long to sleep before checking the state of the Droplet again.
     *
     * State transition:
     *     Success:
     *         Creating --> Created
     *     Failure:
     *         Creating --> Failure
     *
     * Sets `ipv4` on success.
     */
    private void waitUntilCreated(long timeoutMillis, long sleepMillis) {
        if (state != State.Creating) {
            return;
        }

        final long startTimeMillis = System.currentTimeMillis();

        statusCheckLoop:
        while (System.currentTimeMillis() - startTimeMillis < timeoutMillis) {

            com.myjeeva.digitalocean.pojo.Droplet dropletInfo = null;
            try {
                dropletInfo = new DigitalOceanClient(cloud.getAuthToken()).getDropletInfo(dropletId);
            } catch (Exception e) {
                state = State.Failed;
                // TODO: log e
            }

            switch (dropletInfo.getStatus()) {
                case NEW:
                    // still being created
                    break;

                case ACTIVE:
                    for (final Network network : dropletInfo.getNetworks().getVersion4Networks()) {
                        String host = network.getIpAddress();
                        if (host != null) {
                            ipv4 = host;
                        }
                    }
                    if (ipv4 != null) {
                        state = State.Created;
                        break statusCheckLoop;
                    } else {
                        state = State.Failed;
                        // TODO: log
                        //throw new IllegalStateException("Droplet has unexpected ip address: " + ipv4);
                        return;
                    }

                default:
                    state = State.Failed;
                    // TODO: log
                    //throw new IllegalStateException("Droplet has unexpected status: " + dropletInfo.getStatus());
                    return;
            }

            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        state = State.Failed;
    }

    /**
     * Creates an authenticated ssh connection.
     *
     * @param connectionTries How many times try to connect via ssh.
     *
     * @return read yto use ssh connection on success, null on failure.
     */
    private Connection connect(int connectionTries) {
        Connection ssh = new Connection(ipv4, template.getSshPort());
        boolean connected = false;
        for (int i = 0; i < connectionTries; i ++) {
            try {
                ssh.connect(null, 10 * 1000, 10 * 1000);
                connected = true;
                break;
            } catch (Exception e) {
                // ignore
            }
        }

        if (!connected) {
            return null;
        }

        try {
            ssh.authenticateWithPublicKey(template.getUsername(), template.getSshPrivateKey().toCharArray(), "");
            return ssh;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Runs the init script (if any) specified by the user in `template`.
     *
     * State transition:
     *     Success:
     *         Created --> Initialized
     *     Failure:
     *         Created --> Failure
     *
     * Sets `ipv4` on success.
     */
    private void runInitScript(final PrintStream logger) {
        if (state != State.Created) {
            return;
        }

        String initScript = Util.fixEmptyAndTrim(template.getInitScript());

        if (initScript == null) {
            state = State.Initialized;
            return;
        }

        Connection ssh = connect(5);
        if (ssh == null) {
            state = State.Failed;
            return;
        }

        try {
            if (ssh.exec("test -e ~/.hudson-run-init", logger) == 0) {
                ssh.close();
                state = State.Initialized;
                return;
            }

            logger.println("Executing init script");
            SCPClient scp = ssh.createSCPClient();
            scp.put(template.getInitScript().getBytes("UTF-8"), "init.sh", "/tmp", "0700");
            Session session = ssh.openSession();
            session.requestDumbPTY(); // so that the remote side bundles stdout and stderr
            session.execCommand("/tmp/init.sh");

            session.getStdin().close();    // nothing to write here
            session.getStderr().close();   // we are not supposed to get anything from stderr
            IOUtils.copy(session.getStdout(), logger);

            int condition = session.waitForCondition(ChannelCondition.EXIT_STATUS, 10 * 60 * 1000);
            int exitStatus;
            if ((condition & ChannelCondition.EXIT_STATUS) == 0) {
                logger.println("Connection was terminated before the init script could finish");
                session.close();
                ssh.close();
                state = State.Failed;
                return;
            } else if ((exitStatus = session.getExitStatus()) != 0) {
                logger.println("init script failed: exit code=" + exitStatus);
                session.close();
                ssh.close();
                state = State.Failed;
                return;
            }
            session.close();

            ssh.exec("touch ~/.hudson-run-init", logger);
            ssh.close();
            state = State.Initialized;
            return;
        } catch (Exception e) {
            ssh.close();
            state = State.Failed;
            return;
        }
    }

    public Container provisionContainer(Label label) {
        if (state == State.NotCreated) {
            create();
        }
        if (state == State.Creating) {
            waitUntilCreated(5*60*1000, 10*1000);
        }
        if (state == State.Created) {
            runInitScript(logger);
        }
        if (state != State.Initialized) {
            return null; // trow exception?
        }

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
    }

    // TODO: rename to countInstancesOfContainer or something
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

    public List<Container> provisionContainers(Label label, int neededContainers) {
        List<Container> containers = new ArrayList<Container>();

        // check if instance cap of containers this Droplet can have running is reached
        if (template.getContainerInstanceCap() != 0 && template.getContainerInstanceCap() <= activeContainers.size()) {
            return containers;
        }

        for (ContainerTemplate ct : template.getContainerTemplates()) {
            if (!ct.hasLabel(label)) {
                continue;
            }

            // create only min(neededContainers, Droplet's container instance cap - existing containers in droplet, Container's instance cap - existing instance of this particular container in droplet) containers
            int maxAllowedContainersForDropletTemplate = Math.min(template.getContainerInstanceCap() == 0 ? Integer.MAX_VALUE : template.getContainerInstanceCap() - activeContainers.size(), neededContainers);
            int maxAllowedContainersForContainerTemplate = Math.min(ct.getInstanceCap() == 0 ? Integer.MAX_VALUE : ct.getInstanceCap() - countActiveContainers(ct), maxAllowedContainersForDropletTemplate);

            int containerCount;
            for (containerCount = 0; containerCount < maxAllowedContainersForContainerTemplate; containerCount ++) {
                containers.add(new Container(this, ct));
            }

            activeContainers.addAll(containers);

            neededContainers -= containerCount;

            if (neededContainers == 0) {
                break;
            }
        }

        return containers;
    }

    public String getName() {
        return name;
    }

    public DropletTemplate getTemplate() {
        return template;
    }

}
