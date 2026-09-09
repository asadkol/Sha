package com.shahpharmacy.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private WebView web;
    private DB db;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        db=new DB(this);
        web=new WebView(this);
        setContentView(web);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(),"AndroidDB");
        web.loadUrl("file:///android_asset/index.html");
    }
    public class Bridge {
        @JavascriptInterface public String loadDb(){ return db.load(1, "{\"purchases\":[],\"sales\":[],\"expenses\":[],\"openingCash\":0}"); }
        @JavascriptInterface public void saveDb(String json){ db.save(1,json); }
        @JavascriptInterface public String loadAuth(){ return db.load(2, "{\"users\":[{\"username\":\"admin\",\"password\":\"1234\",\"role\":\"admin\"},{\"username\":\"staff\",\"password\":\"1234\",\"role\":\"staff\"}],\"current\":null,\"lastLogin\":null}"); }
        @JavascriptInterface public void saveAuth(String json){ db.save(2,json); }
    }

    static class DB extends SQLiteOpenHelper {
        private static final String KEY_ALIAS="ShahPharmacyDbKey";
        DB(Context c){ super(c,"shah_pharmacy.db",null,2); }
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE IF NOT EXISTS app_data (id INTEGER PRIMARY KEY, json TEXT NOT NULL)");
            putRaw(d,1,"{\"purchases\":[],\"sales\":[],\"expenses\":[],\"openingCash\":0}");
        }
        public void onUpgrade(SQLiteDatabase d,int o,int n){
            d.execSQL("CREATE TABLE IF NOT EXISTS app_data (id INTEGER PRIMARY KEY, json TEXT NOT NULL)");
        }
        private static void putRaw(SQLiteDatabase d,int id,String value){
            ContentValues v=new ContentValues(); v.put("id",id); v.put("json",value);
            d.insertWithOnConflict("app_data",null,v,SQLiteDatabase.CONFLICT_REPLACE);
        }
        private static SecretKey key() throws Exception {
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
            if(ks.containsAlias(KEY_ALIAS)) return ((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
            KeyGenerator kg=KeyGenerator.getInstance("AES","AndroidKeyStore");
            kg.init(new android.security.keystore.KeyGenParameterSpec.Builder(KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
            return kg.generateKey();
        }
        private static String encrypt(String plain) throws Exception {
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key());
            byte[] iv=c.getIV(), ct=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return "ENC1:"+Base64.encodeToString(iv,Base64.NO_WRAP)+":"+Base64.encodeToString(ct,Base64.NO_WRAP);
        }
        private static String decrypt(String stored) throws Exception {
            if(!stored.startsWith("ENC1:")) return stored; // legacy plaintext; caller migrates it
            String[] p=stored.split(":",3);
            byte[] iv=Base64.decode(p[1],Base64.NO_WRAP), ct=Base64.decode(p[2],Base64.NO_WRAP);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
            return new String(c.doFinal(ct),StandardCharsets.UTF_8);
        }
        synchronized String load(int id,String fallback){
            SQLiteDatabase d=getReadableDatabase();
            Cursor c=d.query("app_data",new String[]{"json"},"id=?",new String[]{String.valueOf(id)},null,null,null);
            try{
                if(!c.moveToFirst()){ save(id,fallback); return fallback; }
                String stored=c.getString(0);
                try{
                    String plain=decrypt(stored);
                    if(!stored.startsWith("ENC1:")) save(id,plain); // migrate old plaintext into encrypted storage
                    return plain;
                }catch(Exception e){ return fallback; }
            }finally{c.close();}
        }
        synchronized void save(int id,String json){
            try{
                String encrypted=encrypt(json);
                SQLiteDatabase d=getWritableDatabase();
                ContentValues v=new ContentValues(); v.put("id",id); v.put("json",encrypted);
                d.insertWithOnConflict("app_data",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            }catch(Exception ignored){}
        }
    }
    @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
