package br.com.ciel;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.*;
import android.provider.AlarmClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import org.json.JSONObject;
import org.vosk.*;
import org.vosk.android.*;
import java.util.Locale;
import java.util.concurrent.*;

public class ListeningService extends Service {
    private AudioRecord audio;
    private Thread audioThread;
    private Recognizer recognizer;
    private SystemCommandRecognizer systemRecognizer;
    private volatile boolean systemActive;
    private volatile long resumeCaptureAt;
    private Model model;
    private TextToSpeech tts;
    private volatile boolean destroyed, busy, speaking, ttsReady;
    private final java.util.concurrent.atomic.AtomicReference<String> requestedMode=new java.util.concurrent.atomic.AtomicReference<>("");
    private volatile String activeSpeech="";
    private long speechSequence;
    private volatile long waitingUntil, silenceUntil;
    private final WakeDetector detector=new WakeDetector();
    private final WakeDetector.Segmenter segmenter=new WakeDetector.Segmenter();
    private boolean calibrating;
    private long calibrationDeadline;
    private String failure;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wake;
    public static final String STATUS="br.com.ciel.STATUS";
    @Override public void onCreate() {
        super.onCreate();
        systemRecognizer=new SystemCommandRecognizer(this);
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
        tts=new TextToSpeech(this,code->{ttsReady=code==TextToSpeech.SUCCESS;if(ttsReady&&tts!=null)tts.setLanguage(new Locale("pt","BR"));});
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            @Override public void onStart(String id){if(id.equals(activeSpeech))speaking=true;}
            @Override public void onDone(String id){if(id.equals(activeSpeech)){speaking=false;silenceUntil=SystemClock.elapsedRealtime()+150;}}
            @Override public void onError(String id){if(id.equals(activeSpeech))speaking=false;}
        });
        if(getSharedPreferences("ciel",MODE_PRIVATE).getBoolean("forgetWake",false)){new java.io.File(getFilesDir(),"ciel-wake.bin").delete();getSharedPreferences("ciel",MODE_PRIVATE).edit().remove("forgetWake").apply();}
        try{detector.load(new java.io.File(getFilesDir(),"ciel-wake.bin"));}catch(Exception e){status("Calibração não pôde ser lida. Calibre Ciel novamente.");}
        status("Carregando reconhecimento local…");
        StorageService.unpack(this,"model-pt","model",m->{
            if(destroyed){m.close();return;}
            model=m;
            try {
                recognizer=new Recognizer(model,16000);
                audio=createCapture();
                audioThread=new Thread(this::listen,"Ciel-audio");audioThread.start();
                status(detector.ready()?"Escuta ligada. Diga Ciel, aguarde o sinal e fale o comando.":"Microfone ligado. Toque em Calibrar Ciel para ensinar sua chamada, ou em Falar comando para testar.");
            }
            catch(Exception e){fail("Falha ao iniciar microfone: "+e.getMessage());}
        },e->fail("Não foi possível carregar a fala: "+e.getMessage()));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent!=null&&"stop".equals(intent.getAction()))stopSelf();
        else if(intent!=null&&"calibrate".equals(intent.getAction()))requestedMode.set("calibrate");
        else if(intent!=null&&"command".equals(intent.getAction()))requestedMode.set("command");
        return START_NOT_STICKY;
    }
    private void status(String text) {
        getSharedPreferences("ciel",MODE_PRIVATE).edit().putString("status",text).apply();
        sendBroadcast(new Intent(STATUS).setPackage(getPackageName()).putExtra("text",text));
    }
    private void reply(String text) {
        main.post(()->{if(destroyed)return;status(text);if(ttsReady){String id="ciel-"+(++speechSequence);activeSpeech=id;speaking=true;if(tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.ERROR)speaking=false;main.postDelayed(()->{if(id.equals(activeSpeech))speaking=false;},15000);}});
    }
    private void fail(String message){failure=message;status(message);main.post(this::stopSelf);}
    private AudioRecord createCapture(){
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Permita o microfone nas configurações da Ciel.");
        if(getSystemService(AudioManager.class).isMicrophoneMute())throw new IllegalStateException("O microfone está desativado no Android.");
        int size=Math.max(6400,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
        AudioRecord capture=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);
        try{if(capture.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microfone indisponível. Feche outro app de voz.");capture.startRecording();if(capture.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("O Android não iniciou a gravação.");return capture;}
        catch(RuntimeException e){capture.release();throw e;}
    }
    private void heard(String text){
        getSharedPreferences("ciel",MODE_PRIVATE).edit().putString("lastHeard",text).apply();
        sendBroadcast(new Intent(STATUS).setPackage(getPackageName()).putExtra("heard",text));
    }
    private void acceptCommand(String text){
        heard(text);String after=Command.afterWake(text);Command command=Command.parse(after==null?text:after);
        if(command.kind().equals("invalid")){reply("Ouvi: "+text+". "+command.value());return;}
        run(command);
    }
    private void startSystemCommand(){
        systemActive=true;resumeCaptureAt=0;waitingUntil=0;
        audio.stop();audio.release();audio=null;
        status("Preparando o serviço de voz do Android. Aguarde a indicação para falar.");
        main.post(()->{
            if(destroyed)return;
            activeSpeech="";speaking=false;if(tts!=null)tts.stop();
            systemRecognizer.start(new SystemCommandRecognizer.Callback(){
                @Override public void ready(){if(!destroyed)status("Android pronto. Fale o comando agora.");}
                @Override public void partial(String text){heard(text);status("Android está ouvindo…");}
                @Override public void result(String text){if(destroyed)return;resumeCaptureAt=SystemClock.elapsedRealtime()+300;acceptCommand(text);}
                @Override public void error(String message){if(destroyed)return;resumeCaptureAt=SystemClock.elapsedRealtime()+300;reply(message);}
            });
        });
    }
    private void armCommand(){
        if(getSharedPreferences("ciel",MODE_PRIVATE).getBoolean("systemSpeech",false)){startSystemCommand();return;}
        recognizer.reset();segmenter.reset();waitingUntil=SystemClock.elapsedRealtime()+12000;silenceUntil=SystemClock.elapsedRealtime()+250;
        status("Ciel ativada. Fale o comando agora.");
        main.post(()->{if(destroyed)return;try{ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,65);tone.startTone(ToneGenerator.TONE_PROP_BEEP,100);main.postDelayed(tone::release,200);}catch(Exception ignored){}});
    }
    private void listen(){
        short[] chunk=new short[320];long lastPartial=0;long lastLevel=0;long lastMeter=0;
        try{while(!destroyed){
            if(systemActive){if(resumeCaptureAt>0&&SystemClock.elapsedRealtime()>=resumeCaptureAt){audio=createCapture();systemActive=false;resumeCaptureAt=0;segmenter.reset();recognizer.reset();}else{Thread.sleep(20);continue;}}
            int count=audio.read(chunk,0,chunk.length);if(count<0)throw new IllegalStateException("Leitura de áudio falhou ("+count+").");if(count==0)continue;
            long now=SystemClock.elapsedRealtime();
            if(now-lastMeter>150){double energy=0;for(int i=0;i<count;i++)energy+=(double)chunk[i]*chunk[i];int level=(int)Math.min(100,Math.sqrt(energy/count)/50);sendBroadcast(new Intent(STATUS).setPackage(getPackageName()).putExtra("level",level));lastMeter=now;}
            String mode=requestedMode.getAndSet("");
            if(mode.equals("calibrate")){waitingUntil=0;calibrating=true;calibrationDeadline=now+15000;detector.clear();segmenter.reset();recognizer.reset();status("Calibração 1 de 3: diga somente Ciel e faça uma pausa.");}
            else if(mode.equals("command")){calibrating=false;armCommand();}
            if(systemActive)continue;
            if(speaking||now<silenceUntil||busy){segmenter.reset();recognizer.reset();continue;}
            if(calibrating&&now>calibrationDeadline){calibrating=false;detector.clear();segmenter.reset();status("Não captei a chamada. Confira o acesso ao microfone, feche outros apps de voz e toque em Calibrar Ciel novamente.");}
            if(waitingUntil>0){
                if(now>waitingUntil){waitingUntil=0;recognizer.reset();status("Não ouvi um comando. Diga Ciel novamente ou toque em Falar comando.");continue;}
                if(recognizer.acceptWaveForm(chunk,count)){
                    String text=new JSONObject(recognizer.getResult()).optString("text","");
                    if(!text.isBlank()){waitingUntil=0;recognizer.reset();main.post(()->acceptCommand(text));}
                }else if(now-lastPartial>500){String partial=new JSONObject(recognizer.getPartialResult()).optString("partial","");if(!partial.isBlank()){heard(partial);status("Reconhecimento local está ouvindo…");}lastPartial=now;}
                continue;
            }
            short[] phrase=segmenter.accept(chunk,count);
            if(phrase==null)continue;
            if(calibrating){
                try{detector.enroll(phrase);if(detector.ready()){detector.save(new java.io.File(getFilesDir(),"ciel-wake.bin"));calibrating=false;status("Ciel calibrada. Diga Ciel, espere o sinal e depois fale o comando.");reply("Chamada calibrada.");}else{calibrationDeadline=now+15000;status("Calibração "+(detector.size()+1)+" de 3: diga somente Ciel novamente e faça uma pausa.");}}
                catch(Exception e){detector.clear();status(e.getMessage()+" Toque em Calibrar Ciel para recomeçar.");calibrating=false;}
            }else if(detector.ready()){
                if(detector.matches(phrase))armCommand();
                else if(now-lastLevel>3000){status("Escuta ativa. Som detectado, mas não reconheci sua chamada. Diga só Ciel ou refaça a calibração.");lastLevel=now;}
            }
        }}catch(Exception e){if(!destroyed)fail("Escuta interrompida: "+e.getMessage());}
    }
    private void run(Command c){busy=true;Commands.execute(this,c,message->{busy=false;reply(message);});}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);if(systemRecognizer!=null)systemRecognizer.cancel();if(audio!=null)try{audio.stop();}catch(Exception ignored){}if(audioThread!=null)try{audioThread.join(2000);}catch(InterruptedException e){Thread.currentThread().interrupt();}if(audioThread==null||!audioThread.isAlive()){if(audio!=null)audio.release();if(recognizer!=null)recognizer.close();if(model!=null)model.close();}if(tts!=null){tts.stop();tts.shutdown();}if(wake!=null&&wake.isHeld())wake.release();worker.shutdownNow();if(getSharedPreferences("ciel",MODE_PRIVATE).getBoolean("forgetWake",false)){new java.io.File(getFilesDir(),"ciel-wake.bin").delete();}status(failure==null?"Escuta desligada.":failure);super.onDestroy();}
}
