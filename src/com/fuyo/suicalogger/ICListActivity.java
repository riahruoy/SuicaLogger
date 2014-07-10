package com.fuyo.suicalogger;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fuyo.suicalogger.Suica.History;
import com.fuyo.suicalogger.SuicaHistoryDB.OpenHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ICListActivity extends ListActivity {
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private final static int END_CODE = 1;
	private final static int MODIFY_CODE = 2;
	String[][] mTechLists;
	private ListView mListView;
	private List<Map<String, String>> mListDataSet;
	private HistoryDataBase mDb;
	private SimpleAdapter mSimpleAdapter;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ic_list_view);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        mFilters = new IntentFilter[] {tech};
        mTechLists = new String[][] {new String[] { NfcF.class.getName() }};

        
        mListView = this.getListView();
        mListDataSet = new ArrayList<Map<String, String>>();
    	mDb = new HistoryDataBase(this);
        mSimpleAdapter = new SimpleAdapter(
        		this,
        		mListDataSet,
        		android.R.layout.simple_list_item_2,
        		new String[] {"title", "comment"},
        		new int[] {android.R.id.text1, android.R.id.text2} );
        mListView.setAdapter(mSimpleAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				// TODO Auto-generated method stub
				Map<String, String> clicked = (Map<String, String>)mSimpleAdapter.getItem(position);
				String[] tmp = clicked.get("comment").split(" ");
				String cardId = tmp[0];
				createIntent(cardId);
			}
			
		});
        registerForContextMenu(mListView);
        if (mListDataSet.size() == 1) {
        	String[] tmp = mListDataSet.get(0).get("comment").split(" ");
			String cardId = tmp[0];
			createIntent(cardId);
        }
        if (mDb.getICs().size() == 0) {
	        if (mAdapter == null) {
	        	new AlertDialog.Builder(this)
	        	.setTitle("エラー")
	        	.setMessage("NFCデバイスがありません\n交通ICカードを読み取ることができません")
	        	.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				})
	        	.show();
	        } else {
	        	if (!mAdapter.isEnabled()) {
	            	new AlertDialog.Builder(this)
	            	.setTitle("エラー")
	            	.setMessage("NFCデバイスがOFFになっています\nシステムの設定からNFCをONにしてください\n(設定ではNFC R/W P2Pとなっているかも？）")
	            	.setPositiveButton("OK", new OnClickListener() {
	    				@Override
	    				public void onClick(DialogInterface dialog, int which) {
	    					startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
	    				}
	    			})
	            	.show();
	        		
	        	}
	        }
        }
    	setListDataSet();

    }
	private void createIntent(String cardId) {
		Intent intent = new Intent(ICListActivity.this, SuicaLogViewActivity.class);
		intent.putExtra("cardId", cardId);
		startActivity(intent);
	}
    @Override
    public void onResume() {
    	mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    	mDb.reloadLogFile();  //in case that new suica was loaded at another activity
    	mSimpleAdapter.notifyDataSetChanged();
    	mListView.invalidateViews();
    	super.onResume();
    }
    private void setListDataSet() {
        mListDataSet.clear();
        mDb.reloadLogFile();
        Map<String, HistoryDataBase.CardHistoryFile> ICs = mDb.getICs();
        SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        int i = 0;
        for (Map.Entry<String, HistoryDataBase.CardHistoryFile> entry : ICs.entrySet()) {
        	Cursor c = db.query("card", new String[]{"name"}, "card_id like ?", new String[]{entry.getKey()}, null, null, null, "1");
        	int indexName = c.getColumnIndex("name");
        	c.moveToFirst();
        	String name = c.getString(indexName);
        	c.close();
        	i++;
			Map<String, String> singleHistory = new HashMap<String, String>();
       		singleHistory.put("comment", entry.getKey());
			singleHistory.put("title", name);
			mListDataSet.add(singleHistory);
		}
        mSimpleAdapter.notifyDataSetChanged();

    }
    
    @Override
    public void onPause() {
    	if (this.isFinishing()) {
    		mAdapter.disableForegroundDispatch(this);
    	}
    	super.onPause();
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    	String action = intent.getAction();
    	if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
    		AsyncTask<Intent, Void, Integer> task = new AsyncTask<Intent, Void, Integer>() {
    			ProgressDialog dialog;
    			String cardId;
    			@Override
    			protected void onPreExecute() {
    	        	dialog = new ProgressDialog(ICListActivity.this);
    	        	dialog.setMessage("読み取り中...");
    	        	dialog.show();
    			}
				@Override
				protected Integer doInBackground(Intent... params) {
					try {
						Intent intent = params[0];
			        	Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			        	byte[] byte_id = tag.getId();
			        	cardId = Util.getHexString(byte_id);
		    	    	Suica suica = new Suica(tag, ICListActivity.this);
		    	    	ArrayList<Suica.History> dataFromCard;
							dataFromCard = suica.readSuica();
		    			int writeCount = mDb.writeHistory(dataFromCard, Util.convertToString(tag.getId()));
					
		    	        boolean flagAutobackup = PreferenceManager.getDefaultSharedPreferences(ICListActivity.this).getBoolean("key_autobackup", false);
		    	    	if (flagAutobackup) {
		    	    		backupToExternalStorage(cardId);
		    	    	}
		    	    	return Integer.valueOf(writeCount);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return Integer.valueOf(0);
					}
				}
    			
				@Override
				protected void onPostExecute(Integer result) {
			    	setListDataSet();
			    	mSimpleAdapter.notifyDataSetChanged();
	    	    	mListView.invalidateViews();
				    dialog.dismiss();
	    			Toast.makeText(ICListActivity.this, "履歴を" + String.valueOf(result)+"件追加しました", Toast.LENGTH_SHORT).show();
	    			setListDataSet();
	    			ICListActivity.this.createIntent(cardId);
				}
    		};
    		task.execute(intent);
        }
    }
    private void backupToExternalStorage(String cardId) {
    	final String dir = "suicalogger";
//    	Date date = new Date();
//    	// 表示形式を設定
//    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss");
    	String path = Environment.getExternalStorageDirectory().getPath()
    			+ "/" + dir + "/";
//    	String filePath = path + sdf.format(date) + "_";
    	String filePath = path + cardId + ".txt";
    	
    	try {
			mDb.getICs().get(cardId).backupLogFile(filePath);
			if(Build.VERSION.SDK_INT <= 18){
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
            		Uri.parse("file://" + Environment.getExternalStorageDirectory())));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //コンテキストメニュー
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
      super.onCreateContextMenu(menu, view, menuInfo);
      AdapterContextMenuInfo adapterinfo = (AdapterContextMenuInfo)menuInfo;
      ListView listView = (ListView)view;
 
      menu.setHeaderTitle(((Map<String, String>)listView.getItemAtPosition(adapterinfo.position)).get("title"));
      menu.add(0, END_CODE, 0, "削除");
      menu.add(0, MODIFY_CODE, 1, "名称を編集");
    }
    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    	String[] comment = mListDataSet.get(info.position).get("comment").split(" ");
    	final String cardId = comment[0];
    	SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(this);
    	SQLiteDatabase db = helper.getReadableDatabase();
    	Cursor c = db.query("card", new String[]{"name"}, "card_id like ?", new String[]{cardId}, null, null, null, "1");
    	int indexName = c.getColumnIndex("name");
    	c.moveToFirst();
    	String name = c.getString(indexName);
    	c.close();
    	db.close();

    	
        switch(item.getItemId()){
      //削除
        case END_CODE:
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle(name + " を削除していいですか？");
        	DialogInterface.OnClickListener doc = new DialogInterface.OnClickListener() {
				private AdapterContextMenuInfo info;
				public DialogInterface.OnClickListener setParam(AdapterContextMenuInfo i) {
					info = i;
					return this;
				}
				@Override
				public void onClick(DialogInterface dialog, int which) {
		        	String[] comment = mListDataSet.get(info.position).get("comment").split(" ");
		        	String cardId = comment[0];
		        	mDb.getICs().get(cardId).deleteHistory();
		        	mDb.reloadLogFile();
		        	setListDataSet();
		        	mSimpleAdapter.notifyDataSetChanged();
		        	mListView.invalidateViews();
				}
        	}.setParam(info);
        	builder.setPositiveButton("削除", doc);
        	builder.setNegativeButton("やめる", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			});
        	builder.create().show();
          return true;
        case MODIFY_CODE:
        	AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        	final EditText editText = new EditText(this);
        	editText.setText(name);
        	builder2.setTitle("カード名称の編集");
        	builder2.setView(editText);
        	builder2.setPositiveButton("OK", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(ICListActivity.this);
					SQLiteDatabase db = helper.getWritableDatabase();
					ContentValues val = new ContentValues();
					val.put("name", editText.getText().toString());
					db.update("card", val, "card_id like ?", new String[]{cardId});
					db.close();
					helper.close();
					setListDataSet();
				}
			});
			builder2.setNegativeButton("やめる", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			builder2.create().show();

        	return true;
        default:
          return super.onContextItemSelected(item);
        }
      }
}
