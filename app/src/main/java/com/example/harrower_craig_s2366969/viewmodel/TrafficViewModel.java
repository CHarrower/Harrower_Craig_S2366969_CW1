package com.example.harrower_craig_s2366969.viewmodel;

//
// Name                 Craig Harrower
// Student ID           S2366969
// Programme of Study   BSc Software Development
//

import com.example.harrower_craig_s2366969.model.Situation;
import com.example.harrower_craig_s2366969.model.TrafficModel;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// ViewModel (lecture 7c MVVM) - holds the data for the screens and survives rotation
// runs the download on a worker thread and gets the results back with a handler (lecture 5a)
public class TrafficViewModel extends ViewModel
{
    // Traffic Scotland feeds
    private final static String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    // roadworks feed - same keys and same XML layout as the unplanned events one
    private final static String roadworksURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";
    // files the last good download of each feed gets saved to (lecture 8a)
    private final static String INCIDENTS_FILE = "incidents.xml";
    private final static String ROADWORKS_FILE = "roadworks.xml";

    // how often to re-download (1 hour)
    private final static long UPDATE_INTERVAL = 60 * 60 * 1000; // 1 hour (use 60 * 1000 = 1 min when testing)

    // message types for the handler
    private final static int MESSAGE_INCIDENTS = 1;
    private final static int MESSAGE_ROADWORKS = 2;
    private final static int MESSAGE_NO_DATA = 3;
    private final static int MESSAGE_DONE = 4;     // download finished (worked or not) - hide the spinner

    // which tab is showing, so MainActivity can highlight it (and keep it after a rotation)
    public final static int TAB_INCIDENTS = 0;
    public final static int TAB_ROADWORKS = 1;
    public final static int TAB_MAP = 2;
    public final static int TAB_SEARCH = 3;

