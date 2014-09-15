package com.fuyo.suicalogger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fuyo.suicalogger.Suica.History;
import com.fuyo.suicalogger.SuicaHistoryDB.OpenHelper;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Files;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SuicaLogViewActivity extends Activity {
	private static final int MENU_ID_MENU1 = Menu.FIRST;
	private static final int MENU_ID_MENU2 = Menu.FIRST+1;
	private static final int MENU_ID_MENU3 = Menu.FIRST+2;

	private final static int END_CODE = 1;
	private final static int RELOAD_CODE = 2;
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private ListView mListView;
	private String mCardId;
	private SuicaLogAdapter mSuicaLogAdapter;
//	private SimpleAdapter mSimpleAdapter;
	String[][] mTechLists;
	private HistoryDataBase mDb;
	private SharedPreferences sharedPref;
	private AdView adView;
	static final String MY_AD_UNIT_ID = "ca-app-pub-1661412607542997/2649568063";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        whatsNewDialogInit();
		Locale.setDefault(Locale.JAPAN);	//for currency JPY
        setContentView(R.layout.activity_suica_history_view);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        mFilters = new IntentFilter[] {tech};
        mTechLists = new String[][] {new String[] { NfcF.class.getName() }};

        Intent intent = getIntent();
        mCardId = (String)intent.getExtras().get("cardId");
        String cardId = mCardId;
        long start = System.currentTimeMillis();
    	HistoryDataBase db = mDb = new HistoryDataBase(this);
        long end = System.currentTimeMillis();
        Log.d(getClass().getName(), "measure_new Suica.HistoryDataBase: " + (end - start));
    	db.setCurrentCard(cardId);
        ListView historyListView = (ListView)findViewById(R.id.history_list_view);
        mListView = historyListView;
        start = System.currentTimeMillis();

    	end = System.currentTimeMillis();
        Log.d(getClass().getName(), "measure_setCurrentCard: " + (end - start));
        mSuicaLogAdapter = new SuicaLogAdapter();
//        mSimpleAdapter = new SimpleAdapter(
//        		this,
//        		mListDataSet,
//        		android.R.layout.simple_list_item_2,
//        		new String[] {"title", "comment"},
//        		new int[] {android.R.id.text1, android.R.id.text2} );
        historyListView.setAdapter(mSuicaLogAdapter);
        registerForContextMenu(historyListView);

        
        Button searchButton = (Button)findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SuicaLogViewActivity.this, LogSearchActivity.class);
				intent.putExtra("cardId", mCardId);
				startActivity(intent);
			}
		});
        
        
        
        
        adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
        LinearLayout layout = (LinearLayout)findViewById(R.id.mainLayout);
        LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(
        		TableLayout.LayoutParams.MATCH_PARENT,
        		TableLayout.LayoutParams.WRAP_CONTENT);
        param1.gravity = Gravity.BOTTOM;
        layout.addView(adView, param1);
        AdRequest req = new AdRequest();
