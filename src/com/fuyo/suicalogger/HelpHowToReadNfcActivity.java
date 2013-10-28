package com.fuyo.suicalogger;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class HelpHowToReadNfcActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_howtoread);
        Button button = (Button) findViewById(R.id.help_howtoread_button);
        button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Editor e = PreferenceManager.getDefaultSharedPreferences(HelpHowToReadNfcActivity.this).edit();
				e.putBoolean("key_show_help_howtoread", false);
				e.commit();
				HelpHowToReadNfcActivity.this.finish();
			}
		});
	}

}
