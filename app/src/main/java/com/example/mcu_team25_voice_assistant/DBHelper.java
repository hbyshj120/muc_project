package com.example.mcu_team25_voice_assistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    private Context context;
    private static final String db_name = "VoiceCommand.db";
    private static final int db_version = 1;
    private static final String table_name = "voice_command";
    private static final String col_id = "_id";
    private static final String column_name = "name";
    private static final String column_path = "path";
    private static final String column_last_usage = "lastUsage";
    private static final String column_num_usages = "numUsages";
    private static final String column_total_usages = "totalUsages";

    //Source:
    // https://www.geeksforgeeks.org/how-to-create-and-add-data-to-sqlite-database-in-android/
    public DBHelper(@Nullable Context context) {
        super(context, db_name, null, db_version);
        this.context = context;
    }

    // /data/data/<your_app_package_name>/databases/<database_name>
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query =
                "CREATE TABLE " + table_name +
                        " (" + col_id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        column_name + " TEXT, " +
                        column_path + " TEXT, " +
                        column_last_usage + " REAL, " +
                        column_num_usages + " INTEGER, " +
                        column_total_usages + " REAL);";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + table_name);

    }

    void addCommand(String name, String path, float lastUsage, int numUsages, float totalUsages)

    {
        // create a new database object
        SQLiteDatabase db_command = this.getWritableDatabase();

        // store all data inside the object
        ContentValues command_content = new ContentValues();

        command_content.put(column_name, name);
        command_content.put(column_path, path);
        command_content.put(column_last_usage, lastUsage);
        command_content.put(column_num_usages, numUsages);
        command_content.put(column_total_usages, totalUsages);

        // insert values into the database
        db_command.insert(table_name, null, command_content);

        db_command.close();

    }

    public ArrayList<VoiceCommand> readData(){
        String query = "SELECT * FROM " +  table_name;
        SQLiteDatabase db_command = this.getReadableDatabase();
        Cursor cursorCommand = db_command.rawQuery(query, null);
        ArrayList<VoiceCommand> commandArray = new ArrayList<>();
        if (cursorCommand.moveToFirst())
        {
            do{
                commandArray.add(new VoiceCommand(
                        cursorCommand.getString(1),
                        cursorCommand.getString(2),
                        cursorCommand.getFloat(3),
                        cursorCommand.getInt(4),
                        cursorCommand.getFloat(5)));

            }while (cursorCommand.moveToNext());
        }
        cursorCommand.close();

        return commandArray;
    }

    // Source: https://www.geeksforgeeks.org/how-to-update-data-to-sqlite-database-in-android/?ref=rp

    public void updateAudio(String name, String newPath)
    {
        Cursor cursor = getCommand(name);
        cursor.moveToFirst(); // https://stackoverflow.com/questions/50525179/gdx-sqlite-android-database-cursorindexoutofboundsexception-index-1-requested

        VoiceCommand command = new VoiceCommand(cursor.getString(1),
                cursor.getString(2), cursor.getFloat(3), cursor.getInt(4), cursor.getFloat(5));

        // create a new database object
        SQLiteDatabase db_command = this.getWritableDatabase();

        // store all data inside the object
        ContentValues command_content = new ContentValues();

        command_content.put(column_name, name);
        command_content.put(column_path, newPath);
        command_content.put(column_last_usage, command.getLastUsage());
        command_content.put(column_num_usages, command.getNumUsages());
        command_content.put(column_total_usages, command.getTotalUsages());

        db_command.update(table_name, command_content, "name=?", new String[] {name});
        db_command.close();
    }

    public void updateUsage(String name, float usage)
    {
        Cursor cursor = getCommand(name);
        cursor.moveToFirst(); // https://stackoverflow.com/questions/50525179/gdx-sqlite-android-database-cursorindexoutofboundsexception-index-1-requested

        VoiceCommand command = new VoiceCommand(cursor.getString(1),
                cursor.getString(2), cursor.getFloat(3), cursor.getInt(4), cursor.getFloat(5));
        command.printCommand();
        command.addCurrentUsage(usage);
        command.printCommand();

        // create a new database object
        SQLiteDatabase db_command = this.getWritableDatabase();

        // store all data inside the object
        ContentValues command_content = new ContentValues();

        command_content.put(column_name, name);
        command_content.put(column_path, command.getPath());
        command_content.put(column_last_usage, command.getLastUsage());
        command_content.put(column_num_usages, command.getNumUsages());
        command_content.put(column_total_usages, command.getTotalUsages());

        db_command.update(table_name, command_content, "name=?", new String[] {name});
        db_command.close();
    }


    // specific queries
    public Cursor getCommand(String name)
    {
        String query = "SELECT * FROM "+ table_name + " WHERE " + column_name + " = \"" + name + "\"";
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(query, null);
    }

    public ArrayList<VoiceCommand> getTopRows(int N)
    {
        String query = "SELECT * FROM "+ table_name + " ORDER BY " + column_num_usages + " ASC LIMIT " + N;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursorCommand = db.rawQuery(query, null);
        ArrayList<VoiceCommand> commandArray = new ArrayList<>();
        if (cursorCommand.moveToFirst())
        {
            do{
                commandArray.add(new VoiceCommand(
                        cursorCommand.getString(1),
                        cursorCommand.getString(2),
                        cursorCommand.getFloat(3),
                        cursorCommand.getInt(4),
                        cursorCommand.getFloat(5)));

            }while (cursorCommand.moveToNext());
        }
        cursorCommand.close();
        return commandArray;
    }


    // clear database
    public void clearDB()
    {
        String query = "DELETE FROM " + table_name;
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }
}
