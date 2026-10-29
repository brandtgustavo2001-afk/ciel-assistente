package br.com.ciel;

import android.content.*;
import android.net.Uri;
import android.util.Base64;
import org.json.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class Spotify {
    public static final String REDIRECT="br.com.ciel://oauth/callback";
    private final android.content.SharedPreferences prefs;
    public Spotify(Context c) { prefs=c.getSharedPreferences("spotify",Context.MODE_PRIVATE); }
    public void setClient(String id) { if(!client().equals(id.trim())) prefs.edit().clear().putString("client",id.trim()).apply(); }
    public String client() { return prefs.getString("client",""); }
    public boolean connected() { return prefs.contains("refresh"); }
    private String random() { byte[] b=new byte[32]; new SecureRandom().nextBytes(b); return Base64.encodeToString(b,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING); }
    public Uri authorize() throws Exception {
        if (!client().matches("[a-fA-F0-9]{32}")) throw new Exception("Informe o Client ID do Spotify nas configurações.");
        String verifier=random(), state=random();
        prefs.edit().putString("verifier",verifier).putString("state",state).apply();
        String challenge=Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon().appendQueryParameter("client_id",client()).appendQueryParameter("response_type","code").appendQueryParameter("redirect_uri",REDIRECT).appendQueryParameter("state",state).appendQueryParameter("code_challenge_method","S256").appendQueryParameter("code_challenge",challenge).appendQueryParameter("scope","user-read-playback-state user-modify-playback-state").build();
    }
    public void callback(Uri uri) throws Exception {
        if (!REDIRECT.equals(uri.buildUpon().clearQuery().fragment(null).build().toString()) || !Objects.equals(prefs.getString("state",null),uri.getQueryParameter("state")) || !prefs.contains("state")) throw new Exception("Retorno do Spotify inválido. Conecte novamente.");
        String code=uri.getQueryParameter("code"); if(code==null) throw new Exception("Conexão recusada no Spotify.");
        String verifier=prefs.getString("verifier",""); prefs.edit().remove("state").remove("verifier").apply();
        saveToken(request("https://accounts.spotify.com/api/token","POST","grant_type=authorization_code&code="+enc(code)+"&redirect_uri="+enc(REDIRECT)+"&client_id="+enc(client())+"&code_verifier="+enc(verifier),null,true));
    }
    private static String enc(String s) { return java.net.URLEncoder.encode(s,StandardCharsets.UTF_8); }
    private synchronized String token() throws Exception {
        if(prefs.getLong("expires",0)>System.currentTimeMillis()+30000) return prefs.getString("token","");
        String refresh=prefs.getString("refresh",null); if(refresh==null) throw new Exception("Conecte sua conta Spotify primeiro.");
        saveToken(request("https://accounts.spotify.com/api/token","POST","grant_type=refresh_token&refresh_token="+enc(refresh)+"&client_id="+enc(client()),null,true));
        return prefs.getString("token","");
    }
    private void saveToken(JSONObject j) throws Exception {
        var e=prefs.edit().putString("token",j.getString("access_token")).putLong("expires",System.currentTimeMillis()+j.getLong("expires_in")*1000);
        if(j.has("refresh_token")) e.putString("refresh",j.getString("refresh_token")); e.apply();
    }
    public void disconnect() { String id=client(); prefs.edit().clear().putString("client",id).apply(); }
    public String execute(Command command) throws Exception {
        String t=token();
        if(command.kind().equals("play")) {
            JSONObject j=request("https://api.spotify.com/v1/search?type=track&limit=1&q="+enc(command.value()),"GET",null,t,false);
            JSONArray items=j.getJSONObject("tracks").getJSONArray("items");
            if(items.length()==0) return "Não encontrei essa música.";
            JSONObject track=items.getJSONObject(0);
            request("https://api.spotify.com/v1/me/player/play","PUT",new JSONObject().put("uris",new JSONArray().put(track.getString("uri"))).toString(),t,false);
            return "Tocando "+track.getString("name")+" de "+track.getJSONArray("artists").getJSONObject(0).getString("name");
        }
        String path=switch(command.kind()) { case "pause" -> "pause"; case "resume" -> "play"; case "next" -> "next"; case "previous" -> "previous"; default -> throw new Exception("Comando de música inválido."); };
        request("https://api.spotify.com/v1/me/player/"+path,path.equals("next")||path.equals("previous")?"POST":"PUT",null,t,false);
        return path.equals("pause")?"Reprodução pausada. O Android mantém o Spotify instalado e disponível.":"Comando enviado ao Spotify.";
    }
    private JSONObject request(String url,String method,String body,String token,boolean form) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setRequestMethod(method);
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("Content-Type",form?"application/x-www-form-urlencoded":"application/json");
        try {
            if(body!=null){c.setDoOutput(true);try(var out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
            int status=c.getResponseCode();
            if(status==204)return new JSONObject();
            if(status==404)throw new Exception("Abra o Spotify e inicie uma música uma vez para ativar este celular.");
            if(status==401)throw new Exception("A conexão expirou. Reconecte o Spotify.");
            if(status==403)throw new Exception("Spotify recusou o comando. Confira Premium e acesso do aplicativo no Spotify for Developers.");
            if(status==429)throw new Exception("Spotify pediu uma pausa. Tente novamente depois.");
            if(status<200||status>=300)throw new Exception("Spotify indisponível (HTTP "+status+").");
            try(var in=c.getInputStream()){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}
        } finally { c.disconnect(); }
    }
}
