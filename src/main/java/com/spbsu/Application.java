package com.spbsu;

import com.spbsu.ntp.NTPClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

@Slf4j
public class Application {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: com.spbsu.ntp.NTPClient <hostname-or-address-list>");
            System.exit(1);
        }

        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(10000);
        try {
            client.open();
            for (String arg : args) {
                try {
                    InetAddress hostAddress = InetAddress.getByName(arg);
                    System.out.println("> " + hostAddress.getHostName() + "/" + hostAddress.getHostAddress());
                    TimeInfo info = client.getTime(hostAddress);

                    NTPClient.processResponse(info);
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }
        } catch (SocketException e) {
            log.error(e.getLocalizedMessage(), e);
        } finally {
            client.close();
        }
    }

}