//        req.addTestDevice("TEST_DEVICE_ID"); 
        adView.loadAd(req);
        
        //show how to read ic card activity
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("key_show_help_howtoread", true)) {
        	showHelp();
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int prevVersion = sharedPreferences.getInt("version", 1);
        //StationCode.db is updated at versionCode = 10
        if (prevVersion < 10) {
            DBUtil util = new DBUtil(this);
            try {
				util.copyDataBase();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            util.close();
            PackageManager packageManager = this.getPackageManager();
            int versionCode = 1;
            try {
                   PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
                   versionCode = packageInfo.versionCode;
              } catch (NameNotFoundException e) {
                   e.printStackTrace();
              }
                 Editor e = sharedPreferences.edit();
            e.putInt("version", versionCode);
            e.commit();
            Toast.makeText(this, "データベースを更新しました", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onResume() {
    	mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    	super.onResume();
    }
    
    @Override
    public void onPause() {
    	if (this.isFinishing()) {
    		mAdapter.disableForegroundDispatch(this);
    	}
    	super.onPause();
    }
    public void onDestory() {
    	adView.destroy();
    	super.onDestroy();
    }
    
    private void showHelp() {
		Intent intent = new Intent(this, HelpHowToReadNfcActivity.class);
		startActivity(intent);
    }
    private void whatsNewDialogInit() {
    	if (!sharedPref.contains("key_contribute")) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("データ解析のご協力")
    		.setMessage("Suicaのデータ解析にご協力をお願いします。\n"
    				+ "個人を特定するデータは送信されません。\n"
    				+ "メモに手動で入力した場合にメモ内容とSuica決済店舗のIDが送信されます。\n"
    				+ "ちなみにメモは、物販の項目を長押しで付けれます\n"
   				+ "将来的には決済店舗を自動で表示する機能がつけれればよいと思っています。")
    		.setPositiveButton("協力する(^o^)", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor e = sharedPref.edit();
					e.putBoolean("key_contribute", true);
					e.commit();
				}
			})
			.setNegativeButton("やめておく(TдT)", new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor e = sharedPref.edit();
					e.putBoolean("key_contribute", false);
					e.commit();
				}
			}).create().show();
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
    
    @Override
    public void onNewIntent(Intent intent) {
    	String action = intent.getAction();
    	if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
    		AsyncTask<Intent, Void, Integer> task = new AsyncTask<Intent, Void, Integer>() {
    			ProgressDialog dialog;
    			String cardId;
    			@Override
    			protected void onPreExecute() {
    	        	dialog = new ProgressDialog(SuicaLogViewActivity.this);
    	        	dialog.setMessage("読み取り中...");
    	        	dialog.show();
    			}
				@Override
				protected Integer doInBackground(Intent... params) {
					try {
						Intent intent = params[0];
			        	Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			        	cardId = Util.getHexString(tag.getId());
		    	    	Suica suica = new Suica(tag, SuicaLogViewActivity.this);
		    	    	
		    	    	ArrayList<Suica.History> dataFromCard;
							dataFromCard = suica.readSuica();
		    			int writeCount = mDb.writeHistory(dataFromCard, Util.convertToString(tag.getId()));
					
		    	        boolean flagAutobackup = PreferenceManager.getDefaultSharedPreferences(SuicaLogViewActivity.this).getBoolean("key_autobackup", false);
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
	    	    	mSuicaLogAdapter.notifyDataSetChanged();
	    	    	mListView.invalidateViews();
				    dialog.dismiss();
	    			Toast.makeText(SuicaLogViewActivity.this, "履歴を" + String.valueOf(result)+"件追加しました", Toast.LENGTH_SHORT).show();
				}
    		};
    		task.execute(intent);
        }
    }
    //コンテキストメニュー
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
      super.onCreateContextMenu(menu, view, menuInfo);
      AdapterContextMenuInfo adapterinfo = (AdapterContextMenuInfo)menuInfo;
      History history = (History)mSuicaLogAdapter.getItem(adapterinfo.position);
      menu.setHeaderTitle(history.getProcessType());
      if (history.isProductSales()) {
    	  menu.add(0, END_CODE, 0, "メモの編集");
      }
//      menu.add(0, RELOAD_CODE, 0, "駅名をDBから検索して適用");
    }
    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        History history = (History)mSuicaLogAdapter.getItem(info.position);
   
        switch(item.getItemId()){
        case RELOAD_CODE:
        	history.reLookupStation();
			SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(SuicaLogViewActivity.this);
			SuicaHistoryDB.updateHistory(helper.getWritableDatabase(), mCardId, history);
			helper.close();
	        boolean flagAutobackup = PreferenceManager.getDefaultSharedPreferences(SuicaLogViewActivity.this).getBoolean("key_autobackup", false);
	    	if (flagAutobackup) {
	    		backupToExternalStorage(mCardId);
	    	}
        	mSuicaLogAdapter.notifyDataSetChanged();
        	mListView.invalidateViews();
        	return true;
      //削除
        case END_CODE:
//        	final EditText editText = new EditText(this);
        	final TextView textView = new TextView(this);
        	textView.setGravity(Gravity.CENTER_HORIZONTAL);
        	String rawString = Util.convertToString(history.data);
        	textView.setText("端末ID: " + rawString.substring(16, 20));
        	LinearLayout llayout = new LinearLayout(this);
        	llayout.setOrientation(LinearLayout.VERTICAL);

        	final AutoCompleteTextView editText = new AutoCompleteTextView(this);
        	editText.setText(history.note);
        	editText.setMaxLines(1);
        	editText.setInputType(InputType.TYPE_CLASS_TEXT);
        	final ArrayList<String> candidates = getNoteSuggestion(history);
        	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        			android.R.layout.simple_list_item_1,
        			candidates);
        	editText.setAdapter(adapter);
        	editText.setThreshold(1);
        	editText.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (candidates.size() > 0) {
						editText.showDropDown();
					}
				}
			});
        	
        	llayout.addView(textView);
        	llayout.addView(editText);
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle("メモの編集");

        	builder.setView(llayout);
        	DialogInterface.OnClickListener doc = new DialogInterface.OnClickListener() {
				private History history;
				public DialogInterface.OnClickListener setParam(History h) {
					history = h;
					return this;
				}
				@Override
				public void onClick(DialogInterface dialog, int which) {
					history.note = editText.getText().toString();
					SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(SuicaLogViewActivity.this);
					SuicaHistoryDB.updateHistory(helper.getWritableDatabase(), mCardId, history);
					helper.close();
			        boolean flagAutobackup = sharedPref.getBoolean("key_autobackup", false);
			        boolean flagContribute = sharedPref.getBoolean("key_contribute", false);
			    	if (flagAutobackup) {
			    		backupToExternalStorage(mCardId);
			    	}
			    	if (flagContribute) {
			    		upload_history(mCardId, Util.convertToString(history.data), history.note);
			    	}
					//TODO backup should be done here
					mSuicaLogAdapter.notifyDataSetChanged();
		        	mListView.invalidateViews();
				}
        	}.setParam(history);
        	builder.setPositiveButton("修正完了", doc);
        	builder.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			});
        	builder.create().show();
          return true;
        default:
          return super.onContextItemSelected(item);
        }
      }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
    		//home button was tapped
    		onBackPressed();
    	}
    	return super.onMenuItemSelected(featureId, item);
    }
    

    // オプションメニューが最初に呼び出される時に1度だけ呼び出されます
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューアイテムを追加します
    	menu.add(Menu.NONE, MENU_ID_MENU1, Menu.NONE, "設定");
        
