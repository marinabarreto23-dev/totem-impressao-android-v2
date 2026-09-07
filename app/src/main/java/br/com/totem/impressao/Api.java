package br.com.totem.impressao;

import java.io.*;import java.net.*;import org.json.*;

final class Api {
    static final String BASE="https://uhjapahwvptbbfrgande.supabase.co/rest/v1/rpc/";
    static final String PUB="sb_publishable_6qEP5uu6lk049JLqEVvDcA_JdEhqfGw";
    static JSONObject rpc(String fn, JSONObject body) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+fn).openConnection();
        c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("apikey",PUB);c.setRequestProperty("Authorization","Bearer "+PUB);
        try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}
        InputStream in=(c.getResponseCode()<400)?c.getInputStream():c.getErrorStream();String s=read(in);
        if(c.getResponseCode()>=400)throw new IOException(s);return new JSONObject(s);
    }
    private static String read(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];int n;while((n=in.read(x))>0)b.write(x,0,n);return b.toString("UTF-8");}
}
