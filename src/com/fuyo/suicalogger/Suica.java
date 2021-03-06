package com.fuyo.suicalogger;

import static com.fuyo.suicalogger.DBUtil.COLUMNS_IRUCA_STATIONCODE;
import static com.fuyo.suicalogger.DBUtil.COLUMNS_STATIONCODE;
import static com.fuyo.suicalogger.DBUtil.COLUMN_ID;
import static com.fuyo.suicalogger.DBUtil.TABLE_IRUCA_STATIONCODE;
import static com.fuyo.suicalogger.DBUtil.TABLE_STATIONCODE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


import com.fuyo.suicalogger.Util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Environment;
import android.widget.Toast;

public class Suica {
	final static int BLOCK_SIZE = 16;
	private Tag mTag;
	private NfcF mNfc;
	private byte[] mIdm;
	Context mContext;
	Suica(Tag tag, Context context) {
		mTag = tag;
		mIdm = mTag.getId();
    	mNfc = NfcF.get(tag);
    	mContext = context;
	}
	private byte[] createReadCommand(int blockNum, int blockPadding) {
		if (blockNum > 4) blockNum = 4;
		byte[] command = new byte[14+blockNum*2];
		command[0] = (byte)(command.length & 0xFF);
		command[1] = (byte)0x06; //Write without encryption command
    	for (int i = 0; i < mIdm.length && i < 8; i++) {
    		command[i + 2] = mIdm[i];
    	}
    	command[10] = (byte)0x01; //Number of Services
    	command[11] = (byte)0x0f; //Suica history read service
    	command[12] = (byte)0x09; //Suica history read service
    	command[13] = (byte)(blockNum & 0xFF);
    	for (int i = 0; i < blockNum; i++) {
    		command[14 + i * 2] = (byte)0x80;
    		command[14 + i * 2 + 1] = (byte)((i + blockPadding) & 0xFF);
    	}
		return command;
	}
	public ArrayList<History> readSuica() throws IOException {
		final int CHUNK_SIZE = 4; //maximum number of readable block
		final int HISTORY_NUM = 20;
		ArrayList<History> result = new ArrayList<History>();

		mNfc.connect();
		for (int i = 0; i < HISTORY_NUM / CHUNK_SIZE; i++) {
			byte[] res = mNfc.transceive(createReadCommand(CHUNK_SIZE, i * CHUNK_SIZE));
			ReadWithoutEncryptionResponse response = new ReadWithoutEncryptionResponse(res);
			if (response.isCommandSuccess()) {
				int readCount = response.getNumOfReadBlocks();
				for (int j = 0; j < readCount; j++ ) {
					byte[] block = response.getReadBlock(j);
					result.add(new History(block, mContext));
				}
			}
		}
        mNfc.close();
    	return result;
	}
	
	

	public class ReadWithoutEncryptionResponse {
		private byte[] mResponse;
		ReadWithoutEncryptionResponse(byte[] response) {
			mResponse = response;
		}
		public boolean isCommandSuccess() {
			return (mResponse[10] == 0x00 && mResponse[11] == 0x00);
		}
		public int getNumOfReadBlocks() {
			return mResponse[12];
		}
		public byte[] getReadBlock(int blockNum) {
			byte[] block = new byte[16];
			System.arraycopy(mResponse, 13 + blockNum * 16, block, 0, 16);
			return block;
		}
	}



