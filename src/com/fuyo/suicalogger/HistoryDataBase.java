package com.fuyo.suicalogger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.fuyo.suicalogger.Suica.History;
import com.fuyo.suicalogger.SuicaHistoryDB.OpenHelper;

public class HistoryDataBase {
    	static public final String DATE_PATTERN ="yyyy-MM-dd'T'HH:mm:ss";
    	//the first line of log file should be start with LOG_HEADER + LOG_VERSION 
    	static public final String LOG_HEADER = "# LOG_VERSION";
    	static public final String LOG_VERSION = "3";
    	static public final int BLOCK_SIZE = 16;
    	public final int MAX_LOG = 20480;
    	private static final String DIR = "./";
    	private static final String LOG_PREFIX = "suicaLog-";
    	private CardHistoryFile mCurrentCardHistory;
    	private Map<String, CardHistoryFile>mDataMap;
    	private Context mContext;
    	private SharedPreferences sharedPref;
    	public HistoryDataBase(Context context) {
    		mContext = context;
			mDataMap = new HashMap<String, CardHistoryFile>();
    		sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			reloadLogFile();
    	}
    	public void reloadLogFile() {
    		mDataMap = new HashMap<String, CardHistoryFile>();
    		ArrayList<String> idList;
    		if (!sharedPref.contains("prevVersionCode")) {
    			idList = getIdsFromLogFile();
    		} else {
    			idList = getCardIds();
    		}
			mCurrentCardHistory = null;
			for (String id : idList) {
				CardHistoryFile hf = new CardHistoryFile(id);
				mDataMap.put(id, hf);
				mCurrentCardHistory = hf;
				//TODO mCurrentData
			}
    	}
    	public void setCurrentCard(String cardId) {
    		mCurrentCardHistory = mDataMap.get(cardId);
    	}
    	private ArrayList<String> getCardIds() {
    		SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(mContext);
    		SQLiteDatabase db = helper.getReadableDatabase();
    		return SuicaHistoryDB.getCardIds(db);
    	}
    	private ArrayList<String> getIdsFromLogFile() {
    		ArrayList<String> list = new ArrayList<String>();
    		File dir = mContext.getFilesDir();
    		File[] files = dir.listFiles();
    		for (int i = 0; i < files.length; i++) {
    			if (files[i].getName().startsWith(LOG_PREFIX)) {
    				String stringId = files[i].getName().substring(LOG_PREFIX.length());
    				list.add(stringId);
    			}
    		}
    		return list;
    	}
    	public ArrayList<History> getCurrentData() {
    		if (mCurrentCardHistory == null)return new ArrayList<History>();
    		return mCurrentCardHistory.getData();
    	}
     	public int writeHistory(ArrayList<History> data, String id) {
    		if (!mDataMap.containsKey(id)) {
    			CardHistoryFile hf = new CardHistoryFile(id);
    			mCurrentCardHistory = hf;
    			mDataMap.put(id, hf);
    		}
    		return mDataMap.get(id).writeHistory(data);
    	}
    	public Map<String, CardHistoryFile> getICs() {
    		return mDataMap;
    	}
    	class CardHistoryFile {
    		private String mFilename;
    		private String mId;
    		private String mLastWrite;
    		private ArrayList<History> mData;
    		public CardHistoryFile (String _id) {
    			mId = _id;
    			mFilename = LOG_PREFIX + mId;
    			mLastWrite = "Never";
    			mData = readHistoryFromFile();
    		}
    		public String getLastWrite() {
    			return mLastWrite;
    		}
    		private ArrayList<History> readHistoryFromFile_v1() {

        		ArrayList<History> history = new ArrayList<History>();
        		try {
        			FileInputStream fis = mContext.openFileInput(mFilename);
    				BufferedInputStream bis = new BufferedInputStream(fis);
    				byte[] buffer = new byte[MAX_LOG];
    				int readCount = bis.read(buffer,0,MAX_LOG);
    				int historyCount = readCount / BLOCK_SIZE;
    				for (int i = 0; i < historyCount; i++) {
    					byte[] singleLog = new byte[BLOCK_SIZE];
    					System.arraycopy(buffer, i * BLOCK_SIZE, singleLog, 0, BLOCK_SIZE);
    					history.add(new History(singleLog, mContext));
    				}
    			} catch (FileNotFoundException e) {

    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		return history;
    			
    		}
/*    		public ArrayList<History> rebuildCacheFromFile() {
        		BufferedReader br = null;
        		ArrayList<History> history = new ArrayList<History>();
        		try {
        			FileInputStream fis = mContext.openFileInput(mFilename);
        			
        			br = new BufferedReader(new InputStreamReader(fis));
        			int readCount = 0;
        			while (br.ready()) {
        				String line = br.readLine();
        				if (readCount++ == 0) {
        					if (line.startsWith(LOG_HEADER)) {
        						//LOG_VERSION > 2
        						String[] header = line.split("\t");
        						mLastWrite = header[2];
        						continue;
        					} else {
        						return readHistoryFromFile_v1();
        					}
        				}
        				String[] columns = line.split("\t");
        				if(columns.length != 8) {
        					continue;
        				}
        				history.add(new History(
								Util.convertToByte(columns[0]),mContext
								));
        			}
        			br.close();
        			fis.close();
    			} catch (FileNotFoundException e) {

    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		mData = new ArrayList<History>();
        		writeHistory(history);
        		mData = history;
        		return history;
    			
    		}
*/
    		private ArrayList<History> readHistoryFromFile_v2() {
        		BufferedReader br = null;
        		ArrayList<History> history = new ArrayList<History>();
        		try {
        			FileInputStream fis = mContext.openFileInput(mFilename);
        			
        			br = new BufferedReader(new InputStreamReader(fis));
        			int readCount = 0;
        			while (br.ready()) {
        				String line = br.readLine();
        				if (readCount++ == 0) {
        					if (line.startsWith(LOG_HEADER)) {
        						//LOG_VERSION > 2
        						String[] header = line.split("\t");
        						mLastWrite = header[2];
        						continue;
        					} else {
        						return readHistoryFromFile_v1();
        					}
        				}
        				String[] columns = line.split("\t");
        				if(columns.length != 8) {
        					continue;
        				}
        				try {
							history.add(new History(
									Util.convertToByte(columns[0]),
									columns[1], columns[2],
									(new SimpleDateFormat(DATE_PATTERN)).parse(columns[3]),
									columns[4].split(":"),
									columns[5].split(":"),
									Long.valueOf(columns[6]),
									Integer.valueOf(columns[7]),
									"",
									mContext
									));
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        			}
        			br.close();
        			fis.close();
    			} catch (FileNotFoundException e) {
    				e.printStackTrace();

    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		return history;
    			
    		}
    		private ArrayList<History> readHistoryFromFile_v3() {
        		BufferedReader br = null;
        		ArrayList<History> history = new ArrayList<History>();
        		try {
        			FileInputStream fis = mContext.openFileInput(mFilename);
        			
        			br = new BufferedReader(new InputStreamReader(fis));
        			int readCount = 0;
        			while (br.ready()) {
        				String line = br.readLine();
        				if (readCount++ == 0) {
        					if (line.startsWith(LOG_HEADER)) {
        						//LOG_VERSION > 2
        						String[] header = line.split("\t");
        						mLastWrite = header[2];
        						continue;
        					} else {
        						return readHistoryFromFile_v1();
        					}
        				}
        				String[] columns = line.split("\t");
        				if(columns.length != 9) {
        					continue;
        				}
        				for (int i = 0; i < columns.length; i++) {
        					columns[i] = columns[i].substring(1, columns[i].length()-1);
        				}
        				try {
							history.add(new History(
									Util.convertToByte(columns[0]),
									columns[1], columns[2],
									(new SimpleDateFormat(DATE_PATTERN)).parse(columns[3]),
									columns[4].split(":"),
									columns[5].split(":"),
									Long.valueOf(columns[6]),
									Integer.valueOf(columns[7]),
									columns[8],
									mContext
									));
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        			}
        			br.close();
        			fis.close();
        			for (int i = 0; i < history.size() - 1; i++) {
        				history.get(i).fee = (int)(history.get(i).balance - history.get(i + 1).balance);
        			}
    			} catch (FileNotFoundException e) {

    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		return history;
    			
    		}
    		private ArrayList<History> readHistoryFromSQL() {
        		SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(mContext);
        		SQLiteDatabase db = helper.getReadableDatabase();
        		ArrayList<History> histories = SuicaHistoryDB.readHistory(db, mContext, mId);
        		db.close();
        		return histories;
    		}
        	private ArrayList<History> readHistoryFromFile() {
        		if (!sharedPref.contains("prevVersionCode")) {
        		//version <= 14 is file, version > 14 is sql
	        		try {
	                    
	                    
	            		BufferedReader br = null;
	            		int version = 1;
	        			FileInputStream fis = mContext.openFileInput(mFilename);
	        			
	        			br = new BufferedReader(new InputStreamReader(fis));
	        			int readCount = 0;
	       				String line = br.readLine();
	       				if (readCount++ == 0) {
	       					if (line.startsWith(LOG_HEADER)) {
	       						//LOG_VERSION > 2
	       						String[] header = line.split("\t");
	       						version = Integer.parseInt(header[1]);
	       					}
	       				}
	        			br.close();
	        			fis.close();
	        			
	        			ArrayList<History> history = new ArrayList<History>();
	        			if (version == 1) {
	        				history = readHistoryFromFile_v1();
	        			} else if (version == 2) {
	        				history = readHistoryFromFile_v2();
	        			} else if (version == 3) { 
	        				history = readHistoryFromFile_v3();
	        			}
	        			writeHistory(history);
	        			return history;
	    			} catch (FileNotFoundException e) {
	    				return new ArrayList<History>();
	    			} catch (IOException e) {
	    				// TODO Auto-generated catch block
	    				e.printStackTrace();
	    				return new ArrayList<History>();
	    			}
                } else {
                	//sqlite
                	return readHistoryFromSQL();
                }
        	}
        	public void deleteHistory() {
        		SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(mContext);
        		SQLiteDatabase db = helper.getWritableDatabase();
        		db.delete("history", "card_id=?", new String[]{mId});
        		db.delete("card", "card_id=?", new String[]{mId});
        		
        	}
        	public int writeHistory(ArrayList<History> writeHistory) {
        		PackageManager pm = mContext.getPackageManager();
                int versionCode = 0;
                try{
                    PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
                    versionCode = packageInfo.versionCode;
                }catch(NameNotFoundException e){
                    e.printStackTrace();
                }
                Editor e = sharedPref.edit();
                e.putInt("prevVersionCode", versionCode);
                e.commit();

        		int writeCount = 0;
        		SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(mContext);
        		SQLiteDatabase db = helper.getWritableDatabase();
        		writeCount = SuicaHistoryDB.addHistory(db, writeHistory, mId);
        		db.close();
        		mData = SuicaHistoryDB.readHistory(helper.getReadableDatabase(), mContext, mId);
        		return writeCount;
        	}
        	private int writeHistory_v3(ArrayList<History> writeHistory) {
        		ArrayList<History> historyToBeAdded = new ArrayList<History>(); 
        		for (History singleWriteLog : writeHistory) {
        			boolean found = false;
        			for (History singleLog : mData) {
        				if (singleLog.historyNo == singleWriteLog.historyNo) {
        					found = true;
        					break;
        				}
        			}
        			if (!found) {
        				historyToBeAdded.add(singleWriteLog);
        			}
        		}

        		int writeCount = historyToBeAdded.size();
        		// writeHistory is assumed to be newer than mData;
        		for (History singleLog : mData) {
        			historyToBeAdded.add(singleLog);
        		}
        		mData = historyToBeAdded;

        		try {
    	    		mContext.deleteFile(mFilename);
    				FileOutputStream fos =  mContext.openFileOutput(mFilename, Context.MODE_APPEND);
    				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
/*    		        public History(byte[] data, String consoleType,
    		        		String processType, Date processDate, String[] entranceStation,
    		        		String[] exitStation, long balance, int historyNo) {
*/
    				bw.write(LOG_HEADER);
    				bw.write("\t");
    				bw.write(LOG_VERSION);
    				bw.write("\t");
    				bw.write((new SimpleDateFormat(DATE_PATTERN)).format(new Date()));
    				mLastWrite = (new SimpleDateFormat(DATE_PATTERN)).format(new Date());
    				bw.write("\n");
    				
    				for (History singleWriteLog : mData) {
    					String[] writeLine = new String[] {
    						Util.convertToString(singleWriteLog.data),
    						singleWriteLog.consoleType,
    						singleWriteLog.processType,
    						(new SimpleDateFormat(DATE_PATTERN)).format(singleWriteLog.processDate),
    						join(singleWriteLog.entranceStation, ':'),
    						join(singleWriteLog.exitStation, ':'),
    						Long.toString(singleWriteLog.balance),
    						Integer.toString(singleWriteLog.historyNo),
    						singleWriteLog.note
    					};
    					for (int i = 0; i < writeLine.length ; i++) {
    						writeLine[i] = "\"" + writeLine[i] + "\"";
    					}
    					bw.write(join(writeLine,'\t'));
    					bw.write("\n");
    				}
    				bw.close();
    				fos.close();


    			} catch (FileNotFoundException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		
        		return writeCount;
        	}
        	public ArrayList<History> getData() {
        		return mData;
        	}
        	public void backupLogFile(String dstFilePath) throws IOException {
	      		File dstFile = new File(dstFilePath);
	      		  // ディレクトリを作る.
	      		dstFile.getParentFile().mkdirs();
				FileOutputStream fos =  new FileOutputStream(dstFilePath);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
/*    		        public History(byte[] data, String consoleType,
		        		String processType, Date processDate, String[] entranceStation,
		        		String[] exitStation, long balance, int historyNo) {
*/
				bw.write(LOG_HEADER);
				bw.write("\t");
				bw.write(LOG_VERSION);
				bw.write("\t");
				bw.write((new SimpleDateFormat(DATE_PATTERN)).format(new Date()));
				mLastWrite = (new SimpleDateFormat(DATE_PATTERN)).format(new Date());
				bw.write("\n");
				
				for (History singleWriteLog : mData) {
					String[] writeLine = new String[] {
						Util.convertToString(singleWriteLog.data),
						singleWriteLog.consoleType,
						singleWriteLog.processType,
						(new SimpleDateFormat(DATE_PATTERN)).format(singleWriteLog.processDate),
						join(singleWriteLog.entranceStation, ':'),
						join(singleWriteLog.exitStation, ':'),
						Long.toString(singleWriteLog.balance),
						Integer.toString(singleWriteLog.historyNo),
						singleWriteLog.note,
						Integer.toString(singleWriteLog.fee)
					};
					for (int i = 0; i < writeLine.length ; i++) {
						writeLine[i] = "\"" + writeLine[i] + "\"";
					}
					bw.write(join(writeLine,'\t'));
					bw.write("\n");
				}
				bw.close();
				fos.close();

	      		 
	      		 
        	}
        	public void loadFromBackup(String src) throws IOException {
        		BufferedReader br = null;
        		ArrayList<History> history = new ArrayList<History>();
        		try {
    				FileInputStream fis =  new FileInputStream(src);

        			
        			br = new BufferedReader(new InputStreamReader(fis));
        			int readCount = 0;
        			while (br.ready()) {
        				String line = br.readLine();
        				if (readCount++ == 0) {
        					if (line.startsWith(LOG_HEADER)) {
        						//LOG_VERSION > 2
        						String[] header = line.split("\t");
        						mLastWrite = header[2];
        						continue;
        					} else {
        						continue;
//        						return readHistoryFromFile_v1();
        					}
        				}
        				String[] columns = line.split("\t");
        				if(columns.length != 9) {
        					continue;
        				}
        				for (int i = 0; i < columns.length; i++) {
        					columns[i] = columns[i].substring(1, columns[i].length()-1);
        				}
        				try {
							history.add(new History(
									Util.convertToByte(columns[0]),
									columns[1], columns[2],
									(new SimpleDateFormat(DATE_PATTERN)).parse(columns[3]),
									columns[4].split(":"),
									columns[5].split(":"),
									Long.valueOf(columns[6]),
									Integer.valueOf(columns[7]),
									columns[8],
									mContext
									));
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        			}
        			br.close();
        			fis.close();
        			for (int i = 0; i < history.size() - 1; i++) {
        				history.get(i).fee = (int)(history.get(i).balance - history.get(i + 1).balance);
        			}
    			} catch (FileNotFoundException e) {

    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		mData = history;


        	}
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
    	//    	public static HistoryDataBase getRecentHistoryId(Context context) {
//    		File dir = new File(DIR);
//    		File[] files = dir.listFiles();
//    		String latestId = "";
//    		byte[] latestByteId = null;
//    		for (int i = 0; i < files.length; i++) {
//    			if (files[i].getName().startsWith(LOG_PREFIX)) {
//    				String stringId = files[i].getName().substring(LOG_PREFIX.length());
////    				list.add(stringId);
//    			}
//    		}
//    		return new HistoryDataBase(context, latestId);
//    		
//    	}
    }