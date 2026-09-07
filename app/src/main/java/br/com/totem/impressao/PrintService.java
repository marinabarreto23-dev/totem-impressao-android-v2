package br.com.totem.impressao;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

public class PrintService extends Service {
    public static final String CH="totem_print_agent";
    private volatile boolean running=true;
    private PowerManager.WakeLock wake;

    @Override public void onCreate(){
        super.onCreate();createChannel();
        startForeground(2206,new Notification.Builder(this,CH).setContentTitle("Totem Impressão")
            .setContentText("Aguardando pedidos da cozinha")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).build());
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Totem:PrintAgent");
        wake.acquire();
        new Thread(this::loop,"totem-print-loop").start();
    }
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CH,"Impressão automática",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private void loop(){
        SharedPreferences p=getSharedPreferences("agent",MODE_PRIVATE);
        while(running)try{
            String key=p.getString("agent_key",""),mac=p.getString("printer_mac","");
            if(key.isEmpty()||mac.isEmpty()){sleep(2500);continue;}
            JSONObject r=SupabaseApi.rpc("buscar_impressao_pendente",new JSONObject().put("p_api_key",key));
            JSONObject pedido=extractPedido(r);
            if(pedido==null){sleep(1800);continue;}
            boolean ok=true;String err=null;
            try{Printer.print(this,mac,toTicket(pedido));}catch(Exception e){ok=false;err=e.getMessage();}
            JSONObject conf=new JSONObject().put("p_api_key",key).put("p_pedido_id",pedido.optString("id")).put("p_ok",ok);
            conf.put("p_erro",err==null?JSONObject.NULL:err);
            try{SupabaseApi.rpc("confirmar_impressao",conf);}catch(Exception ignored){}
            sleep(ok?700:5000);
        }catch(Exception e){sleep(3500);}
    }
    private JSONObject extractPedido(JSONObject r){
        if(r==null)return null;Object p=r.opt("pedido");if(p instanceof JSONObject)return(JSONObject)p;
        if(r.has("id")&&(r.has("itens")||r.has("numero")))return r;
        Object d=r.opt("data");if(d instanceof JSONObject){Object q=((JSONObject)d).opt("pedido");if(q instanceof JSONObject)return(JSONObject)q;}return null;
    }
    private static boolean real(String s){return s!=null&&!s.trim().isEmpty()&&!"null".equalsIgnoreCase(s.trim())&&!"[]".equals(s.trim());}
    private static String sabores(Object v){
        if(v==null||v==JSONObject.NULL)return "";
        if(v instanceof JSONArray){JSONArray a=(JSONArray)v;StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){String x=a.optString(i,"").trim();if(real(x)){if(s.length()>0)s.append(" / ");s.append(x);}}return s.toString();}
        String raw=String.valueOf(v).trim();if(!real(raw))return "";try{return sabores(new JSONArray(raw));}catch(Exception ignored){}return raw;
    }
    private String toTicket(JSONObject p){
        StringBuilder b=new StringBuilder();
        b.append("[[B]][[L]]PEDIDO #").append(p.optString("numero","")).append("[[S]][[N]]\n");
        if(p.optBoolean("adicional",false))b.append("[[B]]ADICIONAL / NOVOS ITENS[[N]]\n");
        b.append("--------------------------------\n");
        String ref=p.optString("referencia","");if(real(ref))b.append("[[B]]LOCAL: ").append(ref).append("[[N]]\n");
        String cli=p.optString("cliente_nome","");if(real(cli))b.append("[[B]]CLIENTE: ").append(cli).append("[[N]]\n");
        String gar=p.optString("garcom_nome","");if(real(gar))b.append("[[B]]GARCOM: ").append(gar).append("[[N]]\n");
        b.append("--------------------------------\n");
        JSONArray itens=p.optJSONArray("itens");
        if(itens!=null)for(int i=0;i<itens.length();i++){JSONObject x=itens.optJSONObject(i);if(x==null)continue;
            b.append("[[B]][[L]]").append(x.optInt("quantidade",1)).append("x ").append(x.optString("nome","")).append("[[S]][[N]]\n");
            String s=sabores(x.opt("sabores"));if(real(s))b.append("  SABORES: ").append(s).append("\n");
            String o=x.optString("observacao","");if(real(o))b.append("  OBS: ").append(o).append("\n");
        }
        b.append("--------------------------------\n");
        if("aberto".equalsIgnoreCase(p.optString("pagamento_status","")))b.append("[[B]]COMANDA EM ABERTO[[N]]\n");
        else{String f=p.optString("forma_pagamento","");if(real(f))b.append("[[B]]").append(f).append("[[N]]\n");}
        return b.toString();
    }
    private void sleep(long ms){try{Thread.sleep(ms);}catch(Exception ignored){}}
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){running=false;if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