    public static class History {
        final byte[] data;
        final int historyNo;
        final String consoleType;
        final String processType;
        final long balance;
        final Date processDate;
        public String[] entranceStation;
        public String[] exitStation;
        public int fee;
        public String note;
        
        
        Context context;
        /**
         * 機器種別,利用種別,支払種別,入出場種別,日付,入場駅,出場駅,残額,通番,地域コード
         * console,process,    ,        ,processDate,entrance,exit,balance,historyNo
         */
        /**
         * コンストラクタ
         * @param data データのバイト列(16バイト)をセット
         * @param context androidコンテキストをセット
         */
        public History(byte[] data, Context context) {
            this.data = data;
            this.context = context;
            this.consoleType = lookupConsoleType();
            this.processType = lookupProcessType();
            this.processDate = lookupProcessDate();
            this.entranceStation = lookupEntranceStation();
            this.exitStation = lookupExitStation();
            this.balance = lookupBalance();
            this.historyNo = lookupHistoryNo();
            this.note = lookupNote();
            this.fee = 0;
        }
        public void reLookupStation() {
        	this.entranceStation = lookupEntranceStation();
        	this.exitStation = lookupExitStation();
        }
        public History(byte[] data, String consoleType,
        		String processType, Date processDate, String[] entranceStation,
        		String[] exitStation, long balance, int historyNo, String note, Context context) {
        	this.data = data;
        	this.consoleType = consoleType;
        	this.processType = processType;
        	this.processDate = processDate;
        	this.entranceStation = entranceStation;
        	this.exitStation = exitStation;
        	this.balance = balance;
        	this.historyNo = historyNo;
        	this.note = note;
        	this.context = context;
        	this.fee = 0;
        	//TODO overWrite処理
        	
        }
        private int lookupHistoryNo() {
        	return ((data[13] & 0xff) << 8) + (data[14] & 0xff);
        }
        public int getHistoryNo() {
        	return this.historyNo;
        }
        /**
         * 機器種別を取得します
         * @return String 機器種別が戻ります
         */
        private String lookupConsoleType() {
            return Suica.getConsoleType(this.data[0]);
        }
        public String getConsoleType() {
        	return this.consoleType;
        }
        /**
         * 処理種別を取得します
         * @return String 処理種別
         */
        private String lookupProcessType() {
            return Suica.getProcessType(this.data[1]);
        }
        public String getProcessType() {
        	return this.processType;
        }
        /**
         * 残高を取得します
         * @return BigDecimal 残高が戻ります
         */
        private long lookupBalance() {
            return new Long(Util.toInt(new byte[]{this.data[11], this.data[10]}));
        }
        public long getBalance() {
        	return this.balance;
        }
        /**
         * 処理日付(出場日付)を取得します
         * @return byte[]
         */
        private Date lookupProcessDate() {
            int date = Util.toInt(new byte[]{this.data[4], this.data[5]});
            int yy = date >> 9;
            int mm = (date >> 5) & 0xf;
            int dd = date & 0x1f;
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, 2000 + yy);
            c.set(Calendar.MONTH, mm-1);
            c.set(Calendar.DAY_OF_MONTH, dd);

            //物販だったら時間もセット
            if ( this.isProductSales() ) {
                int time = Util.toInt(new byte[]{this.data[6], this.data[7]});
                int hh = time >> 11;
                int min = (time >> 5) & 0x3f;
                c.set(Calendar.HOUR_OF_DAY, hh);
                c.set(Calendar.MINUTE, min);
            } else {
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
            }
            return c.getTime();
        }
        //TODO rename proccess to process
        public Date getProcessDate () {
        	return this.processDate;
        }
        /**
         * 入場駅を取得します
         * @return String バスの場合、序数0に会社名、1停留所名が戻ります
         *  鉄道の場合、序数0に会社名、1に路線名、2に駅名が戻ります
         */
        private String[] lookupEntranceStation() {
            if (!this.isProductSales()) {
                if ( this.isByBus() ) {
                    //バス利用の場合
                    return getBusStop(Util.toInt(new byte[]{this.data[6], this.data[7]})
                            , Util.toInt(new byte[]{this.data[8], this.data[9]}));
                } else {
                    //鉄道利用の場合
                    return getStation((byte)((this.data[15] >> 6) & 0x03), (byte)(this.data[6]), this.data[7]);
                }
            } else {
                return new String[]{"", "", ""};
            }
        }
        public String[] getEntranceStation() {
        	return this.entranceStation;
        }
        /**
         * 出場駅を取得します
         * @return String バスの場合、序数0に会社名、1停留所名が戻ります
         *  鉄道の場合、序数0に会社名、1に路線名、2に駅名が戻ります (バスの場合入場と同じ値となります)
         */
        private String[] lookupExitStation() {
            if (!this.isProductSales()) {
                if ( this.isByBus() ) {
                    //バス利用の場合
                    return getBusStop(Util.toInt(new byte[]{this.data[6], this.data[7]})
                            , Util.toInt(new byte[]{this.data[8], this.data[9]}));
                } else {
                    //鉄道利用の場合
                    return getStation((byte)((this.data[15] >> 4) & 0x03), (byte)(this.data[8]), this.data[9]);
                }
            } else {
                return new String[]{"", "", ""};
            }
        }
        public String[] getExitStation() {
        	return this.exitStation;
        }
        private String[] getStation(byte rc, byte lc, byte sc) {
        	int regionCode = (rc & 0xff);
        	int lineCode = (lc & 0xff);
        	int statioCode = (sc & 0xff);
        	
            int areaCode = regionCode & 0xff;
            DBUtil util = new DBUtil(this.context);
            try {
                SQLiteDatabase db = util.openDataBase();
                Cursor c = db.query(TABLE_STATIONCODE
                        , COLUMNS_STATIONCODE
                        ,   COLUMNS_STATIONCODE[0] + " = '" + areaCode + "' and "
                          + COLUMNS_STATIONCODE[1] + " = '" + (lineCode & 0xff) + "' and "
                          + COLUMNS_STATIONCODE[2] + " = '" + (statioCode & 0xff) + "'"
                        , null, null, null, COLUMN_ID);

                return ( c.moveToFirst() )
                    ?  new String[]{ c.getString(3), c.getString(4), c.getString(5)}
                    :  new String[]{"???" + String.valueOf(regionCode), "???" + String.valueOf(lineCode), "???" + String.valueOf(statioCode)};
            } catch (Exception e) {
                e.printStackTrace();
                return new String[]{"error", "error", "error"};
            } finally {
                util.close();
            }
        }
        /**
         * パス停留所を取得します
         * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
         * @param lineCode 線区コードをセット
         * @param statioCode 駅順コードをセット
         * @return 取得できた場合、序数0に会社名、1停留所名が戻ります
         */
        private String[] getBusStop(int lineCode, int statioCode) {
        	
            DBUtil util = new DBUtil(this.context);
            try {
                SQLiteDatabase db = util.openDataBase();
                Cursor c = db.query(TABLE_IRUCA_STATIONCODE
                        , COLUMNS_IRUCA_STATIONCODE
                        ,   COLUMNS_IRUCA_STATIONCODE[0] + " = '" + lineCode + "' and "
                          + COLUMNS_IRUCA_STATIONCODE[1] + " = '" + statioCode + "'"
                        , null, null, null, COLUMN_ID);
                return ( c.moveToFirst()  )
                    ?  new String[]{c.getString(2), c.getString(4)}
                    :  new String[]{"???" + String.valueOf(lineCode), "???" + String.valueOf(statioCode)};
            } catch (Exception e) {
                e.printStackTrace();
                return new String[]{"error", "error"};
            } finally {
                util.close();
            }
        }

