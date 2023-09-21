Systemd Install Procedure
=========================

On a cloud environment utilizing SystemD, here is the procedure to install the service file
so that the application starts up on system boot.

The `r3z.service` file should be put in `/etc/systemd/system/`

change its mode: 

    sudo chmod 644 /etc/systemd/system/r3z.service

To reload systemd with this new service unit file, run:

    systemctl daemon-reload

Finally to start the script on boot, enable the service with systemd:

    systemctl enable r3z.service

After reboot, the system should be running.