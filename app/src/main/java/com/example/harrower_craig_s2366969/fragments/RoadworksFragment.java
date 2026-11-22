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

// list of roadworks (type + comment), tap one to select it (lecture 3a ListView + onItemClick)
public class RoadworksFragment extends Fragment implements AdapterView.OnItemClickListener
{
    private TextView roadworksText;
    private ListView roadworksList;
    private TrafficViewModel viewModel;

    public RoadworksFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_roadworks, container, false);
        roadworksText = v.findViewById(R.id.roadworksText);
        roadworksList = v.findViewById(R.id.roadworksList);
        roadworksList.setOnItemClickListener(this);

        // same ViewModel as MainActivity (requireActivity), so the fragment sees the same data
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        // observe the list so this screen updates by itself (observableViewModel example)
        viewModel.getRoadworks().observe(getViewLifecycleOwner(), this::showRoadworks);
        return v;
    }

    // new data from the ViewModel - put it in the list
    private void showRoadworks(ArrayList<Situation> list)
    {
        if (list.isEmpty()) {
            roadworksText.setText("No roadworks reported right now"); // friendly message instead of a blank list
        } else {
            roadworksText.setText(list.size() + " roadworks - tap one for details");
        }
        roadworksList.setAdapter(new SituationAdapter(requireContext(), list));
    }

    // a row was tapped - tell the ViewModel which one (lecture 3a onItemClick)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Situation selected = (Situation) parent.getItemAtPosition(position);
        viewModel.select(selected); // MainActivity sees this and opens the detail screen
    }
}
