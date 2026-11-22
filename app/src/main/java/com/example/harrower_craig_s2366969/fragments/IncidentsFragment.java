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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.harrower_craig_s2366969.R;
import com.example.harrower_craig_s2366969.model.Situation;
import com.example.harrower_craig_s2366969.viewmodel.TrafficViewModel;

import java.util.ArrayList;

// list of incidents (type + comment), tap one to select it (lecture 3a ListView + onItemClick)
public class IncidentsFragment extends Fragment implements AdapterView.OnItemClickListener
{
    private TextView incidentsText;
    private ListView incidentsList;
    private TrafficViewModel viewModel;

    public IncidentsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_incidents, container, false);
        incidentsText = v.findViewById(R.id.incidentsText);
        incidentsList = v.findViewById(R.id.incidentsList);
        incidentsList.setOnItemClickListener(this);

        // same ViewModel as MainActivity (requireActivity), so the fragment sees the same data
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        // observe the list so this screen updates by itself (observableViewModel example)
        viewModel.getIncidents().observe(getViewLifecycleOwner(), this::showIncidents);
        return v;
    }

    // new data from the ViewModel - put it in the list
    private void showIncidents(ArrayList<Situation> list)
    {
        if (list.isEmpty()) {
            incidentsText.setText("No incidents reported right now"); // friendly message instead of a blank list
        } else {
            if (list.size() == 1) {
                incidentsText.setText("1 incident - tap it for details"); // not "1 incidents"
            } else {
                incidentsText.setText(list.size() + " incidents - tap one for details");
            }
        }
        incidentsList.setAdapter(new SituationAdapter(requireContext(), list));
    }

    // a row was tapped - tell the ViewModel which one (lecture 3a onItemClick)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Situation selected = (Situation) parent.getItemAtPosition(position);
        viewModel.select(selected); // MainActivity sees this and opens the detail screen
    }
}