//    	menu.add(Menu.NONE, MENU_ID_MENU2, Menu.NONE, "データベース更新");
//    	menu.add(Menu.NONE, MENU_ID_MENU3, Menu.NONE, "recalc fee");
//        menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, "キャッシュ再構築");
        return super.onCreateOptionsMenu(menu);
    }

    // オプションメニューが表示される度に呼び出されます
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        menu.findItem(MENU_ID_MENU1).setVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    // オプションメニューアイテムが選択された時に呼び出されます
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
        case MENU_ID_MENU1:
    		Intent intent = new Intent(this, PrefActivity.class);
    		startActivity(intent);
        	break;
        case MENU_ID_MENU2:
            DBUtil util = new DBUtil(this);
            try {
				util.copyDataBase();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            util.close();
            break;
        case MENU_ID_MENU3:
        	//recalculate fee
        	SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(this);
        	SQLiteDatabase db = helper.getWritableDatabase();
        	ArrayList<History> histories = mDb.getCurrentData();
        	for (int i = 0; i < histories.size() - 1; i++) {
        		History history = histories.get(i);
        		history.fee = (int)(histories.get(i + 1).balance - history.balance);
            	SuicaHistoryDB.updateHistory(db, mCardId, history);
        	}
        	db.close();
        	break;
/*
        case MENU_ID_MENU1:
        	upload_history();
            ret = true;
            break;
        case MENU_ID_MENU2:
        	mDb.getICs().get(mCardId).rebuildCacheFromFile();
        	mSuicaLogAdapter.notifyDataSetChanged();
        	mListView.invalidateViews();
        	break;
*/
        default:
            ret = super.onOptionsItemSelected(item);
            break;
        }
        return ret;
    }
    
    private ArrayList<String> getNoteSuggestion(History history) {
    	ArrayList<String> result = new ArrayList<String>();
    	String rawString = Util.getHexString(history.data);
    	SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(this);
    	SQLiteDatabase db = helper.getReadableDatabase();
    	Cursor cursor = null;
    	try {
    		cursor = db.query("history", new String[]{"note"}, "raw like ?",
    				new String[]{rawString.substring(0,2)+"______________"+rawString.substring(16,20)+"%"}, null, null, "history_no desc");
    		int indexNote = cursor.getColumnIndex("note");
    		while (cursor.moveToNext()) {
    			String str = cursor.getString(indexNote);
    			if (str.length() > 0) {
    				result.add(str);
    			}
       		}
    	} finally {
    		cursor.close();
    	}
    	db.close();
    	return result;
    }
    
    private void upload_history(String cardId, String raw, String note) {
	    Intent intent = new Intent(this, LogUploader.class);
	    intent.putExtra("url", "http://iijuf.net/suicalogger/upload_note.php");
	    intent.putExtra("paramKeys", new String[]{"cardId", "raw", "note"});
	    intent.putExtra("paramValues", new String[] {cardId, raw, note});
	    this.startService(intent);

    }

    
    private class SuicaLogAdapter extends BaseAdapter {

    	private List<History> mDataSetWithSeparator;
    	private final History SEPARATOR = null;
    	private History getNextHistory (int position) {
    		for (int i = position+1; i < mDataSetWithSeparator.size(); i++) {
    			if (mDataSetWithSeparator.get(i) != null) {
    				return mDataSetWithSeparator.get(i);
    			}
    		}
    		return null;
    	}
    	
    	private long[] getUseOfDay (int position) {
    		if (getItem(position) != null) return null;
    		long[] result = new long[2];
    		//called only from separator
    		//separator must be one
    		long use = 0;
    		long charge = 0;
    	
    		int startPos = position+1;
    		for (int i = startPos; i < mDataSetWithSeparator.size(); i++) {
    			History history = mDataSetWithSeparator.get(i);
    			if (history == null) {
    				break;
    			}
    			int tmp = history.fee;
    			if (tmp > 0) {
    				use += tmp;
    			} else {
    				charge += Math.abs(tmp);
    			}
    		}
    		
    		
    		result[0] = use;
    		result[1] = charge;
    		return result;
    	}

    	public SuicaLogAdapter() {
    		mDataSetWithSeparator = new ArrayList<History>();
    		notifyDataSetChanged();
    	}
    	@Override
    	public void notifyDataSetChanged() {
    		mDataSetWithSeparator = new ArrayList<History>();
    		for (int i = 0; i < mDb.getCurrentData().size()-1;i++) {
    			if (i == 0) {
    				mDataSetWithSeparator.add(SEPARATOR);
    			} else {
    				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
    				String thisDate = sdf.format(mDb.getCurrentData().get(i).getProcessDate());
    				String prevDate = sdf.format(mDb.getCurrentData().get(i-1).getProcessDate());
    				if (!thisDate.contentEquals(prevDate)) {
    					mDataSetWithSeparator.add(SEPARATOR);
    				}    				
    			}
    			mDataSetWithSeparator.add(mDb.getCurrentData().get(i));
    		}
    		super.notifyDataSetChanged();
    	}
		@Override
		public int getCount() {
			return mDataSetWithSeparator.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mDataSetWithSeparator.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NumberFormat nf = NumberFormat.getCurrencyInstance();
			if (!isEnabled(position)) {
				if (convertView == null || convertView.getId() != R.layout.separator) {
					convertView = inflater.inflate(R.layout.separator, null);
				}
				TextView tv = (TextView)convertView.findViewById(R.id.textView_separator);
				TextView tv2 = (TextView)convertView.findViewById(R.id.textView_daysum);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd (E)");
				History nextHistory = mDataSetWithSeparator.get(position+1);
				tv.setText(sdf.format(nextHistory.getProcessDate()));
				long[] use = getUseOfDay(position);
				tv2.setText("利用:" + nf.format(use[0]) + "  チャージ:" + nf.format(use[1]));
			} else {
				if (convertView == null || convertView.getId() != R.layout.row) {
					convertView = inflater.inflate(R.layout.row, null);
				}
				History history = (History)getItem(position);
//    			long use = getUse(position);
				long use = history.fee;
		        TextView textView1 = (TextView) convertView.findViewById(R.id.textView_price);
		        TextView textView3 = (TextView) convertView.findViewById(R.id.textView_note);
		        TextView textView4 = (TextView) convertView.findViewById(R.id.textView_balance);
		        TextView textView5 = (TextView) convertView.findViewById(R.id.textView_processtype);
		        ImageView imageView = (ImageView)convertView.findViewById(R.id.imageView_icon);
		        if (use > 0) {
		        	textView1.setTextColor(Color.RED);
		        } else if (use == 0) {
		        	textView1.setTextColor(Color.BLACK);
		        } else {
		        	textView1.setTextColor(Color.BLUE);
		        	
		        }
		        textView1.setText(nf.format(Math.abs(use)));
		        String noteText = "";
		        /*
		      //入場駅・出場駅とも
		              	case 0x01: return "運賃支払(改札出場)";
		                  case 0x85: return "精算 (乗り越し精算)";
		                  case 0x05: return "精算(入場精算)";
		                  case 0x13: return "支払(新幹線利用)";
		      //入場駅だけ
		                  case 0x03: return "券購(磁気券購入)";
		                  case 0x02: return "チャージ";
		                  case 0x04: return "精算";
		                  case 0x14: return "入A(入場時オートチャージ)";
		                  case 0x48: return "特典(特典チャージ)";
		      //バス（入場だけ）
		                  case 0x0d: return "バス(PiTaPa系)";    //byBus
		                  case 0x0f: return "バス(IruCa系)";     //byBus
		      //表示しない            
		                  case 0x46: return "物販";                 //sales
		                  case 0xc6: return "物現 (現金併用物販)";     //sales
		                  case 0x4b: return "入物 (入場物販)";        //sales
		                  case 0xcb: return "入物 (入場現金併用物販)"; //sales
		                  case 0x49: return "入金(レジ入金)";         //sales
		      //不明
		                  case 0x06: return "窓出(改札窓口処理)";
		                  case 0x07: return "新規(新規発行)";
		                  case 0x08: return "控除(窓口控除)";
		                  case 0x11: return "再発(再発行処理)";
		                  case 0x15: return "出A(出場時オートチャージ)";
		                  case 0x1f: return "入金(バスチャージ)";            //byBus
		                  case 0x23: return "券購 (バス路面電車企画券購入)";  //byBus
		                  case 0x4a: return "物販取消";              //sales
		                  case 0x84: return "精算 (他社精算)";
		      */
		        if (history.isByBus()) {
		        	noteText = "バス利用";
		        	imageView.setImageResource(R.drawable.bus);
		        	String[] entrance = history.getEntranceStation();
		        	noteText = entrance[0] + " " + entrance[1];
		        } else if (history.isProductSales()){
		        	noteText = history.note;
		        	imageView.setImageResource(R.drawable.shopping);
		        } else {
		        	if ((history.data[6] & 0xff) != 0x00
		        			&& (history.data[7] & 0xff) != 0x00){
			        	String[] entrance = history.getEntranceStation();
		        		noteText = entrance[2];
		        		if ((history.data[8] & 0xff) != 0x00
			        			&& (history.data[9] & 0xff) != 0x00){
				        	String[] exit = history.getExitStation();
				        	noteText += " -> " + exit[2];
      			
		        		}
		        	}
		        	if (use >= 0){
		        		imageView.setImageResource(R.drawable.trainpay);
		        	} else {
		        		imageView.setImageResource(R.drawable.charge);
		        	}
		        }
				textView3.setText(noteText);
				textView4.setText("残額: " + nf.format(history.getBalance()));
				textView5.setText(history.getProcessType());
				
			}
			return convertView;
		}

		@Override
		public boolean isEnabled(int position) {
			return (mDataSetWithSeparator.get(position) != null);
		}
    }
}
    