    // the data the screens observe (lecture 7c / observableViewModel example)
    private final MutableLiveData<ArrayList<Situation>> incidents = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<Situation>> roadworks = new MutableLiveData<>();
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final MutableLiveData<String> connectionError = new MutableLiveData<>();
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();   // the situation tapped in a list
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);   // true while downloading
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(TAB_INCIDENTS);
    private boolean usingSavedData = false; // true = showing the saved copy because there's no connection
    private final MutableLiveData<ArrayList<Situation>> searchResults = new MutableLiveData<>(); // road search results
    private String lastSearch = "";
    private boolean mapResultsOnly = false; // true = map shows just the search results
    private final MutableLiveData<Integer> mapType = new MutableLiveData<>(1); // 1 = normal map (GoogleMap.MAP_TYPE_NORMAL)

    private TrafficModel model = null;
    private Handler updateUIHandler = null;

    public TrafficViewModel(){
        status.setValue(""); // nothing until the first download finishes - the spinner shows it's loading
    }

    // what the activity / fragments observe
    public LiveData<ArrayList<Situation>> getIncidents(){ return incidents; }
    public LiveData<ArrayList<Situation>> getRoadworks(){ return roadworks; }
    public LiveData<String> getStatus(){ return status; }
    public LiveData<String> getConnectionError(){ return connectionError; }
    public LiveData<Situation> getSelected(){ return selected; }
    public LiveData<Boolean> getLoading(){ return loading; }
    public LiveData<Integer> getCurrentTab(){ return currentTab; }
    public void setCurrentTab(int tab){ currentTab.setValue(tab); }
    public LiveData<ArrayList<Situation>> getSearchResults(){ return searchResults; }
    public String getLastSearch(){ return lastSearch; }
    public boolean isMapResultsOnly(){ return mapResultsOnly; }
    public void setMapResultsOnly(boolean resultsOnly){ mapResultsOnly = resultsOnly; }
    public LiveData<Integer> getMapType(){ return mapType; }
    public void setMapType(int type){ mapType.setValue(type); }

    // a list row was tapped - the detail screen will observe this
    public void select(Situation situation){ selected.setValue(situation); }

    // search incidents AND roadworks for a road name, e.g. "M8" (lab 2 String methods)
    public void searchByRoad(String road){
        lastSearch = road.trim().toUpperCase().replace(" ", ""); // "m 8" -> "M8"
        ArrayList<Situation> found = new ArrayList<>();
        addMatches(incidents.getValue(), found);
        addMatches(roadworks.getValue(), found);
        searchResults.setValue(found);
    }

    private void addMatches(ArrayList<Situation> list, ArrayList<Situation> found){
        if (list == null) return; // not downloaded yet
        for (Situation s : list) {
            if (matchesRoad(s.getComment(), lastSearch)) {
                found.add(s);
            }
        }
    }

    // search both lists for anything within a number of miles of a town
    public void searchByDistance(String town, double townLat, double townLon, double miles){
        // show "10" not "10.0" if it's a whole number of miles
        String milesText = (miles == Math.floor(miles)) ? String.valueOf((int) miles) : String.valueOf(miles);
        lastSearch = "within " + milesText + " miles of " + town;
        ArrayList<Situation> found = new ArrayList<>();
        addNearby(incidents.getValue(), found, townLat, townLon, miles);
        addNearby(roadworks.getValue(), found, townLat, townLon, miles);
        searchResults.setValue(found);
    }

    private void addNearby(ArrayList<Situation> list, ArrayList<Situation> found, double townLat, double townLon, double miles){
        if (list == null) return; // not downloaded yet
        for (Situation s : list) {
            if (s.getFromLat() == 0 && s.getFromLon() == 0) continue; // no location in the feed, skip it
            if (milesBetween(townLat, townLon, s.getFromLat(), s.getFromLon()) <= miles) {
                found.add(s);
            }
        }
    }

    // rough distance in miles - treats scotland as flat
    // 1 degree of latitude is about 69 miles, 1 degree of longitude about 39 miles up here
    // then pythagoras for the straight line between them
    private static double milesBetween(double lat1, double lon1, double lat2, double lon2){
        double north = (lat2 - lat1) * 69;
        double east = (lon2 - lon1) * 39;
        return Math.sqrt(north * north + east * east);
    }

    // is the road in the comment? checks the characters either side so "M8" doesn't match "M80" or "M876"
    private static boolean matchesRoad(String comment, String road){
        if (comment == null || road.isEmpty()) return false;
        String text = comment.toUpperCase();
        int i = text.indexOf(road);
        while (i != -1) {
            int end = i + road.length();
            boolean startOk = (i == 0) || !Character.isLetterOrDigit(text.charAt(i - 1));
            boolean endOk = (end == text.length()) || !Character.isDigit(text.charAt(end));
            if (startOk && endOk) return true;
            i = text.indexOf(road, i + 1); // try the next place it appears
        }
        return false;
    }

    // once the toast has been shown, clear it so it doesn't pop up again after a rotation
    public void clearConnectionError(){ connectionError.setValue(null); }

    // called from the activity's onCreate - only starts things the first time,
    // so rotating the phone doesn't start a second download or a second timer
    public void start(String clientId, String clientKey, File saveFolder){
        if (model != null) {
            return;   // already running
        }
        model = new TrafficModel(clientId, clientKey, saveFolder);
        createUpdateUiHandler();
        startProgress(); // auto-update at start up - don't wait for the button
        // timer gets started in resumeUpdates() (onResume)
    }

    // stop/start the hourly update when the app goes off/on screen (lecture 4a onPause/onResume)
    public void pauseUpdates(){
        if (updateUIHandler != null) updateUIHandler.removeCallbacks(autoUpdate);
    }

    public void resumeUpdates(){
        if (updateUIHandler == null) return;
        updateUIHandler.removeCallbacks(autoUpdate); // make sure there's only ever one timer
        updateUIHandler.postDelayed(autoUpdate, UPDATE_INTERVAL); // keep updating every hour
    }

    public void startProgress()
    {
        // Run network access on a separate thread;
        loading.setValue(true); // show the spinner (this is always called on the UI thread)
        new Thread(new Task()).start();
    } //

    // timer: download again, then schedule the next one
    private final Runnable autoUpdate = new Runnable() {
        @Override
        public void run() {
            startProgress();
            updateUIHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    // handler lives on the UI thread, so it's allowed to update the LiveData (and so the screen)
    private void createUpdateUiHandler(){
        if(updateUIHandler == null){
            updateUIHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg){
                    // check the message type
                    if(msg.what == MESSAGE_INCIDENTS){
                        incidents.setValue((ArrayList<Situation>) msg.obj);
                        usingSavedData = (msg.arg1 == 1); // arg1 = 1 means it came from the saved file
                        updateStatus();
                    } else if(msg.what == MESSAGE_ROADWORKS){
                        roadworks.setValue((ArrayList<Situation>) msg.obj);
                        usingSavedData = (msg.arg1 == 1);
                        updateStatus();
                    } else if(msg.what == MESSAGE_NO_DATA){
                        // no connection - keep the last lists and just flag it
                        connectionError.setValue("No connection - couldn't update the traffic data");
                        if (incidents.getValue() == null && roadworks.getValue() == null) {
                            status.setValue("No connection - no traffic data yet"); // nothing to show at all
                        }
                    } else if(msg.what == MESSAGE_DONE){
                        loading.setValue(false); // hide the spinner
                    }
                }
            };
        }
    }

    // status line under the counts, e.g. "Updated at 17:16" (the counts are in the tiles now)
    private void updateStatus(){
        if (usingSavedData) {
            status.setValue("Showing the saved copy - no connection");
        } else {
            String time = new SimpleDateFormat("HH:mm", Locale.UK).format(new Date()); // time now, like 17:16
            status.setValue("Updated at " + time);
        }
    }

    // Need separate thread to access the internet resource over network
    // Other neater solutions should be adopted in later iterations.
    private class Task implements Runnable
    {
        @Override
        public void run(){
            Log.d("MyTask","in run");
            // get both feeds - the model downloads and parses each one (null = download failed)
            ArrayList<Situation> newIncidents = model.getSituations(unplannedEventsURL, INCIDENTS_FILE);
            ArrayList<Situation> newRoadworks = model.getSituations(roadworksURL, ROADWORKS_FILE);
            int fromSaved = 0; // 1 = data came from the saved file (sent in arg1)
            if (newIncidents == null && newRoadworks == null) {
                // nothing came back (no internet?) - stop here so it doesn't crash
                Log.e("MyTask", "No XML downloaded - check the internet connection and DATEX keys");
                Message failed = new Message();
                failed.what = MESSAGE_NO_DATA;
                updateUIHandler.sendMessage(failed);   // tell the UI thread so it can show a toast
                // no internet - use the last saved copy instead
                newIncidents = model.getSavedSituations(INCIDENTS_FILE);
                newRoadworks = model.getSavedSituations(ROADWORKS_FILE);
                fromSaved = 1;
                if (newIncidents == null && newRoadworks == null) {
                    sendDone();
                    return; // nothing saved yet either - keep showing whatever's on screen
                }
            }

            // Now that you have the xml data into result, you can parse it
            // (parsing is done in TrafficModel now - one list for incidents, one for roadworks)

            // Now update the TextView to display raw XML data
            // Probably not the best way to update TextView
            // but we are just getting started !
            // (no raw XML on screen any more - the lists get the parsed data below)

            // send each list to the UI thread's handler instead of runOnUiThread
            if (newIncidents != null) {
                Message message = new Message();
                message.what = MESSAGE_INCIDENTS; // message type
                message.obj = newIncidents;
                message.arg1 = fromSaved;
                updateUIHandler.sendMessage(message);
            }
            if (newRoadworks != null) {
                Message message = new Message();
                message.what = MESSAGE_ROADWORKS;
                message.obj = newRoadworks;
                message.arg1 = fromSaved;
                updateUIHandler.sendMessage(message);
            }
            sendDone();
        }

        // tell the UI thread the download's finished so it can hide the spinner
        private void sendDone(){
            Message done = new Message();
            done.what = MESSAGE_DONE;
            updateUIHandler.sendMessage(done);
        }
    }
}
