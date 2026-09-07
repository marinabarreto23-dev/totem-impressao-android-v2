package br.com.totem.impressao;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){ if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) && c.getSharedPreferences("agent",Context.MODE_PRIVATE).getBoolean("auto",false)) c.startForegroundService(new Intent(c,PrintService.class)); } }