        /**
         * 処理種別がバス利用か否かを検査します
         * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
         * @return boolean バス利用の場合trueが戻ります
         */
        public boolean isByBus() {
            //data[0]端末種別が 車載の場合
            return (this.data[0] & 0xff) == 0x05;
        }
        /**
         *　端末種別が「物販」か否かを判定します
         * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
         * @return boolean 物販だった場合はtrueが戻ります
         */
        public boolean isProductSales() {
            //data[0]端末種別が物販又は自販機
            return (this.data[0] & 0xff) == 0xc7
                || (this.data[0] & 0xff) == 0xc8;
        }
        /**
         *　処理種別が「チャージ」か否かを判定します (店舗名を取得できるか否かを判定します)
         * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
         * @return boolean チャージだった場合はtrueが戻ります
         */
        public boolean isCharge() {
            return ( this.data[1] & 0xff) == 0x02;
        }

        private String lookupNote() {
            String rawString = Util.getHexString(this.data);
            SQLiteOpenHelper helper = new SuicaHistoryDB.OpenHelper(context);
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query("history", new String[]{"distinct note"}, "raw like ?",
                        new String[]{rawString.substring(0,2)+"______________"+rawString.substring(16,20)+"%"}, null, null, "history_no desc");
                int indexNote = cursor.getColumnIndex("note");
                while (cursor.moveToNext()) {
                    String str = cursor.getString(indexNote);
                    if (str.length() > 0) {
                        return str;
                    }
                }
            } finally {
                cursor.close();
            }
            db.close();
            return "";
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            nf.setMaximumFractionDigits(0);
            SimpleDateFormat dfl = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            SimpleDateFormat dfs = new SimpleDateFormat("yyyy/MM/dd");

