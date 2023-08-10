##
# Project name - used to set the jar's file name
##
PROJ_NAME := r3z

HOST_NAME := inmra.com

HOST_USER := opc

WEB_ENGINE_JAR := r3z.jar

MINUM_LIB := lib/$(WEB_ENGINE_JAR)

VERSION=1.0.0

OUT_DIR=build

##
# default target(s)
##
all:: help

#: clean up any output files
clean::
	    ./gradlew clean

#: jar up the application (See Java's jar command)
jar::
	 ./gradlew jar

# This command is necessary for sending the data up to the cloud for production
#: put the jar and all scripts necessary for remote running into a tar file
bundle:: jar
	    mkdir -p $(OUT_DIR)/$(PROJ_NAME)
	    cp docs/operations/* $(OUT_DIR)/$(PROJ_NAME)
	    cp $(OUT_DIR)/libs/$(PROJ_NAME).jar $(OUT_DIR)/$(PROJ_NAME)
	    git log --oneline|head -1 > $(OUT_DIR)/$(PROJ_NAME)/code_status.txt && git diff >> $(OUT_DIR)/$(PROJ_NAME)/code_status.txt
	    cd $(OUT_DIR)/$(PROJ_NAME) && tar -czf "../r3z.tar.gz" *

#: send to the cloud but don't run.  Depends on tests passing.
deliver:: bundle
	    scp -r $(OUT_DIR)/r3z.tar.gz $(HOST_USER)@$(HOST_NAME):~/

# This command does a lot.  It runs a command on the production server to create a new directory, inmra, which
# won't fail if it does exist, then it un-tars our files into that directory, and then it restarts that system.
#: run the software in the cloud.  Depends on deliver.
deploy:: deliver
	    ssh $(HOST_USER)@$(HOST_NAME) "mkdir -p r3z && tar zxf r3z.tar.gz -C r3z && rm r3z.tar.gz && cd r3z && ./restart.sh"

JMX_PROPERTIES=-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
DEBUG_PROPERTIES=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y

# This command includes several system properties so that we can connect to the
# running Java Virtual Machine with Java Mission Control (JMC)
#: run the application
run:: classes copyresources
	    ./gradlew run

#: run the application using the jar
runjar:: jar
	    $(JAVA) $(JMX_PROPERTIES) -jar $(OUT_DIR)/$(PROJ_NAME).jar

#: run the tests
test:: classes copyresources testclasses
	    ./gradlew test

# a handy debugging tool.  If you want to see the value of any
# variable in this file, run something like this from the
# command line:
#
#     make print-CLS
#
# and you'll get something like: CLS = out/inmra.logging/ILogger.class out/inmra.logging/Logger.class out/inmra.testing/Main.class out/inmra.utils/ActionQueue.class
print-%::
	    @echo $* = $($*)

# This is a handy helper.  This prints a menu of items
# from this file - just put hash+colon over a target and type
# the description of that target.  Run this from the command
# line with "make help"
help::
	 @echo
	 @echo Help
	 @echo ----
	 @echo
	 @grep -B1 -E "^[a-zA-Z0-9_-]+:([^\=]|$$)" Makefile \
     | grep -v -- -- \
     | sed 'N;s/\n/###/' \
     | sed -n 's/^#: \(.*\)###\(.*\):.*/\2###\1/p' \
     | column -t  -s '###'

