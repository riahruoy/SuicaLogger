package com.fuyo.suicalogger;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.fuyo.suicalogger.Suica.History;
public class LogSearchActivity extends Activity {
	ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Locale.setDefault(Locale.JAPAN);	//for currency JPY
        setContentView(R.layout.activity_suica_history_view);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mListView = (ListView)findViewById(R.id.search_result_list_view);
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
			History history = (History)getItem(position);
			if (convertView == null) {
				convertView = layoutInflater.inflate(R.layout.row, null);
			}
			
			return convertView;
		}
    
    }
}
