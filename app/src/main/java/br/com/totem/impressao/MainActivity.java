package br.com.totem.impressao;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    EditText code; TextView status; Spinner printers; ArrayList<BluetoothDevice> devices=new ArrayList<>(); SharedPreferences prefs;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);prefs=getSharedPreferences("agent",MODE_PRIVATE);code=findViewById(R.id.code);status=findViewById(R.id.status);printers=findViewById(R.id.printers);findViewById(R.id.activate).setOnClickListener(v->activate());findViewById(R.id.test).setOnClickListener(v->test());findViewById(R.id.start).setOnClickListener(v->startAgent());findViewById(R.id.stop).setOnClickListener(v->stopAgent());askPermissions();loadDevices();refreshStatus();}
    private void askPermissions(){ArrayList<String> p=new ArrayList<>();if(Build.VERSION.SDK_INT>=31)p.add(Manifest.permission.BLUETOOTH_CONNECT);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),44);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);loadDevices();}
    private void loadDevices(){ try{if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null){status.setText("Bluetooth não disponível");return;}devices.clear();devices.addAll(a.getBondedDevices());Collections.sort(devices,(x,y)->String.valueOf(x.getName()).compareToIgnoreCase(String.valueOf(y.getName())));ArrayList<String> names=new ArrayList<>();for(BluetoothDevice d:devices)names.add((d.getName()==null?"Impressora":d.getName())+"  •  "+d.getAddress());printers.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));String saved=prefs.getString("printer_mac","");for(int i=0;i<devices.size();i++)if(devices.get(i).getAddress().equals(saved))printers.setSelection(i);}catch(Exception e){status.setText("Permita o acesso ao Bluetooth");}}
    private void activate(){String c=code.getText().toString().trim();if(c.length()!=6){status.setText("Digite o código de 6 dígitos");return;}status.setText("Ativando este celular...");new Thread(()->{try{JSONObject r=SupabaseApi.rpc("resgatar_codigo_pareamento_impressao",new JSONObject().put("p_codigo",c).put("p_nome",Build.MANUFACTURER+" "+Build.MODEL));boolean ok=r.optBoolean("ok",false);String key=r.optString("api_key",r.optString("agent_key",r.optString("chave","")));if(!ok&&key.isEmpty())throw new Exception(r.optString("error","Código inválido ou expirado"));if(key.isEmpty())throw new Exception("Servidor não retornou a chave do agente");prefs.edit().putString("agent_key",key).putString("cliente_nome",r.optString("cliente_nome","")).apply();runOnUiThread(()->{status.setText("Celular ativado com sucesso");refreshStatus();});}catch(Exception e){runOnUiThread(()->status.setText("Falha na ativação: "+clean(e.getMessage())));}}).start();}
    private void test(){BluetoothDevice d=selected();if(d==null){status.setText("Selecione uma impressora pareada");return;}prefs.edit().putString("printer_mac",d.getAddress()).apply();status.setText("Enviando teste para a impressora...");new Thread(()->{try{Printer.print(this,d.getAddress(),"      TESTE TOTEM\n------------------------------\nImpressao Bluetooth OK\nGoldensky 58mm\n------------------------------\n");runOnUiThread(()->status.setText("Teste enviado. Verifique o papel."));}catch(Exception e){runOnUiThread(()->status.setText("Falha ao imprimir: "+clean(e.getMessage())));}}).start();}
    private BluetoothDevice selected(){int i=printers.getSelectedItemPosition();return i>=0&&i<devices.size()?devices.get(i):null;}
    private void startAgent(){if(prefs.getString("agent_key","").isEmpty()){status.setText("Ative este celular primeiro");return;}BluetoothDevice d=selected();if(d==null){status.setText("Selecione a Goldensky");return;}prefs.edit().putString("printer_mac",d.getAddress()).putBoolean("auto",true).apply();startForegroundService(new Intent(this,PrintService.class));status.setText("Impressão automática ATIVA");}
    private void stopAgent(){prefs.edit().putBoolean("auto",false).apply();stopService(new Intent(this,PrintService.class));status.setText("Impressão automática desativada neste celular");}
    private void refreshStatus(){if(prefs.getString("agent_key","").isEmpty())status.setText("Aguardando ativação");else status.setText("Celular ativado"+(prefs.getBoolean("auto",false)?" • impressão automática ativa":""));}
    private String clean(String s){return s==null?"erro desconhecido":s.replaceAll("https?://\\S+","servidor");}
}
