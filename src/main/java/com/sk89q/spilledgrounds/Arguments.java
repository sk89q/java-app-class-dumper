package com.sk89q.spilledgrounds;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Arguments {

    @Parameter(names = {"-h", "-?", "--help"}, help = true, description = "show help")
    public boolean help = false;

    @Parameter(names = {"-v", "--verbose"}, description = "show exceptions")
    public boolean verbose = false;

    @Parameter(names = {"-i", "--interactive"}, description = "accept interactive input of class names to dump")
    public boolean interactive;

    @Parameter(names = {"-p", "--pid"}, description = "PID of VM to connect to")
    public String pid;

    @Parameter(names = "--dir", description = "directory to dump classes to")
    public File dir = new File("dumps");

    @Parameter(description = "list of class names to dump")
    public List<String> patterns = new ArrayList<String>();

}
