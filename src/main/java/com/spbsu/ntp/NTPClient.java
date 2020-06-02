package com.spbsu.ntp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;

@Slf4j
public class NTPClient {

    private static int stratum;
    private static String refType;

    private static int version;
    private static int leapIndicator;
    private static int precision;

    private static String modeName;
    private static int mode;

    private static double rootDelayInMillisDouble;
    private static double rootDispersionInMillisDouble;

    private static int poll;

    private static int referenceId;
    private static String referenceAddress;
    private static String referenceName;

    private static TimeStamp referenceTimeStamp;
    private static TimeStamp originateTimeStamp;
    private static TimeStamp receiveTimeStamp;
    private static TimeStamp transmitTimeStamp;
    private static TimeStamp destinationTimeStamp;

    private static String delay;
    private static String offset;

    private static final NumberFormat numberFormat = new java.text.DecimalFormat("0.00");

    /**
     * Process <code>TimeInfo</code> object and print its details.
     *
     * @param hostAddress <code>InetAddress</code> object.
     */
    public static void processResponse(InetAddress hostAddress) {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(10000);
        try {
            client.open();
            TimeInfo info = client.getTime(hostAddress);
            processResponse(info);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        } finally {
            client.close();
        }

    }

    /**
     * Process <code>TimeInfo</code> object and print its details.
     *
     * @param info <code>TimeInfo</code> object.
     */
    public static void processResponse(TimeInfo info) {
        NtpV3Packet message = info.getMessage();
        stratum = message.getStratum();

        if (stratum <= 0) {
            refType = "(Unspecified or Unavailable)";
        } else if (stratum == 1) {
            refType = "(Primary Reference; e.g., GPS)"; // GPS, radio clock, etc.
        } else {
            refType = "(Secondary Reference; e.g. via NTP or SNTP)";
        }

        version = message.getVersion();
        leapIndicator = message.getLeapIndicator();
        precision = message.getPrecision();

        modeName = message.getModeName();
        mode = message.getMode();

        poll = message.getPoll();

        rootDelayInMillisDouble = message.getRootDelayInMillisDouble();
        rootDispersionInMillisDouble = message.getRootDispersionInMillisDouble();

        referenceId = message.getReferenceId();
        referenceAddress = NtpUtils.getHostAddress(referenceId);
        referenceName = null;
        if (referenceId != 0) {
            if (referenceAddress.equals("127.127.1.0")) {
                referenceName = "LOCAL"; // This is the ref address for the Local Clock
            } else if (stratum >= 2) {
                // If reference id has 127.127 prefix then it uses its own reference clock
                // defined in the form 127.127.clock-type.unit-num (e.g. 127.127.8.0 mode 5
                // for GENERIC DCF77 AM; see refclock.htm from the NTP software distribution.
                if (!referenceAddress.startsWith("127.127")) {
                    try {
                        InetAddress addr = InetAddress.getByName(referenceAddress);
                        String name = addr.getHostName();
                        if (name != null && !name.equals(referenceAddress)) {
                            referenceName = name;
                        }
                    } catch (UnknownHostException e) {
                        // some stratum-2 servers sync to ref clock device but fudge stratum level higher... (e.g. 2)
                        // ref not valid host maybe it's a reference clock name?
                        // otherwise just show the ref IP address.
                        referenceName = NtpUtils.getReferenceClock(message);
                    }
                }
            } else if (version >= 3 && (stratum == 0 || stratum == 1)) {
                referenceName = NtpUtils.getReferenceClock(message);
                // refname usually have at least 3 characters (e.g. GPS, WWV, LCL, etc.)
            }
            // otherwise give up on naming the beast...
        }
        if (referenceName != null && referenceName.length() > 1) {
            referenceAddress += " (" + referenceName + ")";
        }

        referenceTimeStamp = message.getReferenceTimeStamp();

        // Originate Time is time request sent by client (t1)
        originateTimeStamp = message.getOriginateTimeStamp();

        // Receive Time is time request received by server (t2)
        receiveTimeStamp = message.getReceiveTimeStamp();

        // Transmit time is time reply sent by server (t3)
        transmitTimeStamp = message.getTransmitTimeStamp();

        long destinationTime = info.getReturnTime();
        // Destination time is time reply received by client (t4)
        destinationTimeStamp = TimeStamp.getNtpTime(destinationTime);

        info.computeDetails(); // compute offset/delay if not already done
        Long offsetValue = info.getOffset();
        Long delayValue = info.getDelay();
        delay = (delayValue == null) ? "N/A" : delayValue.toString();
        offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

        printResponseResult();
    }

    public static void printResponseResult() {
        log.info("Stratum: {} {}", stratum, refType);
        log.info("LeapIndicator: {}", leapIndicator);
        log.info("Version: {}", version);
        log.info("Precision: {}", precision);
        log.info("Mode: {} ({})", modeName, mode);
        log.info("Poll: {} = (2 ** {}) seconds", (poll <= 0 ? 1 : (int) Math.pow(2, poll)), poll);
        log.info("RootDelay: {} ms", numberFormat.format(rootDelayInMillisDouble));
        log.info("RootDispersion: {} ms", numberFormat.format(rootDispersionInMillisDouble));

        log.info("Reference Identifier: {}", referenceAddress);

        log.info("Reference TimeStamp: {}", referenceTimeStamp.toDateString());
        log.info("Originate TimeStamp: {}", originateTimeStamp.toDateString());
        log.info("Receive TimeStamp: {}", receiveTimeStamp.toDateString());
        log.info("Transmit TimeStamp: {}", transmitTimeStamp.toDateString());
        log.info("Destination TimeStamp: {}", destinationTimeStamp.toDateString());

        log.info("RoundTrip Delay: {} ms", delay);
        log.info("Clock Offset: {} ms", offset); // offset in ms
    }

}
