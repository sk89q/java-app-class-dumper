# Spilled Beans

A tool to dump Java .class files from a running JVM instance.

## Compiling

    ./gradlew clean build

## Usage

`$JAVA_HOME` must point to a JDK installation.

    java -Djava.library.path=$JAVA_HOME/jre/bin \
        -cp $JAVA_HOME/lib/tools.jar;spilledgrounds-1.0-SNAPSHOT-all.jar \
        com.sk89q.spilledgrounds.SpilledGrounds \
		--dir dumps com\.example1\..* com\.example2\..*
        
    Options:
    --dir
       directory to dump classes to
       Default: dumps
    -h, -?, --help
       show help
       Default: false
    -i, --interactive
       accept interactive input of class names to dump
       Default: false
    -p, --pid
       PID of VM to connect to
    -v, --verbose
       show exceptions
       Default: false
