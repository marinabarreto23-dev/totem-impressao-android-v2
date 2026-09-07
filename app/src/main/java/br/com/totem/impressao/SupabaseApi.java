package br.com.totem.impressao;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class SupabaseApi {
    private static final String BASE = "https://uhjapahwvptbbfrgande.supabase.co/rest/v1/rpc/";
    private static final String API_KEY = "sb_publishable_6qEP5uu6lk049JLqEVvDcA_JdEhqfGw";
    private SupabaseApi() {}

    public static JSONObject rpc(String name, JSONObject body) throws Exception {
        URL url = new URL(BASE + name);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(15000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("apikey", API_KEY);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try(OutputStream os=c.getOutputStream()){ os.write(bytes); }
        int code=c.getResponseCode(); InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
        String text=readAll(is); if(code<200||code>=300) throw new IOException("HTTP "+code+": "+text);
        if(text==null||text.trim().isEmpty()||"null".equals(text.trim())) return new JSONObject();
        String t=text.trim();
        if(t.startsWith("[")){ JSONArray a=new JSONArray(t); return a.length()>0 && a.opt(0) instanceof JSONObject ? a.getJSONObject(0) : new JSONObject(); }
        if(t.startsWith("{")) return new JSONObject(t);
        JSONObject out=new JSONObject(); out.put("value", t); return out;
    }
    private static String readAll(InputStream in) throws Exception { if(in==null)return ""; BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); StringBuilder b=new StringBuilder(); String s; while((s=r.readLine())!=null)b.append(s); return b.toString(); }
}
