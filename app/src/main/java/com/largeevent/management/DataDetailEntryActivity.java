package com.largeevent.management;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class DataDetailEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_detail_entry);
        initToolbar();
        initActions();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initActions() {
        View cardPerson = findViewById(R.id.card_person_entry);
        View cardVehicle = findViewById(R.id.card_vehicle_entry);
        cardPerson.setOnClickListener(v -> openList(false));
        cardVehicle.setOnClickListener(v -> openList(true));
    }

    private void openList(boolean vehicle) {
        Intent intent = CertificateListActivity.newIntent(this, vehicle);
        startActivity(intent);
    }
}


