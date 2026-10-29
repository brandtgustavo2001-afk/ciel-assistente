package br.com.ciel;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.provider.AlarmClock;
import android.speech.tts.TextToSpeech;
import org.json.JSONObject;
import org.vosk.*;
import org.vosk.android.*;
import java.util.Locale;
import java.util.concurrent.*;

public class ListeningService extends Service implements RecognitionListener {
    private SpeechService speech;
    private Model model;
    private TextToSpeech tts;
    private boolean destroyed, busy;
    private long waitingUntil, silenceUntil;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wake;
    public static final String STATUS="br.com.ciel.STATUS";
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("listening","Escuta da Ciel",NotificationManager.IMPORTANCE_LOW));
        Intent stop=new Intent(this,ListeningService.class).setAction("stop");
        Notification n=new Notification.Builder(this,"listening").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Ciel • microfone ativo").setContentText("Reconhecimento local. Toque em Parar para desligar.")
            .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE))
            .addAction(new Notification.Action.Builder(null,"Parar",PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE)).build()).setOngoing(true).build();
        startForeground(1,n);
        wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Ciel:Listening");
        wake.acquire(8*60*60*1000L);
        main.postDelayed(()->{status("Escuta encerrada após 8 horas. Abra a Ciel para ativar novamente.");stopSelf();},8*60*60*1000L);
        tts=new TextToSpeech(this,code->{if(code==TextToSpeech.SUCCESS)tts.setLanguage(new Locale("pt","BR"));});
        status("Carregando reconhecimento local…");
        StorageService.unpack(this,"model-pt","model",m->{
            if(destroyed){m.close();return;}
            model=m;
            try { speech=new SpeechService(new Recognizer(model,16000),16000); speech.startListening(this); status("Escuta ligada. Diga Ciel e o comando. Nome ainda em validação."); }
            catch(Exception e){status("Falha ao iniciar microfone: "+e.getMessage());stopSelf();}
        },e->{status("Não foi possível carregar a fala: "+e.getMessage());stopSelf();});
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent!=null&&"stop".equals(intent.getAction()))stopSelf();
        return START_NOT_STICKY;
    }
    private void status(String text) {
        getSharedPreferences("ciel",MODE_PRIVATE).edit().putString("status",text).apply();
        sendBroadcast(new Intent(STATUS).setPackage(getPackageName()).putExtra("text",text));
    }
    private void reply(String text) {
        main.post(()->{if(destroyed)return; status(text); silenceUntil=SystemClock.elapsedRealtime()+Math.min(15000,1800+text.length()*70L); tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"ciel");});
    }
    private void hear(String json) {
        try {
            String text=new JSONObject(json).optString("text","");
            if(text.isEmpty())return;
            status("Ouvi: "+text);
            long now=SystemClock.elapsedRealtime();
            if(now<silenceUntil||busy)return;
            String after=Command.afterWake(text);
            if(after!=null) {
                waitingUntil=now+12000;
                if(after.isBlank()){reply("Pode falar.");return;}
                waitingUntil=0; run(Command.parse(after));
            } else if(now<waitingUntil) {waitingUntil=0;run(Command.parse(text));}
        } catch(Exception e){status("Erro ao entender a fala.");}
    }
    private void open(Intent intent,String description) {
        // Apps de terceiros não podem ser abertos silenciosamente por um serviço em segundo plano.
        // Uma notificação permite o gesto explícito do usuário e respeita o bloqueio do Android.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(intent.resolveActivity(getPackageManager())==null){reply("Não encontrei aplicativo para "+description);return;}
        PendingIntent pending=PendingIntent.getActivity(this,20,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        getSystemService(NotificationManager.class).notify(2,new Notification.Builder(this,"listening").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Ciel • "+description).setContentText("Toque para continuar. O celular poderá pedir desbloqueio.").setContentIntent(pending).setAutoCancel(true).build());
        reply("Toque na notificação para "+description+". O Android pode pedir desbloqueio.");
    }
    private void run(Command c) {
        switch(c.kind()) {
            case "invalid": reply(c.value()); break;
            case "whatsapp": {Intent i=getPackageManager().getLaunchIntentForPackage("com.whatsapp");if(i==null)reply("WhatsApp não instalado.");else open(i,"abrir WhatsApp");break;}
            case "spotify": {Intent i=getPackageManager().getLaunchIntentForPackage("com.spotify.music");if(i==null)reply("Spotify não instalado.");else open(i,"abrir Spotify");break;}
            case "alarm": open(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,c.hour()).putExtra(AlarmClock.EXTRA_MINUTES,c.minute()).putExtra(AlarmClock.EXTRA_MESSAGE,"Ciel").putExtra(AlarmClock.EXTRA_SKIP_UI,false),String.format(Locale.ROOT,"criar alarme às %02d:%02d",c.hour(),c.minute()));break;
            case "volumeUp": case "volumeDown": getSystemService(AudioManager.class).adjustStreamVolume(AudioManager.STREAM_MUSIC,c.kind().equals("volumeUp")?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,0);reply("Volume ajustado.");break;
            default: busy=true;worker.submit(()->{try{reply(new Spotify(this).execute(c));}catch(Exception e){reply(e.getMessage());}finally{main.post(()->busy=false);}});
        }
    }
    @Override public void onResult(String json){main.post(()->hear(json));}
    @Override public void onFinalResult(String json){main.post(()->hear(json));}
    @Override public void onPartialResult(String json){}
    @Override public void onError(Exception e){status("Microfone interrompido: "+e.getMessage());stopSelf();}
    @Override public void onTimeout(){status("Escuta interrompida. Ative novamente.");stopSelf();}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);if(speech!=null){speech.stop();speech.shutdown();}if(model!=null)model.close();if(tts!=null){tts.stop();tts.shutdown();}if(wake!=null&&wake.isHeld())wake.release();worker.shutdownNow();status("Escuta desligada.");super.onDestroy();}
}
