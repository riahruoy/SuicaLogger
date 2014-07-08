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

public class SuicaHistoryDB {
	static public final String DATE_PATTERN ="yyyy-MM-dd'T'HH:mm:ss";
	public static ArrayList<String> getCardIds(SQLiteDatabase db) {
		ArrayList<String> cardIds = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = db.query("card", null, null, null, null, null, "id desc");
			int indexCardId = cursor.getColumnIndex("card_id");
			while (cursor.moveToNext()) {
				cardIds.add(cursor.getString(indexCardId));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return cardIds;
	}
	public static ArrayList<History> readHistory(SQLiteDatabase db, Context context, String cardId) {
		ArrayList<History> histories = new ArrayList<History>();
		Cursor cursor = null;
		try {
			cursor = db.query("history",null, "card_id=?", new String[]{ cardId }, null, null, "history_no DESC", null);
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
	public static int addHistory (SQLiteDatabase db, ArrayList<History> histories, String cardId) {
		//if card is registered
		Cursor cursorCard = db.query("card", new String[]{"id"}, "card_id=?",new String[]{cardId},null, null, null, "1");
		boolean isRegistered = (cursorCard.getCount() > 0);
		cursorCard.close();
		if (!isRegistered) {
			ContentValues val = new ContentValues();
			val.put("card_id", cardId);
			val.put("name", cardId);
			db.insert("card",null, val);
		}
		
		
		
		//sort by historyNo ASC
		Collections.sort(histories, new Comparator<History>(){
			@Override
			public int compare(History h1, History h2) {
				return (h1.historyNo - h2.historyNo);
			}
		});

		int writeCount = 0;
		for (History history : histories) {
			String raw = Util.convertToString(history.data);
			Cursor cursor = db.query("history", new String[]{"id"}, "raw=? and card_id=?", new String[]{raw, cardId}, null, null, null, "1");
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
				val.put("card_id", cardId);
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
	public static class OpenHelper extends SQLiteOpenHelper {
		static final String DB = "history_sqlite";
		static final int DB_VERSION = 2;
		static final String CREATE_TABLE_CARD = "create table card ( id integer primary key autoincrement,card_id varchar(32) not null, name varchar(128))";
		static final String CREATE_TABLE_RECORD = "create table history"
				+"( id integer primary key autoincrement,"
				+ "card_id varchar(32),"
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
	        db.execSQL("create unique index rawindex on history(raw);");
	        db.execSQL("create index cardindex on history(card_id);");
	    }
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	        db.execSQL(DROP_TABLE_CARD);
	        db.execSQL(DROP_TABLE_RECORD);
	        onCreate(db);
	    }
	}
}
