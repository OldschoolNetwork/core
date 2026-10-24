package com.oldschoolnetwork.core;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvResults;
    private DashboardActivity.SearchAdapter adapter;
    private View noResults;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        etSearch = v.findViewById(R.id.et_search_tab);
        rvResults = v.findViewById(R.id.rv_search_results);
        noResults = v.findViewById(R.id.layout_no_results);

        rvResults.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = ((DashboardActivity) getActivity()).getSearchAdapter();
        rvResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
                noResults.setVisibility(adapter.getItemCount() == 0 && !s.toString().isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        return v;
    }
}
