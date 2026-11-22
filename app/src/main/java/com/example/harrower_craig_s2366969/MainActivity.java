/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/
// (it shows the lists, map and search now - not the raw data)

//
// Name                 Craig Harrower
// Student ID           S2366969
// Programme of Study   BSc Software Development
//

// UPDATE THE PACKAGE NAME to include your Student Identifier
package com.example.harrower_craig_s2366969;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.View.OnClickListener;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.harrower_craig_s2366969.fragments.DetailFragment;
import com.example.harrower_craig_s2366969.fragments.IncidentsFragment;
import com.example.harrower_craig_s2366969.fragments.RoadworksFragment;
import com.example.harrower_craig_s2366969.fragments.SearchFragment;
import com.example.harrower_craig_s2366969.fragments.TrafficMapFragment;
import com.example.harrower_craig_s2366969.model.Situation;
import com.example.harrower_craig_s2366969.viewmodel.TrafficViewModel;
import com.google.android.gms.maps.GoogleMap;

import android.content.pm.PackageManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private TextView rawDataDisplay;
    private TextView incidentCountText;   // summary tiles
    private TextView roadworkCountText;
    private ProgressBar loadingSpinner;   // spinner while downloading (lab 6 progress bar)
    // (refresh button moved into the toolbar menu)
    private Button incidentsButton;
    private Button roadworksButton;
    private Button mapButton;
    private Button searchButton;

    // MVVM (lecture 7c) - the ViewModel holds the data and survives rotation
    private TrafficViewModel viewModel;

    //DOWNLOADING FROM TRAFFIC SCOTLAND
    // keys now come from the local.properties via the manifest <meta-data>
    private String clientId = "";
    private String clientKey = "";
    // (the feed addresses are in TrafficViewModel now)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // toolbar for the options menu (lecture 7a, AppBarTest)
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        setTitle("Traffic Scotland");
        // Set up the raw links to the graphical components
        rawDataDisplay = (TextView)findViewById(R.id.rawDataDisplay);
        incidentsButton = findViewById(R.id.incidentsButton);
        incidentsButton.setOnClickListener(this);
        roadworksButton = findViewById(R.id.roadworksButton);
        roadworksButton.setOnClickListener(this);
        mapButton = findViewById(R.id.mapButton);
        mapButton.setOnClickListener(this);
        searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(this);
        incidentCountText = findViewById(R.id.incidentCountText);
        roadworkCountText = findViewById(R.id.roadworkCountText);
        loadingSpinner = findViewById(R.id.loadingSpinner);

        // More Code goes here
        // read the DATEX II keys from the manifest <meta-data>
        Bundle metaData = null;
        try {
            metaData = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            clientId =  metaData.getString("com.example.harrower_craig_s2366969.DATEX_CLIENT_ID");
            clientKey =  metaData.getString("com.example.harrower_craig_s2366969.DATEX_CLIENT_KEY");
        } catch (PackageManager.NameNotFoundException e) { throw new RuntimeException(e); }
        Log.d("Keys", "DATEX keys loaded: " + (clientId != null && !clientId.isEmpty()));

        // get the ViewModel - created the first time, reconnected to after a rotation (lecture 7c)
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TrafficViewModel.class);
        // observe the data so the screen updates by itself (observableViewModel example)
        viewModel.getStatus().observe(this, this::showStatus);
        viewModel.getConnectionError().observe(this, this::showConnectionError);
        viewModel.getSelected().observe(this, this::showDetails); // a list row was tapped
        // counts in the summary tiles, spinner while downloading, and which tab is highlighted
        viewModel.getIncidents().observe(this, list -> incidentCountText.setText(String.valueOf(list.size())));
        viewModel.getRoadworks().observe(this, list -> roadworkCountText.setText(String.valueOf(list.size())));
        viewModel.getLoading().observe(this, busy -> loadingSpinner.setVisibility(busy ? View.VISIBLE : View.GONE));
        viewModel.getCurrentTab().observe(this, this::highlightTab);
        viewModel.start(clientId, clientKey, getApplication().getFilesDir()); // download at start up + timer, and where to save the feeds

        // show the incidents list first - only when the app first opens (savedInstanceState is null),
        // after a rotation the fragment manager puts the last fragment back by itself (lecture 4a)
        if (savedInstanceState == null) {
            showFragment(new IncidentsFragment());
        }
    }

    // swap the fragment shown in the holder (FragmentExample / fragment_comms)
    private void showFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, fragment).commit();
    }

    // status line - hidden while it's empty so there aren't two loading messages
    private void showStatus(String text){
        rawDataDisplay.setText(text);
        rawDataDisplay.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // highlight the tab for the screen that's showing, the others go grey
    private void highlightTab(int tab){
        Button[] tabs = {incidentsButton, roadworksButton, mapButton, searchButton}; // same order as the TAB_ numbers
        for (int i = 0; i < tabs.length; i++) {
            int colour;
            if (i == tab) {
                colour = getResources().getColor(R.color.tab_selected_text, null);
                tabs[i].setBackgroundColor(getResources().getColor(R.color.tab_selected_bg, null));
            } else {
                colour = getResources().getColor(R.color.tab_text, null);
                tabs[i].setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
            }
            tabs[i].setTextColor(colour);
            ((MaterialButton) tabs[i]).setIconTint(ColorStateList.valueOf(colour)); // icon goes the same colour as the text
        }
    }

    // open the details for the tapped situation - slides up over everything (anim files in res/anim, lecture 7b)
    // nothing selected any more (back button / a tab) = slide it back down
    private void showDetails(Situation situation){
        FrameLayout detailHolder = findViewById(R.id.detailHolder);
        Fragment showing = getSupportFragmentManager().findFragmentById(R.id.detailHolder);
        if (situation != null) {
            detailHolder.setVisibility(View.VISIBLE);
            if (showing == null) { // not already open (after a rotation it's put back by itself)
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.detailHolder, new DetailFragment()).commit();
            }
        } else if (showing != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, R.anim.slide_down)
                    .remove(showing).commit();
            // hide the holder once the slide has finished (postDelayed, lecture 5a)
            detailHolder.postDelayed(() -> {
                if (viewModel.getSelected().getValue() == null) detailHolder.setVisibility(View.GONE);
            }, 300);
        }
    }

    // no-connection toast (lecture 3c)
    private void showConnectionError(String error){
        if (error != null) {
            Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            viewModel.clearConnectionError();
        }
    }

    // pause the hourly update when the app's not on screen (lecture 4a)
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.resumeUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.pauseUpdates();
    }

    // put the menu in the toolbar (AppBarTest)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // menu picked - refresh, map type or about (the buttons do the screens)
    // (if/else not switch because the R.id values aren't constants any more)
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.menuRefresh) {
            viewModel.startProgress(); // manual refresh
            Toast.makeText(this, "Refreshing traffic data...", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menuMapNormal) {
            setMapType(GoogleMap.MAP_TYPE_NORMAL, "normal");
        } else if (id == R.id.menuMapSatellite) {
            setMapType(GoogleMap.MAP_TYPE_SATELLITE, "satellite");
        } else if (id == R.id.menuMapTerrain) {
            setMapType(GoogleMap.MAP_TYPE_TERRAIN, "terrain");
        } else if (id == R.id.menuAbout) {
            showAbout();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // map type is kept in the ViewModel so the map picks it up (and keeps it after a rotation)
    private void setMapType(int type, String name)
    {
        viewModel.setMapType(type);
        Toast.makeText(this, "Map type: " + name, Toast.LENGTH_SHORT).show();
    }

    // about box with where the data and icons come from (dialog like lab 5)
    private void showAbout()
    {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("Traffic data: contains Traffic Scotland data (DATEX II feeds)\n\n"
                        + "Icons: Font Awesome Free (fontawesome.com), CC BY 4.0\n\n"
                        + "Map: Google Maps\n\n"
                        + "Craig Harrower - S2366969")
                .setPositiveButton("OK", null)
                .show();
    }

    public void onClick(View aview)
    {
        if (aview == incidentsButton) {
            viewModel.select(null); // back to a list - clear the selection so a rotation doesn't reopen the details
            viewModel.setCurrentTab(TrafficViewModel.TAB_INCIDENTS);
            showFragment(new IncidentsFragment());
        } else if (aview == roadworksButton) {
            viewModel.select(null);
            viewModel.setCurrentTab(TrafficViewModel.TAB_ROADWORKS);
            showFragment(new RoadworksFragment());
        } else if (aview == mapButton) {
            viewModel.select(null); // same as the lists - no detail screen popping back up
            viewModel.setMapResultsOnly(false); // map button = show everything
            viewModel.setCurrentTab(TrafficViewModel.TAB_MAP);
            showFragment(new TrafficMapFragment());
        } else if (aview == searchButton) {
            viewModel.select(null);
            viewModel.setCurrentTab(TrafficViewModel.TAB_SEARCH);
            showFragment(new SearchFragment());
        }
    }

}
