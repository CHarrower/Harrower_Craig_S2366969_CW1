package com.example.harrower_craig_s2366969.model;

//
// Name                 Craig Harrower
// Student ID           S2366969
// Programme of Study   BSc Software Development
//

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

// Model (lecture 7c MVVM) - gets and processes the traffic data, no screen code in here
public class TrafficModel
{
    private final String clientId;
    private final String clientKey;
    private final File saveFolder; // app's own folder on the phone for the saved feeds (lecture 8a)

    public TrafficModel(String clientId, String clientKey, File saveFolder){
        this.clientId = clientId;
        this.clientKey = clientKey;
        this.saveFolder = saveFolder;
    }

    // what the ViewModel calls - download one feed and parse it
    // returns null if the download failed, so the ViewModel can keep the last data
    public ArrayList<Situation> getSituations(String feedUrl, String fileName){
        String xml = downloadFeed(feedUrl);
        if (xml.isEmpty()) {
            return null;
        }
        saveFeed(xml, fileName); // keep a copy in case there's no connection next time
        //YOUR PARSING HERE!!!
        return parseData(xml);   // parse the downloaded string with the PullParser
    }

    // write the downloaded XML to a file in the app's own storage (lecture 8a slide 4)
    private void saveFeed(String xml, String fileName){
        try {
            File f = new File(saveFolder, fileName);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(xml.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("SaveFeed", "couldn't save " + fileName + ": " + e); // not the end of the world, just no backup this time
        }
    }

    // no connection - read the last saved copy back in and parse it (lecture 8a slide 4)
    // returns null if nothing's been saved yet (e.g. first ever run with no internet)
    public ArrayList<Situation> getSavedSituations(String fileName){
        File f = new File(saveFolder, fileName);
        if (!f.exists()) {
            return null;
        }
        String xml = "";
        String inputLine;
        try {
            // same reading loop as the download, just from the file instead of the internet
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            while ((inputLine = in.readLine()) != null){
                xml = xml + inputLine;
            }
            in.close();
        } catch (IOException e) {
            Log.e("SavedFeed", "couldn't read " + fileName + ": " + e);
            return null;
        }
        if (xml.isEmpty()) {
            return null;
        }
        return parseData(xml); // same PullParser as the live data
    }

    // downloads one feed and returns the XML, or "" if it fails
    // (skeleton download code moved into a method so I can use it for both feeds)
    private String downloadFeed(String feedUrl){
        String xml = "";
        BufferedReader in;
        String inputLine;
        try {
            URL url = new URL(feedUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            //ADD AUTHORISATION (string with clientIID:clientKey)
            urlConnection.setRequestProperty("Authorization",Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
            try {
                // 200 = ok, 401/403 = bad keys, 404 = wrong address
                Log.d("MyTask", "HTTP response code: " + urlConnection.getResponseCode() + " for " + feedUrl);
                in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                while ((inputLine = in.readLine()) != null){
                    xml = xml + inputLine;
                }
                in.close();
            } catch (IOException e) {
                // getErrorStream() can be null and crashed the app, so just log it
                Log.e("urlConnection", "ioexception " + e);
            }
            finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            Log.e("URL", "ioexception " + e);
        }
        //Clean up any leading garbage characters
        int i = xml.indexOf("<?"); //initial tag
        if (i < 0) {
            return "";   // nothing usable came back
        }
        return xml.substring(i);
    }

    private ArrayList<Situation> parseData(String dataToParse)
    {
        ArrayList<Situation> situations = new ArrayList<>();
        Situation aSituation = null;
        boolean insideASituation = false;       // inside <situationRecord>
        boolean insideComment = false;          // inside <generalPublicComment>
        boolean insideFrom = false;             // inside <from>
        boolean insideTo = false;               // inside <to>
        // chunk 1 - set up the parser on the downloaded string and loop through it (same as Lab 3)
        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(dataToParse));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                // START_TAG handling (chunk 2)
                // new tag opened - check which one it is
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("situationRecord")){
                        insideASituation = true;
                        aSituation = new Situation();
                        // attribute 0 is xsi:type
                        aSituation.setRecordType(xpp.getAttributeValue(0));
                        Log.d("Parsing", "New situation record: " + aSituation.getRecordType());
                    }
                    else if (insideASituation) {
                        if (tag.equalsIgnoreCase("overallStartTime")){
                            aSituation.setStartTime(xpp.nextText());
                        } else if (tag.equalsIgnoreCase("overallEndTime")){
                            aSituation.setEndTime(xpp.nextText());
                        } else if (tag.equalsIgnoreCase("delaysType")){
                            aSituation.setDelaysType(xpp.nextText());
                        } else if (tag.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType") || tag.equalsIgnoreCase("abnormalTrafficType") || tag.equalsIgnoreCase("roadMaintenanceType") || tag.equalsIgnoreCase("obstructionType")){
                            aSituation.setSubType(xpp.nextText());
                        } else if (tag.equalsIgnoreCase("numberOfOperationalLanes")){   // chunk 2b - lanes, comment and from/to coords
                            aSituation.setOperationalLanes(toInt(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("numberOfLanesRestricted")){
                            aSituation.setLanesRestricted(toInt(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("generalPublicComment")){
                            insideComment = true;
                        } else if (tag.equalsIgnoreCase("value") && insideComment){
                            aSituation.setComment(xpp.nextText());
                        } else if (tag.equalsIgnoreCase("from")){
                            insideFrom = true;
                        } else if (tag.equalsIgnoreCase("to")){
                            insideTo = true;
                        } else if (tag.equalsIgnoreCase("latitude")){
                            double lat = toDouble(xpp.nextText());
                            if (insideFrom) aSituation.setFromLat(lat);
                            else if (insideTo) aSituation.setToLat(lat);
                        } else if (tag.equalsIgnoreCase("longitude")){
                            double lon = toDouble(xpp.nextText());
                            if (insideFrom) aSituation.setFromLon(lon);
                            else if (insideTo) aSituation.setToLon(lon);
                        }
                        // anything else just gets ignored
                    }
                }
                // END_TAG handling (chunk 3)
                // tag closed - add the finished record or turn the flag back off
                else if (eventType == XmlPullParser.END_TAG) {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("situationRecord")){
                        // break the comment text down too - road and direction (lab 2 string methods)
                        aSituation.setRoad(findRoad(aSituation.getComment()));
                        aSituation.setDirection(findDirection(aSituation.getComment()));
                        situations.add(aSituation);     // finished this one, add it to the list
                        insideASituation = false;
                        Log.d("Parsing", "Situation added: " + aSituation);
                    } else if (tag.equalsIgnoreCase("generalPublicComment")){
                        insideComment = false;
                    } else if (tag.equalsIgnoreCase("from")){
                        insideFrom = false;
                    } else if (tag.equalsIgnoreCase("to")){
                        insideTo = false;
                    }
                }
                eventType = xpp.next(); // need this or it loops forever
            }
        }
        catch (XmlPullParserException e)
        {
            Log.e("Parsing", "Parsing error " + e);
        }
        catch (IOException e)
        {
            Log.e("Parsing", "IO error during parsing");
        }
        Log.d("Parsing", "End of document: " + situations.size() + " situations");
        return situations;
    }

    // find the road in the comment, e.g. "M8 WB Woodside - A804..." -> "M8", "The A78 is closed..." -> "A78"
    // a road is a word starting with A, M or B followed by a number
    private String findRoad(String comment){
        if (comment == null) return "";
        String[] words = comment.split(" ");
        for (String w : words) {
            String word = w.replace(",", "").replace(".", "").replace(":", "").trim().toUpperCase();
            if (word.length() >= 2) {
                char first = word.charAt(0);
                if ((first == 'A' || first == 'M' || first == 'B') && Character.isDigit(word.charAt(1))) {
                    return word; // first road mentioned is the main one
                }
            }
        }
        return "";
    }

    // find the direction in the comment - roadworks use NB/SB/EB/WB, incidents spell it out
    private String findDirection(String comment){
        if (comment == null) return "";
        String text = comment.toUpperCase();
        if (text.contains("BOTH DIRECTIONS")) return "Both directions";
        String[] words = text.split(" ");
        for (String w : words) {
            String word = w.replace(",", "").replace(".", "").replace(")", "").replace("(", "").trim();
            if (word.equals("NB") || word.equals("NORTHBOUND")) return "Northbound";
            if (word.equals("SB") || word.equals("SOUTHBOUND")) return "Southbound";
            if (word.equals("EB") || word.equals("EASTBOUND")) return "Eastbound";
            if (word.equals("WB") || word.equals("WESTBOUND")) return "Westbound";
        }
        return "";
    }

    // text to number - returns 0 if the feed sends something weird instead of crashing (try/catch like week 2)
    private int toInt(String text){
        try { return Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private double toDouble(String text){
        try { return Double.parseDouble(text.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
