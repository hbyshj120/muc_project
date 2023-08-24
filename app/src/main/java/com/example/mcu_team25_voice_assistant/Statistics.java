package com.example.mcu_team25_voice_assistant;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Statistics  extends Activity implements AdapterView.OnItemSelectedListener {
    private ArrayList<String> dropdownList;

    private ArrayList<VoiceCommand> voiceCommandArrayList;

    TextView lastUse;
    TextView totalUse;
    TextView avglUse;
    TextView exit;

    private DBHelper dbHelper;

    private static final String TAG = "Statistics: ";

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);


        // initialize the dropdown List
        dropdownList = new ArrayList<>();

        // initialize db Helper
        dbHelper = new DBHelper(Statistics.this);

        // get all Jobs
        voiceCommandArrayList = dbHelper.readData();

        // calculate the job score
        for (int i = 0; i < voiceCommandArrayList.size(); ++i) {
            VoiceCommand voiceCommand = voiceCommandArrayList.get(i);
            Log.d("JobCompare", Integer.toString(i) + " - " + voiceCommand.printCommand());

            String displayText = null;

            displayText = voiceCommand.getName();
            dropdownList.add(displayText);
        }

        //convert array list to array for the dropdown items
        String[] dropdownArray = dropdownList.toArray(new String[dropdownList.size()]);

        // reference: https://stackoverflow.com/questions/13377361/how-to-create-a-drop-down-list
        // get the spinner1_Job from the xml
        Spinner dropdown1 = findViewById(R.id.spinner_id);
        //create an adapter to describe how the items are displayed
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dropdownArray);
        //set the spinners adapter to the previously created one.
        dropdown1.setAdapter(adapter1);
        // assign dropdown to listener
        dropdown1.setOnItemSelectedListener(this);

        lastUse =(TextView)findViewById(R.id.lastuse);
        totalUse =(TextView)findViewById(R.id.totaluse);
        avglUse =(TextView)findViewById(R.id.avguse);



        exit = findViewById(R.id.statisticsReturn);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Exit the Statistics");
                Intent intent = new Intent(Statistics.this, MainPage.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String selected = adapterView.getItemAtPosition(i).toString();
        if  (adapterView.getId() == R.id.spinner_id) {
            Log.d("JobCompare", "Job 1 selected - " + selected);
            Toast.makeText(this, selected, Toast.LENGTH_SHORT).show();

            Cursor cursor = dbHelper.getCommand(selected);
            cursor.moveToFirst(); // https://stackoverflow.com/questions/50525179/gdx-sqlite-android-database-cursorindexoutofboundsexception-index-1-requested

            VoiceCommand command = new VoiceCommand(cursor.getString(1),
                    cursor.getString(2), cursor.getFloat(3), cursor.getInt(4), cursor.getFloat(5));

            lastUse.setText(String.valueOf(command.getLastUsage()));
            totalUse.setText(String.valueOf(command.getTotalUsages()));
            avglUse.setText(String.valueOf(command.getAverageUsages()));

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


}


