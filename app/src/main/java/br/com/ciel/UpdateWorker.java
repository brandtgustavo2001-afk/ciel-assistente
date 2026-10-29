package br.com.ciel;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import androidx.work.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Updates only come from the configured repository and must have our installed signer. */
public class UpdateWorker extends Worker {
    public static final String REPO="brandtgustavo2001-afk/ciel-assistente";
    public UpdateWorker(Context c,WorkerParameters p){super(c,p);}
    public static void schedule(Context c){
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresStorageNotLow(true).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("ciel-updates",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(UpdateWorker.class,24,TimeUnit.HOURS).setConstraints(constraints).build());
    }
    @Override public Result doWork(){
        try{String result=check(getApplicationContext());getApplicationContext().getSharedPreferences("updates",Context.MODE_PRIVATE).edit().putString("last",result).apply();return Result.success();}
        catch(Exception e){return Result.retry();}
    }
    private static long installed(Context c) throws Exception{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).getLongVersionCode();}
    private static File apk(Context c){return new File(c.getFilesDir(),"updates/Ciel.apk");}
    private static Set<String> signers(PackageInfo p) throws Exception{
        if(p.signingInfo==null)throw new IOException("Assinatura ausente.");
        Set<String> set=new HashSet<>();for(Signature s:p.signingInfo.getApkContentsSigners())set.add(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));return set;
    }
    private static PackageInfo validate(Context c,File file) throws Exception{
        PackageManager pm=c.getPackageManager();PackageInfo candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
        PackageInfo current=pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(candidate==null||!c.getPackageName().equals(candidate.packageName)||candidate.getLongVersionCode()<=current.getLongVersionCode()||!signers(candidate).equals(signers(current)))throw new IOException("Atualização recusada: versão, pacote ou assinatura inválida.");
        return candidate;
    }
    public static boolean ready(Context c){try{validate(c,apk(c));return true;}catch(Exception e){return false;}}
    private static HttpURLConnection connection(String address,boolean api) throws Exception{
        URI uri=URI.create(address);
        if(!UpdatePolicy.trustedUrl(address,api))throw new IOException("Endereço de atualização inválido.");
        HttpURLConnection con=(HttpURLConnection)uri.toURL().openConnection();con.setConnectTimeout(15000);con.setReadTimeout(30000);con.setInstanceFollowRedirects(false);con.setRequestProperty("User-Agent","Ciel-Android-Updater");
        return con;
    }
    private static String json(String url) throws Exception{
        HttpURLConnection con=connection(url,true);con.setRequestProperty("Accept","application/vnd.github+json");
        try{int status=con.getResponseCode();if(status==404)return "{}";if(status!=200)throw new IOException("GitHub indisponível (HTTP "+status+").");try(InputStream in=con.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("Resposta muito grande.");out.write(b,0,n);}return out.toString(StandardCharsets.UTF_8);}}finally{con.disconnect();}
    }
    private static void download(String url,File output,long size) throws Exception{
        String next=url;
        for(int redirects=0;redirects<5;redirects++){
            HttpURLConnection con=connection(next,false);
            try{int status=con.getResponseCode();if(status==301||status==302||status==303||status==307||status==308){next=new URI(next).resolve(con.getHeaderField("Location")).toString();continue;}
                if(status!=200)throw new IOException("Falha no download (HTTP "+status+").");
                try(InputStream in=con.getInputStream();FileOutputStream out=new FileOutputStream(output)){byte[] b=new byte[32768];int n;long bytes=0;while((n=in.read(b))!=-1){bytes+=n;if(bytes>size||bytes>180L*1024*1024)throw new IOException("Download maior que o esperado.");out.write(b,0,n);}if(bytes!=size)throw new IOException("Download incompleto.");out.getFD().sync();}return;
            }finally{con.disconnect();}
        }throw new IOException("Redirecionamentos demais.");
    }
    public static synchronized String check(Context c) throws Exception{
        if(ready(c)){notifyReady(c);return "Atualização baixada. Toque em Instalar atualização.";}
        JSONObject latest=new JSONObject(json("https://api.github.com/repos/"+REPO+"/releases/latest"));
        if(!latest.has("tag_name"))return "Nenhuma versão publicada ainda.";
        if(latest.optBoolean("draft")||latest.optBoolean("prerelease"))return "Nenhuma atualização estável disponível.";
        JSONArray assets=latest.getJSONArray("assets");JSONObject metadata=null,asset=null;
        for(int i=0;i<assets.length();i++){JSONObject item=assets.getJSONObject(i);if("update.json".equals(item.getString("name")))metadata=item;if("Ciel.apk".equals(item.getString("name")))asset=item;}
        if(metadata==null||asset==null)throw new IOException("Esta versão não possui os arquivos completos.");
        // Metadata is a release asset; fetch it through the same restricted download path.
        File dir=new File(c.getFilesDir(),"updates");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Não foi possível criar a pasta de atualização.");
        long metaSize=metadata.getLong("size");if(metaSize<1||metaSize>65536)throw new IOException("Metadados inválidos.");
        File metaFile=new File(dir,"update.json");
        String prefix="https://github.com/"+REPO+"/releases/download/";
        if(!metadata.getString("browser_download_url").startsWith(prefix)||!asset.getString("browser_download_url").startsWith(prefix))throw new IOException("Origem da versão inválida.");
        download(metadata.getString("browser_download_url"),metaFile,metaSize);
        JSONObject info=new JSONObject(new String(java.nio.file.Files.readAllBytes(metaFile.toPath()),StandardCharsets.UTF_8));
        if(info.getLong("versionCode")<=installed(c))return "Ciel já está na versão mais recente.";
        long size=asset.getLong("size");if(!UpdatePolicy.safeSize(size))throw new IOException("Tamanho de atualização inválido.");
        File tmp=new File(dir,"Ciel.download");
        try{download(asset.getString("browser_download_url"),tmp,size);
            MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(tmp)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!hash.toString().equals(info.getString("sha256")))throw new IOException("Arquivo corrompido: hash diferente.");
            PackageInfo candidate=validate(c,tmp);if(candidate.getLongVersionCode()!=info.getLong("versionCode"))throw new IOException("A versão não corresponde ao arquivo.");
            java.nio.file.Files.move(tmp.toPath(),apk(c).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            notifyReady(c);return "Nova versão baixada: "+candidate.versionName+". Toque em Instalar atualização.";
        }finally{if(tmp.exists())tmp.delete();}
    }
    private static void notifyReady(Context c){
        if(c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=c.getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("updates","Atualizações da Ciel",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent pi=PendingIntent.getActivity(c,50,new Intent(c,MainActivity.class).putExtra("installUpdate",true),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        nm.notify(50,new Notification.Builder(c,"updates").setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Ciel • atualização pronta").setContentText("Toque para instalar. Suas configurações serão preservadas.").setContentIntent(pi).setAutoCancel(true).build());
    }
    public static void install(Activity activity){
        try{validate(activity,apk(activity));
            if(!activity.getPackageManager().canRequestPackageInstalls()){
                new AlertDialog.Builder(activity).setTitle("Permitir atualização da Ciel").setMessage("Autorize a Ciel a instalar atualizações. Depois volte e toque em Instalar atualização.").setPositiveButton("Abrir configuração",(d,w)->activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())))).setNegativeButton("Agora não",null).show();return;
            }
            Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".updates",apk(activity));
            activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }catch(Exception e){new AlertDialog.Builder(activity).setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
    }
}
