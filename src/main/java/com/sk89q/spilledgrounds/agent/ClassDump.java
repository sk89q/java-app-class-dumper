package com.sk89q.spilledgrounds.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ClassDump implements ClassDumpMBean {

    private static final Logger log = Logger.getLogger(ClassDump.class.getName());
    private final Instrumentation instrumentation;

    public ClassDump(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public void dumpClass(String patternString, String dirName) {
        Pattern pattern = Pattern.compile(patternString);
        File dir = new File(dirName);
        List<Class<?>> classes = new ArrayList<>();

        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (pattern.matcher(clazz.getName()).matches()) {
                classes.add(clazz);
            }
        }

        if (!classes.isEmpty()) {
            transform(pattern, classes, dir);
        } else {
            log.log(Level.WARNING, "(SpilledBeans) Failed to match any classes against " + patternString);
        }
    }

    private void transform(Pattern pattern, List<Class<?>> classes, File dir) {
        ClassDumpTransformer transformer = new ClassDumpTransformer(pattern, dir);
        instrumentation.addTransformer(transformer, true);
        try {
            for (Class<?> clazz : classes) {
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (UnmodifiableClassException e) {
                    log.log(Level.WARNING, "Unexpected error", e);
                }
            }
        } finally {
            instrumentation.removeTransformer(transformer);
        }
    }

}
