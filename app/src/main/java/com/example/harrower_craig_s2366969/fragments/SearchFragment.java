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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.harrower_craig_s2366969.R;
import com.example.harrower_craig_s2366969.model.Situation;
import com.example.harrower_craig_s2366969.viewmodel.TrafficViewModel;

import java.util.ArrayList;

// search by road name or by distance from a town - looks through incidents AND roadworks (lecture 3a EditText + Spinner + ListView)
public class SearchFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener
{
    private EditText roadInput;
    private Button roadSearchButton;
    private Button showOnMapButton;
    private Spinner townSpinner;
    private EditText milesInput;
    private Button distanceSearchButton;
    private TextView resultsText;
    private ListView resultsList;
    private TrafficViewModel viewModel;

    // towns for the distance search - fixed lat/long so no gps needed
    private final String[] towns = {"Glasgow", "Edinburgh", "Aberdeen", "Dundee", "Inverness", "Stirling", "Perth"};
    private final double[] townLats = {55.8642, 55.9533, 57.1497, 56.4620, 57.4778, 56.1165, 56.3950};
    private final double[] townLons = {-4.2518, -3.1883, -2.0943, -2.9707, -4.2247, -3.9369, -3.4308};

    public SearchFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        roadInput = v.findViewById(R.id.roadInput);
        roadSearchButton = v.findViewById(R.id.roadSearchButton);
        roadSearchButton.setOnClickListener(this);
        showOnMapButton = v.findViewById(R.id.showOnMapButton);
        showOnMapButton.setOnClickListener(this);
        resultsText = v.findViewById(R.id.resultsText);
        resultsList = v.findViewById(R.id.resultsList);
        resultsList.setOnItemClickListener(this);

        // distance search widgets - spinner filled from the towns array (lecture 3a)
        townSpinner = v.findViewById(R.id.townSpinner);
        ArrayAdapter<String> townAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, towns);
        townAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        townSpinner.setAdapter(townAdapter);
        milesInput = v.findViewById(R.id.milesInput);
        distanceSearchButton = v.findViewById(R.id.distanceSearchButton);
        distanceSearchButton.setOnClickListener(this);

        // same ViewModel as MainActivity (requireActivity), so the results survive a rotation
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::showResults);
        return v;
    }

    @Override
    public void onClick(View aview)
    {
        if (aview == roadSearchButton) {
            String road = roadInput.getText().toString();
            if (road.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Type a road first, e.g. M8", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.searchByRoad(road); // results come back through getSearchResults()
        } else if (aview == distanceSearchButton) {
            double miles;
            try {
                miles = Double.parseDouble(milesInput.getText().toString()); // text -> number, like lab 2 task 3 (so 2.5 miles works too)
            } catch (NumberFormatException e) { // empty or silly number (try/catch like week 2)
                Toast.makeText(requireContext(), "Type how many miles, e.g. 10", Toast.LENGTH_SHORT).show();
                return;
            }
            int t = townSpinner.getSelectedItemPosition(); // which town is picked
            viewModel.searchByDistance(towns[t], townLats[t], townLons[t], miles);
        } else if (aview == showOnMapButton) {
            ArrayList<Situation> results = viewModel.getSearchResults().getValue();
            if (results == null || results.isEmpty()) {
                Toast.makeText(requireContext(), "Search for a road first", Toast.LENGTH_SHORT).show();
                return;
            }
            // map shows just the search results, then swap to the map (FragmentExample)
            viewModel.setMapResultsOnly(true);
            viewModel.setCurrentTab(TrafficViewModel.TAB_MAP); // highlight the map tab
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentHolder, new TrafficMapFragment()).commit();
        }
    }

    // new results from the ViewModel - put them in the list
    private void showResults(ArrayList<Situation> list)
    {
        if (list == null) return; // no search yet
        String search = viewModel.getLastSearch();
        if (list.isEmpty()) {
            // friendly message instead of an empty list
            if (search.startsWith("within")) {
                resultsText.setText("Nothing found " + search + " - try a bigger distance");
            } else {
                resultsText.setText("No results for " + search + " - try another road, like M8 or A9");
            }
        } else {
            resultsText.setText(list.size() + " found (" + search + ") - tap one for details");
        }
        resultsList.setAdapter(new SituationAdapter(requireContext(), list));
    }

    // a result was tapped - same as the other lists, MainActivity opens the detail screen
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Situation selected = (Situation) parent.getItemAtPosition(position);
        viewModel.select(selected);
    }
}
