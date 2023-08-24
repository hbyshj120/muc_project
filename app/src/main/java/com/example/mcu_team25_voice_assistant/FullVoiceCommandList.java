package com.example.mcu_team25_voice_assistant;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class FullVoiceCommandList extends Activity {
    private DBHelper dbHelper;
    private ArrayList<VoiceCommand> voiceCommandArrayList;
    String TAG = "Full Voice Command List: ";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullvoicecommandlist);

        // initialize db Helper
        dbHelper = new DBHelper(FullVoiceCommandList.this);

        // get all voice commands
        voiceCommandArrayList = dbHelper.readData();

        String stringArray[] = new String[voiceCommandArrayList.size()];
        for (int i = 0; i < voiceCommandArrayList.size(); ++i) {
            VoiceCommand voiceCommand = voiceCommandArrayList.get(i);
            stringArray[i] = voiceCommand.getName();
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.voicecommand_listview, stringArray);

        ListView listView = (ListView) findViewById(R.id.mobile_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long item_id)
            {
                Log.d(TAG,   String.valueOf(position) + "  " + item_id);
                Intent n = new Intent(getApplicationContext(), AddNewVoiceCommand.class);
                n.putExtra("commandName", stringArray[position]);
                n.putExtra("isModify", true);
                startActivity(n);
            }
        });
    }
}
