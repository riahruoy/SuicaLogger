package com.fuyo.suicalogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PrefActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();

    }
 

	
	public static class PrefsFragment extends PreferenceFragment{
		@Override
		public void onCreate(Bundle savedInstanceState){
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref);
            PreferenceScreen screen = (PreferenceScreen)findPreference("key_import");
            screen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Context context = getActivity();
                    final String DIR = "suicalogger/import";
                    final String path = Environment.getExternalStorageDirectory().getPath()
                            + "/" + DIR;
                    AlertDialog.Builder reallyToImport = new AlertDialog.Builder(context);
                    reallyToImport.setTitle("Confirm")
                            .setMessage(path + " のデータを読み取ってよいですか？")
                            .setNegativeButton("cancel",new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                            })
                            .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File dir = new File(path);
                                    if (!dir.exists()) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                        builder.setTitle("Error")
                                                .setMessage(path + " not found")
                                                .create()
                                                .show();
                                        return;
                                    }
                                    int importCount = 0;
                                    final HistoryDataBase db = new HistoryDataBase(context);
                                    Map<String, HistoryDataBase.CardHistoryFile> ICs = db.getICs();
                                    for (File file : dir.listFiles()) {
                                        String filename = file.getName();
                                        if (filename.endsWith(".txt")) {
                                            final String cardId = filename.replace(".txt", "");
                                            boolean found = false;
                                            final String filePath = path + "/" + filename;
                                            for (Map.Entry<String, HistoryDataBase.CardHistoryFile> entry : ICs.entrySet()) {
                                                if (entry.getKey().equals(cardId)) {
                                                    found = true;
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                    builder.setTitle("上書き？")
                                                            .setMessage("既にデータがあります。データを追加してよいですか？card id: "+cardId)
                                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    db.loadFromBackup(cardId, filePath);
                                                                    Toast.makeText(context, "1 card loaded", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .setNegativeButton("Cacnel", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                                }
                                                            })
                                                            .create()
                                                            .show();
                                                    break;
                                                }
                                            }
                                            if (!found) {
                                                db.loadFromBackup(cardId, filePath);
                                                importCount++;
                                            }
                                        }
                                    }
                                    if (importCount > 0)
                                        Toast.makeText(context, "" + importCount + " cards loaded", Toast.LENGTH_SHORT).show();

                                }
                            })
                            .create()
                            .show();
                    return true;
                }
            });
		}
	}
}