            StringBuilder sb = new StringBuilder();
//            sb.append("機器種別: " + this.getConsoleType() + "\n");
            sb.append("No." + getHistoryNo() + " : 残高" +nf.format(this.getBalance()) + " " + this.getProcessType() + "\n");
            if ( !this.isProductSales() ) {
//                sb.append("処理日付: " + dfs.format(this.getProccessDate()) + "\n");
                if ( this.isByBus() ) {
                    String[] busStopInfo = this.getEntranceStation();
//                    sb.append("利用会社: " + busStopInfo[0]);
//                  sb.append("停留所: " + busStopInfo[1] + "\n");
                    sb.append(busStopInfo[1] + "(" + busStopInfo[0] + ")");
                } else {
                    String[] entranceInfo = this.getEntranceStation();
                    String[] exitInfo = this.getExitStation();

//                    sb.append("入場: " + "\n");
//                    sb.append("  利用会社: " + entranceInfo[0]+ "\n");
//                    sb.append("  路線名: " + entranceInfo[1]+ "線\n");
//                    sb.append("  駅名: " + entranceInfo[2] + "\n");
                    sb.append(" " + entranceInfo[2] + "(" + entranceInfo[0] + entranceInfo[1] + "線)");
                    if ( !this.isCharge()) {
//                        sb.append("出場: " + "\n");
//                        sb.append("  利用会社: " + exitInfo[0]+ "\n");
//                        sb.append("  路線名: " + exitInfo[1]+ "線\n");
//                        sb.append("  駅名: " + exitInfo[2] + "\n");
                    	sb.append(" - " + exitInfo[2] + "(" + exitInfo[0] + exitInfo[1] +"線)");
                    }
                }
            } else {
//                sb.append("処理日付: " + dfl.format(this.getProccessDate()) + "\n");
            }
            //sb.append("支払種別: " + this.getPaymentType() + "\n");
//            sb.append("残高: " + nf.format(this.getBalance()) + "\n");
//            sb.append("\n " + SuicaHistory.bytesToText(data));
            return sb.toString();
       }



    }
    /**
     * 機器種別を取得します
     * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     * @param cType コンソールタイプをセット
     * @return String 機器タイプが文字列で戻ります
     */
    public static final String getConsoleType(int cType) {
        switch (cType & 0xff) {
            case 0x03: return "精算機";
            case 0x04: return "携帯型端末";
            case 0x05: return "等車載端末"; //bus
            case 0x07: return "券売機";
            case 0x08: return "券売機";
            case 0x09: return "入金機(クイックチャージ機)";
            case 0x12: return "券売機(東京モノレール)";
            case 0x13: return "券売機等";
            case 0x14: return "券売機等";
            case 0x15: return "券売機等";
            case 0x16: return "改札機";
            case 0x17: return "簡易改札機";
            case 0x18: return "窓口端末";
            case 0x19: return "窓口端末(みどりの窓口)";
            case 0x1a: return "改札端末";
            case 0x1b: return "携帯電話";
            case 0x1c: return "乗継清算機";
            case 0x1d: return "連絡改札機";
            case 0x1f: return "簡易入金機";
            case 0x46: return "VIEW ALTTE";
            case 0x48: return "VIEW ALTTE";
            case 0xc7: return "物販端末";  //sales
            case 0xc8: return "自販機";   //sales
            default:
                return "???" + String.valueOf(cType & 0xff);
        }
    }
    /**
     * 処理種別を取得します
     * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     * @param proc 処理タイプをセット
     * @return String 処理タイプが文字列で戻ります
     */
    public static final String getProcessType(int proc) {
        switch (proc & 0xff) {
            case 0x01: return "運賃支払(改札出場)";
            case 0x02: return "チャージ";
            case 0x03: return "券購(磁気券購入)";
            case 0x04: return "精算";
            case 0x05: return "精算(入場精算)";
            case 0x06: return "窓出(改札窓口処理)";
            case 0x07: return "新規(新規発行)";
            case 0x08: return "控除(窓口控除)";
            case 0x0d: return "バス(PiTaPa系)";    //byBus
            case 0x0f: return "バス(IruCa系)";     //byBus
            case 0x11: return "再発(再発行処理)";
            case 0x13: return "支払(新幹線利用)";
            case 0x14: return "入A(入場時オートチャージ)";
            case 0x15: return "出A(出場時オートチャージ)";
            case 0x1f: return "入金(バスチャージ)";            //byBus
            case 0x23: return "券購 (バス路面電車企画券購入)";  //byBus
            case 0x46: return "物販";                 //sales
            case 0x48: return "特典(特典チャージ)";
            case 0x49: return "入金(レジ入金)";         //sales
            case 0x4a: return "物販取消";              //sales
            case 0x4b: return "入物 (入場物販)";        //sales
            case 0xc6: return "物現 (現金併用物販)";     //sales
            case 0xcb: return "入物 (入場現金併用物販)"; //sales
            case 0x84: return "精算 (他社精算)";
            case 0x85: return "精算 (他社入場精算)";
            default:
                return "???";
        }
    }
}
