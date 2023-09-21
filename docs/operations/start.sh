#!/bin/sh

cd $R3Z_HOME

# the following presumes there is a keystore in the same directory as r3z.jar.
# Check "convert_lets_encrypt_certs_to_keystore.txt" in docs/operations for a
# bit more info on this

# Using the Xss180k means that each thread uses 180 kb of memory.  This is just to
# minimize memory use where we can

# run the timekeeping application
$JAVA_HOME/bin/java -Djavax.net.ssl.keyStore=/home/opc/inmra.com.keystore -Djavax.net.ssl.keyStorePassword=changeit -jar r3z.jar -h inmra.com &

# get the process id, pop it in a file (we'll use this to stop the process later)
echo $! > pid
