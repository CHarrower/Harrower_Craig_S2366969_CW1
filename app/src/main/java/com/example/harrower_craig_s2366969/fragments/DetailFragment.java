package com.example.harrower_craig_s2366969.fragments;

//
// Name                 Craig Harrower
// Student ID           S2366969
// Programme of Study   BSc Software Development
//

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.harrower_craig_s2366969.R;
import com.example.harrower_craig_s2366969.model.Situation;
import com.example.harrower_craig_s2366969.viewmodel.TrafficViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// details of the situation tapped in a list - observes the selected situation in the ViewModel
// + a little map showing where it is (lecture 6c: OnMapReadyCallback + addMarker)
public class DetailFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener
{
    private Button backButton;
    private ImageView detailIcon;
    private View detailDelayBar;
    private TextView detailRoad, detailDirection;
    private TextView detailType, detailComment, detailDelays, detailLanes, detailStart, detailEnd, detailFrom, detailTo;
    private GoogleMap miniMap;           // the little map
    private TrafficViewModel viewModel;
    private Situation current;           // the situation on screen (the map can load before or after it)

    public DetailFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_detail, container, false);
        detailIcon = v.findViewById(R.id.detailIcon);
        detailDelayBar = v.findViewById(R.id.detailDelayBar);
        detailType = v.findViewById(R.id.detailType);
        detailComment = v.findViewById(R.id.detailComment);
        detailRoad = v.findViewById(R.id.detailRoad);
        detailDirection = v.findViewById(R.id.detailDirection);
        detailDelays = v.findViewById(R.id.detailDelays);
        detailLanes = v.findViewById(R.id.detailLanes);
        detailStart = v.findViewById(R.id.detailStart);
        detailEnd = v.findViewById(R.id.detailEnd);
        detailFrom = v.findViewById(R.id.detailFrom);
        detailTo = v.findViewById(R.id.detailTo);
        backButton = v.findViewById(R.id.backButton);
        backButton.setOnClickListener(this);

        // same ViewModel as MainActivity, so it knows which situation was tapped (and keeps it after a rotation)
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        viewModel.getSelected().observe(getViewLifecycleOwner(), this::showSituation);

        // the map box is a fragment inside this fragment, so child fragment manager (same as the map screen)
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.detailMap);
        mapFragment.getMapAsync(this); // calls onMapReady when it's loaded
        return v;
    }

    // back button - clear the selection and MainActivity slides this away
    // (the list is still underneath so you end up where you were)
    @Override
    public void onClick(View v)
    {
        viewModel.select(null);
    }

    // fill in the screen for the selected situation
    private void showSituation(Situation situation)
    {
        if (situation == null) {
            return; // nothing selected yet
        }
        current = situation;
        showOnMiniMap();
        detailIcon.setImageResource(SituationAdapter.iconFor(situation));
        detailType.setText(SituationAdapter.readableType(situation));
        detailDelayBar.setBackgroundColor(getResources().getColor(SituationAdapter.delayColour(situation.getDelaysType()), null));
        detailComment.setText(situation.getComment() == null ? "No description" : situation.getComment());
        detailRoad.setText(situation.getRoad().isEmpty() ? "Not given" : situation.getRoad());
        detailDirection.setText(situation.getDirection().isEmpty() ? "Not given" : situation.getDirection());

        String delays = SituationAdapter.readableDelays(situation.getDelaysType());
        detailDelays.setText(delays.isEmpty() ? "None reported" : delays.substring(0, 1).toUpperCase() + delays.substring(1));

        // 0 could mean "not in the feed", so only show lanes when the feed gave a number
        if (situation.getOperationalLanes() > 0 || situation.getLanesRestricted() > 0) {
            detailLanes.setText(situation.getOperationalLanes() + " open, " + situation.getLanesRestricted() + " restricted");
        } else {
            detailLanes.setText("No lane information");
        }

        detailStart.setText(readableDate(situation.getStartTime()));
        detailEnd.setText(situation.getEndTime() == null ? "No end date given" : readableDate(situation.getEndTime()));
        detailFrom.setText("From: " + situation.getFromLat() + ", " + situation.getFromLon());
        // lots of incidents only have one point, so no "to" in the feed (would show 0.0, 0.0)
        if (situation.getToLat() == 0 && situation.getToLon() == 0) {
            detailTo.setText("To: not given");
        } else {
            detailTo.setText("To: " + situation.getToLat() + ", " + situation.getToLon());
        }
    }

    // little map is ready (lecture 6c slide 8)
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        miniMap = googleMap;
        // same map type as picked in the toolbar menu (normal / satellite / terrain)
        viewModel.getMapType().observe(getViewLifecycleOwner(), type -> miniMap.setMapType(type));
        showOnMiniMap();
    }

    // marker at the "from" point (and the "to" point if there is one), zoomed in on it
    private void showOnMiniMap()
    {
        if (miniMap == null || current == null) return; // wait until both the map and the situation are ready
        miniMap.clear();
        if (current.getFromLat() == 0 && current.getFromLon() == 0) return; // no location in the feed
        LatLng from = new LatLng(current.getFromLat(), current.getFromLon());
        miniMap.addMarker(new MarkerOptions().position(from).title(SituationAdapter.readableType(current)));
        if (current.getToLat() != 0 || current.getToLon() != 0) {
            LatLng to = new LatLng(current.getToLat(), current.getToLon());
            miniMap.addMarker(new MarkerOptions().position(to).title("End"));
        }
        miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(from, 12)); // close enough to see the roads
    }

    // "2023-07-22T00:00:00+01:00" -> "22/07/2023 at 00:00" (just string methods, like week 2)
    public static String readableDate(String dateTime) // static so the list can use it too
    {
        if (dateTime == null || dateTime.length() < 16) return "Not given";
        String[] date = dateTime.substring(0, 10).split("-");
        return date[2] + "/" + date[1] + "/" + date[0] + " at " + dateTime.substring(11, 16);
    }
}
