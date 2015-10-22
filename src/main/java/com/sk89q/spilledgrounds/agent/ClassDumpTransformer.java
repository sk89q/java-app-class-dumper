package com.sk89q.spilledgrounds.agent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ClassDumpTransformer implements ClassFileTransformer {

    private static final Logger log = Logger.getLogger(ClassDumpTransformer.class.getName());
    private final Pattern pattern;
    private final File outputDir;

    public ClassDumpTransformer(Pattern pattern, File outputDir) {
        this.pattern = pattern;
        this.outputDir = outputDir;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (pattern.matcher(className.replace("/", ".")).matches()) {
            File file = new File(outputDir, className + ".class");
            file.getAbsoluteFile().getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bos.write(classfileBuffer);
                log.info("Wrote class dump to " + file.getAbsolutePath());
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to write to " + file, e);
            }
        }

        return null;
    }

}
