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

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.harrower_craig_s2366969.R;
import com.example.harrower_craig_s2366969.model.Situation;
import com.example.harrower_craig_s2366969.viewmodel.TrafficViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

// map of the situations - one marker each (lecture 6c: OnMapReadyCallback + addMarker)
public class TrafficMapFragment extends Fragment implements OnMapReadyCallback
{
    private GoogleMap mMap;
    private TrafficViewModel viewModel;

    public TrafficMapFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        // same ViewModel as MainActivity (requireActivity), so the map sees the same data
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        // the map is a fragment inside this fragment, so it's the child fragment manager here
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // calls onMapReady when the map is loaded
        return v;
    }

    // map is ready (lecture 6c slide 8)
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        // start zoomed out over Scotland
        LatLng scotland = new LatLng(56.8, -4.2);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scotland, 6));

        // redraw the markers whenever either list changes (observableViewModel example)
        viewModel.getIncidents().observe(getViewLifecycleOwner(), list -> showMarkers());
        viewModel.getRoadworks().observe(getViewLifecycleOwner(), list -> showMarkers());
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), list -> showMarkers());
        // normal / satellite / terrain picked from the toolbar menu (lab 7 task 3)
        viewModel.getMapType().observe(getViewLifecycleOwner(), type -> mMap.setMapType(type));
    }

    // clear the map and put a marker on every incident + roadwork
    // (or just the road search results if it was opened from the search screen)
    private void showMarkers()
    {
        mMap.clear();
        if (viewModel.isMapResultsOnly()) {
            ArrayList<Situation> results = viewModel.getSearchResults().getValue();
            addMarkers(results, -1); // -1 = work out the colour for each one
            // zoom in on the first result so they're easy to see
            if (results != null && !results.isEmpty()) {
                Situation first = results.get(0);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(first.getFromLat(), first.getFromLon()), 8));
            }
        } else {
            addMarkers(viewModel.getIncidents().getValue(), BitmapDescriptorFactory.HUE_RED);    // incidents red
            addMarkers(viewModel.getRoadworks().getValue(), BitmapDescriptorFactory.HUE_AZURE);  // roadworks blue - easy to tell apart from red
        }
    }

    // colour = marker hue, or -1 to pick it per situation (search results are a mix of both)
    private void addMarkers(ArrayList<Situation> list, float colour)
    {
        if (list == null) return; // not downloaded yet
        for (Situation s : list) {
            if (s.getFromLat() == 0 && s.getFromLon() == 0) continue; // no location in the feed, skip it
            LatLng pos = new LatLng(s.getFromLat(), s.getFromLon());
            float hue = colour;
            if (hue < 0) {
                ArrayList<Situation> roadworks = viewModel.getRoadworks().getValue();
                hue = (roadworks != null && roadworks.contains(s)) ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_RED;
            }
            // tap a marker to see the type + comment
            mMap.addMarker(new MarkerOptions().position(pos)
                    .title(SituationAdapter.readableType(s))
                    .snippet(s.getComment())
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }
    }
}
