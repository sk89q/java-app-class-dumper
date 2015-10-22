package com.sk89q.spilledgrounds;

import com.beust.jcommander.JCommander;
import com.sun.tools.attach.*;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SpilledGrounds {

    private static final Logger log = Logger.getLogger(SpilledGrounds.class.getName());

    private SpilledGrounds() {
    }

    private static VirtualMachine getVirtualMachine(String providedId) throws FatalException {
        try {
            if (providedId != null) {
                return VirtualMachine.attach(providedId);
            } else {
                List<VirtualMachineDescriptor> descriptors = VirtualMachine.list();
                int index = 1;
                System.out.println("Choose target VM:");
                for (VirtualMachineDescriptor descriptor : descriptors) {
                    System.out.println("[" + (index++) + "] " + descriptor.displayName() + " (pid: " + descriptor.id() + ")");
                }

                Scanner scanner = new Scanner(System.in);
                try {
                    int choice = scanner.nextInt();
                    return VirtualMachine.attach(descriptors.get(choice - 1));
                } catch (IndexOutOfBoundsException | NoSuchElementException e) {
                    throw new FatalException("Unknown choice selected!");
                }
            }
        } catch (AttachNotSupportedException | IOException e) {
            throw new FatalException("Could not attach to Java virtual machine", e);
        }
    }

    private static void loadAgent(VirtualMachine vm) throws FatalException {
        try {
            String agentPath = new File(SpilledGrounds.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
            if (vm.getSystemProperties().getProperty("com.sk89q.spilledgrounds.agent.installed") == null) {
                log.info("Trying to install the Java agent...");
                vm.loadAgent(agentPath);
            } else {
                log.info("Java agent already installed in target process (nothing needs to be done)");
            }
        } catch (AgentInitializationException | IOException | AgentLoadException e) {
            throw new FatalException("Failed to install the SpilledGrounds Java agent into the target process", e);
        } catch (URISyntaxException e) {
            throw new FatalException("Failed to get the path to the agent .jar file that would be installed into the target process", e);
        }
    }

    private static String getJMXConnectionAddress(VirtualMachine vm) throws FatalException {
        String address;
        try {
            address = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
        } catch (IOException e) {
            throw new FatalException("Failed to connect to the target process to get agent properties", e);
        }
        if (address != null) {
            return address;
        } else {
            try {
                String javaHome = vm.getSystemProperties().getProperty("java.home");
                File managementAgentJarFile = new File(javaHome + File.separator + "lib" + File.separator + "management-agent.jar");
                vm.loadAgent(managementAgentJarFile.getAbsolutePath());
                return vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
            } catch (AgentInitializationException | AgentLoadException | IOException e) {
                throw new FatalException("Failed to start the JMX agent in the target process", e);
            }
        }
    }

    private static void dumpClasses(String address, List<String> classNames, File outputDir, boolean interactive) throws FatalException {
        JMXConnector connector = null;
        try {
            ObjectName objectName = new ObjectName("spilledgrounds:type=ClassDumper");
            connector = JMXConnectorFactory.connect(new JMXServiceURL(address));
            MBeanServerConnection server = connector.getMBeanServerConnection();

            for (String className : classNames) {
                dumpClass(server, objectName, className, outputDir);
            }

            if (interactive) {
                Scanner scanner = new Scanner(System.in);
                String line;

                log.info("Interactive mod started. Enter patterns to match classes, line by line:");

                while (true) {
                    line = scanner.nextLine().trim();
                    if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                        break;
                    } else if (!line.isEmpty()) {
                        dumpClass(server, objectName, line, outputDir);
                    }
                }
            }
        } catch (MalformedObjectNameException | MalformedURLException | ReflectionException e) {
            throw new RuntimeException("Unexpected error", e);
        } catch (InstanceNotFoundException e) {
            throw new FatalException("The class dumping MBean was installed into the target process (supposedly) but now it can't be found", e);
        } catch (IOException e) {
            throw new FatalException("Failed to connect to target process", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void dumpClass(MBeanServerConnection server, ObjectName objectName, String className, File outputDir) throws InstanceNotFoundException, IOException, ReflectionException {
        try {
            log.info("Trying to dump " + className + "...");
            server.invoke(objectName, "dumpClass",
                    new Object[] { className, outputDir.getAbsolutePath() },
                    new String[]{ String.class.getName(), String.class.getName() });
        } catch (MBeanException e) {
            log.log(Level.WARNING, "Failed to dump class (an exception was raised by the class dumper)", e);
        }
    }

    public static void main(String[] args) {
        SimpleLogFormatter.configureGlobalLogger();

        Arguments parsed = new Arguments();
        JCommander jCommander = new JCommander(parsed, args);
        jCommander.setProgramName("spilledbeans");
        if (parsed.help) {
            jCommander.usage();
            return;
        }

        try {
            VirtualMachine vm = getVirtualMachine(parsed.pid);
            if (!parsed.interactive && parsed.patterns.isEmpty()) {
                log.info("Interactive mode not enabled and no class patterns to dump provided, so the Java agent will be installed but then this program will quit.");
            }
            loadAgent(vm);
            String jmxAddress = getJMXConnectionAddress(vm);
            dumpClasses(jmxAddress, parsed.patterns, parsed.dir, parsed.interactive);
        } catch (FatalException e) {
            if (e.getCause() != null) {
                if (parsed.verbose) {
                    log.log(Level.SEVERE, "A fatal error occurred", e);
                } else {
                    log.severe("ERROR: " + e.getMessage());
                    System.err.println("Use --verbose to see error details");
                }
            } else {
                log.severe("ERROR: " + e.getMessage());
            }
        }
    }

}
