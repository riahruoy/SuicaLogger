package com.fuyo.suicalogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.fuyo.suicalogger.Suica.History;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SuicaHisotryDB {
	static public final String DATE_PATTERN ="yyyy-MM-dd'T'HH:mm:ss";
	public static ArrayList<History> readHistory(SQLiteDatabase db, Context context) {
		ArrayList<History> histories = new ArrayList<History>();
		Cursor cursor = null;
		try {
			cursor = db.query("history",null, null, null, null, null, null, null);
			int indexRaw = cursor.getColumnIndex("raw");
			int indexConsoleType = cursor.getColumnIndex("console_type");
			int indexProcessType = cursor.getColumnIndex("process_type");
			int indexProcessDate = cursor.getColumnIndex("process_date");
			int indexEntrance = cursor.getColumnIndex("entrance_station");
			int indexExit = cursor.getColumnIndex("exit_station");
			int indexFee = cursor.getColumnIndex("fee");
			int indexBalance = cursor.getColumnIndex("balance");
			int indexHistoryNo = cursor.getColumnIndex("history_no");
			int indexNote = cursor.getColumnIndex("note");
			while (cursor.moveToNext()) {
				History history = new History(
						Util.convertToByte(cursor.getString(indexRaw)),
						cursor.getString(indexConsoleType),
						cursor.getString(indexProcessType),
						(new SimpleDateFormat(DATE_PATTERN)).parse(cursor.getString(indexProcessDate)),
						cursor.getString(indexEntrance).split(":"),
						cursor.getString(indexExit).split(":"),
						(long)cursor.getInt(indexBalance),
						cursor.getInt(indexHistoryNo),
						cursor.getString(indexNote),
						context
						);
				history.fee = cursor.getInt(indexFee);
				histories.add(history);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		return histories;
	}
	public static int addHistory (SQLiteDatabase db, ArrayList<History> histories) {
		//sort by historyNo ASC
		Collections.sort(histories, new Comparator<History>(){
			@Override
			public int compare(History h1, History h2) {
				return (h1.historyNo - h2.historyNo);
			}
		});

		int writeCount = 0;
		for (History history : histories) {
			Cursor cursor = db.query("history", new String[]{"id"}, "raw=?", new String[]{""}, null, null, null, "1");
			int count = cursor.getCount();
			cursor.close();
			if (count == 0) {
				//add
				ContentValues val = new ContentValues();
				val.put("raw", Util.convertToString(history.data));
				val.put("console_type", history.consoleType);
				val.put("process_type", history.processType);
				val.put("process_date", (new SimpleDateFormat(DATE_PATTERN)).format(history.processDate));
				val.put("entrance_station", join(history.entranceStation, ':'));
				val.put("exit_station", join(history.exitStation, ':'));
				val.put("balance", Long.toString(history.balance));
				val.put("fee", history.fee);
				val.put("history_no", history.historyNo);
				val.put("note", history.note);
				db.insert("history", null, val);
				writeCount++;
			}
		}
		return writeCount;
	}
	public static String join(String[] tokens, char delimiter) {
	    if (tokens == null) {
	        return "";
	    }
	    StringBuffer ret = new StringBuffer();
	    for (int i = 0; i < tokens.length; i++) {
	        if (i > 0) {
	            ret.append(delimiter);
	        }
	        ret.append(tokens[i]);
	    }
	    return ret.toString();
	}
	private static class OpenHelper extends SQLiteOpenHelper {
		static final String DB = "history_sqlite";
		static final int DB_VERSION = 1;
		static final String CREATE_TABLE_CARD = "create table card ( id integer primary key autoincrement, name varchar(128))";
		static final String CREATE_TABLE_RECORD = "create table history"
				+"( id integer primary key autoincrement,"
				+ "raw varchar(32),"
				+ "console_type varchar(32),"
				+ "process_type varchar(32),"
				+ "process_date datetime,"
				+ "entrance_station varchar(128),"
				+ "exit_station varchar(128),"
				+ "fee integer,"
				+ "balance integer,"
				+ "history_no integer,"
				+ "note varchar(255));";
		static final String DROP_TABLE_CARD = "drop table card;";
		static final String DROP_TABLE_RECORD = "drop table history;";
	    public OpenHelper(Context c) {
	        super(c, DB, null, DB_VERSION);
	    }
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(CREATE_TABLE_CARD);
	        db.execSQL(CREATE_TABLE_RECORD);
	    }
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	        db.execSQL(DROP_TABLE_CARD);
	        db.execSQL(DROP_TABLE_RECORD);
	        onCreate(db);
	    }
	}
}
