package com.sk89q.spilledgrounds.agent;

import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

public final class ClassDumpAgent {

    private static final Logger log = Logger.getLogger(ClassDumpAgent.class.getName());

    private ClassDumpAgent() {
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        ClassDump provider = new ClassDump(inst);
        ObjectName objectName = new ObjectName("spilledgrounds:type=ClassDumper");
        ManagementFactory.getPlatformMBeanServer().registerMBean(provider, objectName);
        log.info("Installed SpilledBeans class file dumper into the platform MBeanServer as " + objectName.getCanonicalName());
        System.setProperty("com.sk89q.spilledgrounds.agent.installed", "true");
    }


}
