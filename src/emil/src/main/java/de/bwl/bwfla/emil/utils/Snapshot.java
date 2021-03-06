package de.bwl.bwfla.emil.utils;

import de.bwl.bwfla.api.imagearchive.ImageArchiveMetadata;
import de.bwl.bwfla.api.imagearchive.ImageType;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emil.DatabaseEnvironmentsAdapter;
import de.bwl.bwfla.emil.datatypes.*;
import de.bwl.bwfla.emil.datatypes.snapshot.SaveDerivateRequest;
import de.bwl.bwfla.emil.datatypes.snapshot.SaveNewEnvironmentRequest;
import de.bwl.bwfla.emil.datatypes.snapshot.SaveObjectEnvironmentRequest;
import de.bwl.bwfla.emil.datatypes.snapshot.SaveUserSessionRequest;
import de.bwl.bwfla.emucomp.api.*;
import de.bwl.bwfla.objectarchive.util.ObjectArchiveHelper;

import javax.xml.bind.JAXBException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Snapshot
{
    protected static final Logger LOG = Logger.getLogger("Image Snapshot");

    private final MachineConfiguration configuration;

    /** Binding's changed/written data */
    private final List<BindingDataHandler> data;


    public Snapshot(MachineConfiguration config)
    {
        // No disks, but we create an empty snapshot to handle floppy objs env
        this(config, null);
    }

    public Snapshot(MachineConfiguration config, List<BindingDataHandler> data) {
        this.configuration = config;
        this.data = data;
    }

    private static void copyBinding(MachineConfiguration src, MachineConfiguration target, String id) {
        if (src == null || src.getAbstractDataResource() == null || target == null)
            return;

        for (AbstractDataResource r : src.getAbstractDataResource()) {
            if (!r.getId().equals(id))
                continue;
            if (target.getAbstractDataResource() == null)
                target.setAbstractDataResource(new ArrayList<AbstractDataResource>());
            target.getAbstractDataResource().add(r);

            String bindingId = r.getId();

            // need to register drive (ideally keep drive index)
            int index = 0;
            String dataValue = null;
            if (src.getDrive() == null)
                continue;

            for (Drive d : src.getDrive()) {
                if (d.getData().contains(bindingId)) {
                    dataValue = d.getData();
                    break;
                }
                index++;
            }
            if (dataValue == null)
                try {
                    LOG.warning("inconsistent metadata " + src.value(true));
                    return;
                } catch (JAXBException e) {
                    LOG.log(Level.WARNING, e.getMessage(), e);
                }
            List<Drive> drives = target.getDrive();
            drives.get(index).setData(dataValue);
        }
    }

    private static void addSoftwareId(MachineConfiguration env, String softwareId)
    {
        List<String> installedSoftwareList = env.getInstalledSoftwareIds();
        Set<String> installedSoftwareSet = new HashSet<String>(installedSoftwareList);
        if (!installedSoftwareSet.contains(softwareId)) {
            installedSoftwareList.add(softwareId);
        }
    }

    public EmilEnvironment createEnvironment(DatabaseEnvironmentsAdapter environmentsAdapter,
                                             SaveDerivateRequest req,
                                             EmilEnvironment parentEnv,
                                             boolean checkpoint) throws BWFLAException {

        // if(dataHandler == null)
        //    throw new BWFLAException("empty snapshots not supported.");

        EmilEnvironment newEnv = null;
        MachineConfiguration machineConfiguration;
        if(!checkpoint)
            machineConfiguration = EmulationEnvironmentHelper.clean(configuration);
        else
            machineConfiguration = configuration.copy();

        ImageArchiveMetadata iaMd = new ImageArchiveMetadata();
        if (parentEnv instanceof EmilSessionEnvironment) {
            iaMd.setType(ImageType.SESSIONS);
            newEnv = new EmilSessionEnvironment((EmilSessionEnvironment)parentEnv);
        } else if (parentEnv instanceof EmilObjectEnvironment) {
            iaMd.setType(ImageType.OBJECT);
            MachineConfiguration oldEnv = (MachineConfiguration) environmentsAdapter.getEnvironmentById(req.getArchive(), req.getEnvId());
            copyBinding(oldEnv, machineConfiguration, ((EmilObjectEnvironment) parentEnv).getObjectId());
            newEnv = new EmilObjectEnvironment((EmilObjectEnvironment) parentEnv);
        } else {
            iaMd.setType(ImageType.DERIVATE);
            newEnv = new EmilEnvironment(parentEnv);
        }

        if (req.getSoftwareId() != null)
            addSoftwareId(machineConfiguration, req.getSoftwareId());

        if(req instanceof SaveNewEnvironmentRequest)
        {
            SaveNewEnvironmentRequest newReq = (SaveNewEnvironmentRequest) req;
            machineConfiguration.getDescription().setTitle(newReq.getTitle());
            newEnv.setTitle(newReq.getTitle());

        }

        if(!checkpoint) {
            for (Iterator<BindingDataHandler> it = data.iterator(); it.hasNext(); ) {
                BindingDataHandler bdh = it.next();
                if (bdh.getId().equals("emucon-rootfs")) {
                    it.remove();
                }
            }
        }

        String newId = environmentsAdapter.importMachineEnvironment("default", machineConfiguration, data, iaMd);
        if(newId == null)
            throw new BWFLAException("create revision: importMachineEnvironment failed");

        if(checkpoint)
            newEnv.setTitle("Snapshot " + parentEnv.getEnvId());

        newEnv.setParentEnvId(parentEnv.getEnvId());
        newEnv.setEnvId(newId);
        newEnv.setDescription(req.getMessage());
        newEnv.setArchive("default");
        return newEnv;
    }

    public String saveUserSession(DatabaseEnvironmentsAdapter environmentsAdapter,
                                  ObjectArchiveHelper objectArchiveHelper,
                                  SaveUserSessionRequest request) throws BWFLAException
    {

        if (data == null)
            throw new BWFLAException("empty snapshots not supported.");

        MachineConfiguration env = EmulationEnvironmentHelper.clean(configuration);
        env.getDescription().setTitle("user session: " + request.getUserId());

        ImageArchiveMetadata iaMd = new ImageArchiveMetadata();
        iaMd.setType(ImageType.SESSIONS);
        iaMd.setUserId(request.getUserId());

        String objectId = request.getObjectId();
//        if(objectId != null)
//        {
//            String archiveName = request.getObjectArchiveId();
//            if (archiveName == null)
//                archiveName = "default";
//
//            ObjectArchiveBinding binding = new ObjectArchiveBinding(environmentsAdapter.toString(), archiveName, request.getObjectId());
//            FileCollection fc = objectArchiveHelper.getObjectReference(request.getObjectArchiveId(), request.getObjectId());
//
//            if (fc == null)
//                throw new BWFLAException("create object environment: object not found: " + request.getObjectId());
//
//            //**?
//            int driveId = EmulationEnvironmentHelper.addArchiveBinding(env, binding, fc);
//            try {
//                System.out.println("ready to import machine: " + env.value());
//            } catch (JAXBException e) {
//                e.printStackTrace();
//            }
//        }

        for (Iterator<BindingDataHandler> it = data.iterator(); it.hasNext();)
        {
            BindingDataHandler bdh = it.next();
            if(bdh.getId().equals("emucon-rootfs")) {
                it.remove();
            }
        }
        String newId = environmentsAdapter.importMachineEnvironment("default", env, data, iaMd);
        if (newId == null)
            throw new BWFLAException("importMachineEnvironment failed");

        return newId;
    }

    public EmilObjectEnvironment createObjectEnvironment(DatabaseEnvironmentsAdapter environmentsAdapter,
                                                         ObjectArchiveHelper objectArchiveHelper,
                                                         SaveObjectEnvironmentRequest request) throws BWFLAException {

        if(request.getTitle() == null)
            throw new BWFLAException("invalid request: title is missing");

        if(request.getObjectId() == null)
            throw new BWFLAException("invalid request: invalid object data");

        MachineConfiguration env = EmulationEnvironmentHelper.clean(configuration);
        env.getDescription().setTitle(request.getTitle());

        ImageArchiveMetadata iaMd = new ImageArchiveMetadata();
        iaMd.setType(ImageType.OBJECT);

        int driveId = request.getDriveId();

        if(request.isEmbeddedObject()) {
            String archiveName = request.getObjectArchiveId();
            if (archiveName == null)
                archiveName = "default";

            ObjectArchiveBinding binding = new ObjectArchiveBinding(environmentsAdapter.toString(), archiveName, request.getObjectId());
            FileCollection fc = objectArchiveHelper.getObjectReference(request.getObjectArchiveId(), request.getObjectId());

            if (fc == null)
                throw new BWFLAException("create object environment: object not found: " + request.getObjectId());

            driveId = EmulationEnvironmentHelper.addArchiveBinding(env, binding, fc);
            try {
                System.out.println("ready to import machine: " + env.value());
            } catch (JAXBException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }

        String newId = null;
        if (data != null) {
            for (Iterator<BindingDataHandler> it = data.iterator(); it.hasNext();)
            {
                BindingDataHandler bdh = it.next();
                if(bdh.getId().equals("emucon-rootfs")) {
                    it.remove();
                }
            }
            newId = environmentsAdapter.importMachineEnvironment(request.getArchive(), env, data, iaMd);
        }
        else
            newId = environmentsAdapter.importMetadata(request.getArchive(), env, iaMd, false);

        if (newId == null)
            throw new BWFLAException("importMachineEnvironment failed");

        EmilObjectEnvironment ee = new EmilObjectEnvironment();
        ee.setParentEnvId(request.getEnvId());
        ee.setEnvId(newId);
        ee.setTitle(request.getTitle());
        ee.setDriveId(driveId);
        ee.setObjectId(request.getObjectId());
        ee.setObjectArchiveId(request.getObjectArchiveId());
        ee.setEmulator(env.getEmulator().getBean());
        ee.setOs(env.getOperatingSystemId());
        ee.setDescription(request.getMessage());
        ee.setArchive("default");

        return ee;
    }
}
