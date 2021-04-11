package main;

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
    private static String lastLogoutTime;
    private static ConfigData config;

    DateTimeCheck() {
        config = ConfigData.getInstance();
    }

    /**
     * @return flag returns true if date time is correct or false if incorrect.
     */
    boolean checkDateTime() {
        System.out.println("DATE AND TIME CHECK OF YOUR SYSTEM IS UNDER PROGRESS");
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
                    System.out.println("Network Connection not available and Last login details not available in config file");
                    return false;
                }
                System.out.println("last logout  Date & Time is :   " + serverDateTime);
            } else {
                serverDateTime = simpleDateFormat.parse(serverDate);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        if (simpleDateFormat1.format(serverDateTime).compareTo(getCurrentDateTime()) <= 0) {
            System.out.println("Server Date & Time is :   " + simpleDateFormat1.format(serverDateTime));
            System.out.println("Time is correct");
            setLastLogoutTime();
            flag = true;
        } else {
            System.out.println("Server Date & Time is :   " + simpleDateFormat1.format(serverDateTime));
            System.out.println("Time is Incorrect");
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
            System.out.println("The error is " + ex);
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
