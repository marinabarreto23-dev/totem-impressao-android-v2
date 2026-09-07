package br.com.totem.impressao;

import android.Manifest;
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
    private static final Charset CP860=Charset.forName("CP860");
    private Printer(){}

    public static void print(Context ctx,String mac,String text)throws Exception{
        if(Build.VERSION.SDK_INT>=31 &&
           ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Permissão de dispositivos próximos não concedida");

        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null)throw new Exception("Bluetooth indisponível");
        BluetoothDevice device=adapter.getRemoteDevice(mac);
        BluetoothSocket socket=device.createRfcommSocketToServiceRecord(SPP);

        try{
            if(Build.VERSION.SDK_INT<31)adapter.cancelDiscovery();
            socket.connect();
            OutputStream out=socket.getOutputStream();
            out.write(new byte[]{0x1B,0x40});

            // Marcadores internos:
            // [[B]] negrito, [[N]] normal
            // [[L]] tamanho levemente maior (altura dupla), [[S]] tamanho normal
            String[] parts=text.split("(?=\\[\\[(?:B|N|L|S)\\]\\])|(?<=\\[\\[(?:B|N|L|S)\\]\\])");
            for(String part:parts){
                if("[[B]]".equals(part))out.write(new byte[]{0x1B,0x45,0x01});
                else if("[[N]]".equals(part))out.write(new byte[]{0x1B,0x45,0x00});
                else if("[[L]]".equals(part))out.write(new byte[]{0x1D,0x21,0x01}); // altura 2x, largura normal
                else if("[[S]]".equals(part))out.write(new byte[]{0x1D,0x21,0x00});
                else out.write(part.getBytes(CP860));
            }

            out.write(new byte[]{0x1B,0x45,0x00});
            out.write(new byte[]{0x1D,0x21,0x00});
            out.write(new byte[]{0x0A,0x0A,0x0A,0x0A});
            out.flush();
            Thread.sleep(450);
        }finally{
            try{socket.close();}catch(Exception ignored){}
        }
    }
}
