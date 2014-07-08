package com.fuyo.suicalogger;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.fuyo.suicalogger.Suica.History;
import com.fuyo.suicalogger.SuicaHistoryDB.OpenHelper;
public class LogSearchActivity extends Activity {
	ListView mListView;
	SuicaSearchAdapter mAdapter;
	EditText mSearchBox;
	SQLiteOpenHelper helper;
	SQLiteDatabase db = null;
	String mCardId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mCardId = (String)intent.getExtras().get("cardId");

		Locale.setDefault(Locale.JAPAN);	//for currency JPY
        setContentView(R.layout.activity_log_search);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mSearchBox = (EditText)findViewById(R.id.searchEditText1);
        mListView = (ListView)findViewById(R.id.search_result_list_view);
        mAdapter = new SuicaSearchAdapter(this, R.layout.row, new ArrayList<History>());
        mListView.setAdapter(mAdapter);
        helper = new SuicaHistoryDB.OpenHelper(this);
        mSearchBox.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mAdapter.clear();
				//never search without keyword
				if (s.length() == 0) return;
				
				Cursor cursor = null;
				try {
					ArrayList<History> result = new ArrayList<History>();
					cursor = db.query("history",null, "card_id=? and (entrance_station like ? or exit_station like ? or note like ?)",
							new String[]{ mCardId, "%"+s+"%", "%"+s+"%", "%"+s+"%" }, null, null, "history_no DESC", null);
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
								(new SimpleDateFormat(SuicaHistoryDB.DATE_PATTERN)).parse(cursor.getString(indexProcessDate)),
								cursor.getString(indexEntrance).split(":"),
								cursor.getString(indexExit).split(":"),
								(long)cursor.getInt(indexBalance),
								cursor.getInt(indexHistoryNo),
								cursor.getString(indexNote),
								LogSearchActivity.this
								);
						history.fee = cursor.getInt(indexFee);
						//reassess condition to avoid entrance[0] or [1] , exit[0] or [1] matching
						if (history.isByBus()) {
							result.add(history);
						} else if (history.isProductSales()) {
							result.add(history);
						} else {
							if (history.entranceStation[2].contains(s) || history.exitStation[2].contains(s) || history.note.contains(s)) {
								result.add(history);
							}
						}
					}
					mAdapter.addAll(result);
					mAdapter.notifyDataSetChanged();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}
        	
        });
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	db = helper.getReadableDatabase();
    }
    @Override
    public void onPause() {
    	super.onPause();
    	db.close();
    	db = null;
    }
    
    
    
    private class SuicaSearchAdapter extends ArrayAdapter<History> {
    	private LayoutInflater layoutInflater;
		public SuicaSearchAdapter(Context context, int resource,
				List<History> objects) {
			super(context, resource, objects);
			layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            NumberFormat nf = NumberFormat.getCurrencyInstance();
			History history = (History)getItem(position);
			if (convertView == null) {
				convertView = layoutInflater.inflate(R.layout.row, null);
			}
//			long use = getUse(position);
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
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd (E)");
			textView4.setText(sdf.format(history.processDate));
			textView5.setText(history.getProcessType());
			return convertView;
		}
    
    }
}
