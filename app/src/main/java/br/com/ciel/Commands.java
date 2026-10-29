package br.com.ciel;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.provider.AlarmClock;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class Commands {
    private static final java.util.concurrent.ExecutorService NETWORK=Executors.newSingleThreadExecutor();
    static void execute(Context c,Command command,Consumer<String> reply){
        try{
            switch(command.kind()){
                case "invalid":reply.accept(command.value());return;
                case "whatsapp":case "spotify":{
                    String name=command.kind().equals("whatsapp")?"WhatsApp":"Spotify";
                    Intent i=c.getPackageManager().getLaunchIntentForPackage(command.kind().equals("whatsapp")?"com.whatsapp":"com.spotify.music");
                    if(i==null)reply.accept(name+" não está instalado.");else open(c,i,"abrir "+name,reply);return;
                }
                case "alarm":open(c,new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,command.hour()).putExtra(AlarmClock.EXTRA_MINUTES,command.minute()).putExtra(AlarmClock.EXTRA_MESSAGE,"Ciel").putExtra(AlarmClock.EXTRA_SKIP_UI,false),String.format(Locale.ROOT,"criar alarme às %02d:%02d",command.hour(),command.minute()),reply);return;
                case "volumeUp":case "volumeDown":c.getSystemService(AudioManager.class).adjustStreamVolume(AudioManager.STREAM_MUSIC,command.kind().equals("volumeUp")?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,0);reply.accept("Volume ajustado.");return;
                default:NETWORK.execute(()->{String result;try{result=new Spotify(c.getApplicationContext()).execute(command);}catch(Exception e){result=e.getMessage();}String message=result;new Handler(Looper.getMainLooper()).post(()->reply.accept(message));});
            }
        }catch(Exception e){reply.accept("Não consegui executar: "+e.getMessage());}
    }
    private static void open(Context c,Intent intent,String description,Consumer<String> reply){
        if(intent.resolveActivity(c.getPackageManager())==null){reply.accept("Não encontrei aplicativo para "+description);return;}
        if(c instanceof Activity){c.startActivity(intent);reply.accept("Solicitei "+description+". Confira a tela do aplicativo.");return;}
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("actions","Ações da Ciel",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent pending=PendingIntent.getActivity(c,20,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        nm.notify(2,new Notification.Builder(c,"actions").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Ciel • "+description).setContentText("Toque para continuar. O Android pode pedir desbloqueio.").setContentIntent(pending).setAutoCancel(true).build());
        reply.accept("Toque na notificação para "+description+". O Android pode pedir desbloqueio.");
    }
}
