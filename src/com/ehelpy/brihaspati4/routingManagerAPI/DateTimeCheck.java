package com.ehelpy.brihaspati4.routingManagerAPI;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * It carries out a date time check of the system with the network server so that the user is not able to forge validity of the certificate.
 * The code retrieves a network packet from B4 (Identity Server) or Google server (or any server defined) and extracts timestamp from it as serverDateTime.
 * ServerDateTime is compared with system date and time (LocalDateTime). The system date has to be equal or ahead of server time so as to ensure that user is not able to forge
 * certificate validity.
 */
class DateTimeCheck {
    private static final Logger log = Logger.getLogger(DateTimeCheck.class);
    private static String lastLogoutTime;
    private static ConfigData config;

    DateTimeCheck() {
        config = ConfigData.getInstance();
    }

    /**
     * @return flag returns true if date time is correct or false if incorrect.
     */
    boolean checkDateTime() {
        log.info("Date and Time check of your system is under Process");
        boolean flag = false;
        //String serverUrl = "http://172.20.82.6:8080/b4server";
        String serverUrl = "https://www.google.co.in";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault());
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date serverDateTime = null;
        try {
            String serverDate = getServerHttpDate(serverUrl);
            if (serverDate == null) {
                try {
                    serverDateTime = simpleDateFormat1.parse(getLastLogoutTime());
                } catch (NullPointerException e) {
                    log.warn("Network Connection not available and Last login details not available in config file");
                    return false;
                }
                log.info("Last logout  Date & Time is :   " + serverDateTime);
            } else {
                serverDateTime = simpleDateFormat.parse(serverDate);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        if (simpleDateFormat1.format(serverDateTime).compareTo(getCurrentDateTime()) <= 0) {
            log.info("Server Date & Time is :   " + simpleDateFormat1.format(serverDateTime));
            log.info("System Time is correct");
            setLastLogoutTime();
            flag = true;
        } else {
            log.info("Server Date & Time is :   " + simpleDateFormat1.format(serverDateTime));
            log.info(" System Time is Incorrect,update your data and time and come back");
        }
        return flag;
    }

    private String getServerHttpDate(String serverUrl) throws IOException {
        try {
            URL url = new URL(serverUrl);
            URLConnection connection = url.openConnection();
            Map<String, List<String>> httpHeaders = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
                String headerName = entry.getKey();
                if (headerName != null && headerName.equalsIgnoreCase("date")) {
                    return entry.getValue().get(0);
                }
            }
        } catch (Exception ex) {
            log.info("Exception Occurred",ex);
            return null;
        }
        return null;
    }

     String getCurrentDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date date = new Date();
        return dateFormat.format(date);
    }

     String getLastLogoutTime() {
        lastLogoutTime = config.getValue("LastLogoutTime");
        return lastLogoutTime;
    }

     void setLastLogoutTime() {
        lastLogoutTime = getCurrentDateTime();
        config.addLastLogOutToConfigFile(lastLogoutTime);
    }

}
