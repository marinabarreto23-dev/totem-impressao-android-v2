package br.com.totem.impressao;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public final class Printer {
    private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Printer(){}
    public static void print(Context ctx,String mac,String text) throws Exception {
        if(Build.VERSION.SDK_INT>=31 && ctx.checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED) throw new SecurityException("Permissão Bluetooth não concedida");
        BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter(); if(a==null) throw new Exception("Bluetooth indisponível");
        BluetoothDevice d=a.getRemoteDevice(mac); BluetoothSocket s=d.createRfcommSocketToServiceRecord(SPP);
        try{ a.cancelDiscovery(); s.connect(); OutputStream o=s.getOutputStream(); o.write(new byte[]{0x1B,0x40}); o.write(text.getBytes(Charset.forName("CP860"))); o.write(new byte[]{0x0A,0x0A,0x0A,0x0A}); o.flush(); Thread.sleep(450); }
        finally{ try{s.close();}catch(Exception ignored){} }
    }
}
