package br.com.ciel;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private TextView status;
    private LinearLayout home, settings;
    private ProgressBar meter;
    private TextView meterText;
    private static final int BG=0xff0c141b, CARD=0xff17232d, INK=0xffedf5f6, MUTED=0xffa6bbc5, ACCENT=0xff86e2cf;
    private TextView lastHeard;
    private TextView speechMode;
    private EditText client;
    private Spotify spotify;
    private String pendingAudioAction="";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(i.hasExtra("text"))status.setText(i.getStringExtra("text"));if(i.hasExtra("heard"))lastHeard.setText(i.getStringExtra("heard"));if(i.hasExtra("level")){int level=i.getIntExtra("level",0);meter.setProgress(level);meterText.setText(level>3?"Microfone captando som":"Microfone sem sinal de voz");}}};
    @Override public void onCreate(Bundle saved){super.onCreate(saved);spotify=new Spotify(this);render();UpdateWorker.schedule(this);handle(getIntent());}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private android.graphics.drawable.GradientDrawable shape(int color,int radius){var d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);t.setPadding(0,dp(6),0,dp(6));return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(LinearLayout parent,String title){LinearLayout c=column();c.setPadding(dp(20),dp(16),dp(20),dp(16));c.setBackground(shape(CARD,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(16);parent.addView(c,lp);if(!title.isBlank())c.addView(label(title,13,MUTED));return c;}
    private Button button(LinearLayout parent,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(INK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff233642));b.setMinHeight(dp(52));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);lp.bottomMargin=dp(2);parent.addView(b,lp);b.setOnClickListener(v->action.run());return b;}
    private void page(boolean config){home.setVisibility(config?View.GONE:View.VISIBLE);settings.setVisibility(config?View.VISIBLE:View.GONE);}
    private void render(){
        LinearLayout shell=column();shell.setBackgroundColor(BG);setContentView(shell);
        shell.setOnApplyWindowInsetsListener((v,insets)->{var bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(24),dp(12),dp(24),dp(8));shell.addView(top);
        TextView logo=label("ciel",32,INK);logo.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,-2,1));
        String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
        top.addView(label("PRÉVIA  "+version,12,ACCENT));
        FrameLayout pages=new FrameLayout(this);shell.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView hs=new ScrollView(this);hs.setFillViewport(true);home=column();home.setPadding(dp(24),dp(12),dp(24),dp(20));hs.addView(home);pages.addView(hs);
        ScrollView ss=new ScrollView(this);settings=column();settings.setPadding(dp(24),dp(12),dp(24),dp(20));ss.addView(settings);pages.addView(ss);ss.setVisibility(View.GONE);
        // Switch scroll containers, retaining their fields and unsaved settings.
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(16),dp(8),dp(16),dp(8));shell.addView(nav);
        Button h=new Button(this);h.setText("Início");h.setAllCaps(false);nav.addView(h,new LinearLayout.LayoutParams(0,dp(52),1));h.setOnClickListener(v->{hs.setVisibility(View.VISIBLE);ss.setVisibility(View.GONE);});
        Button cfg=new Button(this);cfg.setText("Configurações");cfg.setAllCaps(false);nav.addView(cfg,new LinearLayout.LayoutParams(0,dp(52),1));cfg.setOnClickListener(v->{hs.setVisibility(View.GONE);ss.setVisibility(View.VISIBLE);});
        home.addView(label("Vamos fazer acontecer.",28,INK));home.addView(label("Toque, espere o microfone abrir e fale seu pedido.",16,MUTED));
        LinearLayout voice=card(home,"COMANDO POR VOZ");
        Button talk=button(voice,"Toque para falar",this::foregroundVoice);talk.setContentDescription("Falar com Ciel");talk.setTextSize(21);talk.setTextColor(BG);talk.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));talk.setMinHeight(dp(104));
        voice.addView(label("Abre o reconhecimento do seu Android.\nNão precisa calibrar Ciel para usar este botão.",14,MUTED));
        status=label("Pronta para começar",18,INK);status.setTag("voice-status");voice.addView(status);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        meter=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);meter.setMax(100);meter.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));voice.addView(meter,new LinearLayout.LayoutParams(-1,dp(6)));
        meterText=label("Nível de áudio aparece durante a escuta local",12,MUTED);voice.addView(meterText);
        LinearLayout transcript=card(home,"O QUE EU OUVI");lastHeard=label(getSharedPreferences("ciel",MODE_PRIVATE).getString("lastHeard","Sua fala aparecerá aqui."),20,INK);lastHeard.setTag("last-heard");lastHeard.setTextIsSelectable(true);transcript.addView(lastHeard);
        LinearLayout examples=card(home,"COMECE COM UM PEDIDO SIMPLES");examples.addView(label("“Aumente o volume”\n“Abra o WhatsApp”\n“Crie alarme para sete horas”",16,INK));examples.addView(label("Spotify precisa de uma conta conectada. A Ciel executa comandos; ainda não é uma conversa livre.",14,MUTED));
        LinearLayout wake=card(home,"CHAMAR SEM TOCAR");wake.addView(label("Diga Ciel, espere a indicação e fale.\nEsse modo é experimental e precisa de calibração.",15,MUTED));button(wake,"Ativar chamada Ciel",()->startAudio(""));button(wake,"Desligar escuta",()->stopService(new Intent(this,ListeningService.class)));
        settings.addView(label("Do seu jeito",28,INK));settings.addView(label("Voz, contas e atualização em um só lugar.",16,MUTED));
        LinearLayout engine=card(settings,"RECONHECIMENTO");speechMode=label(speechModeText(),16,INK);engine.addView(speechMode);
        button(engine,"Usar voz do Android",()->voiceConsent(()->{getSharedPreferences("ciel",MODE_PRIVATE).edit().putBoolean("systemSpeech",true).apply();speechMode.setText(speechModeText());}));
        button(engine,"Usar modelo local",()->{getSharedPreferences("ciel",MODE_PRIVATE).edit().putBoolean("systemSpeech",false).putBoolean("voiceConsent",false).apply();stopService(new Intent(this,ListeningService.class));speechMode.setText(speechModeText());});
        button(engine,"Testar comando no serviço da Ciel",()->startAudio("command"));
        LinearLayout calibration=card(settings,"CHAMADA CIEL");calibration.addView(label("Em silêncio, diga só Ciel três vezes, esperando cada indicação. As referências acústicas ficam neste aparelho; áudio bruto não é salvo.",15,MUTED));button(calibration,"Calibrar minha chamada",()->startAudio("calibrate"));button(calibration,"Apagar calibração",()->{stopService(new Intent(this,ListeningService.class));getSharedPreferences("ciel",MODE_PRIVATE).edit().putBoolean("forgetWake",true).apply();new java.io.File(getFilesDir(),"ciel-wake.bin").delete();status.setText("Calibração apagada.");});
        LinearLayout music=card(settings,"SPOTIFY PREMIUM");music.addView(label("Conecte seu aplicativo Spotify for Developers. Retorno: br.com.ciel://oauth/callback",15,MUTED));music.addView(label("Client ID do Spotify",14,INK));client=new EditText(this);client.setSingleLine(true);client.setTextColor(INK);client.setHintTextColor(MUTED);client.setHint("Cole o Client ID");client.setText(spotify.client());music.addView(client);button(music,"Conectar Spotify",()->{try{spotify.setClient(client.getText().toString());startActivity(new Intent(Intent.ACTION_VIEW,spotify.authorize()));}catch(Exception e){status.setText(e.getMessage());}});button(music,"Desconectar conta",()->{spotify.disconnect();status.setText("Spotify desconectado.");});
        LinearLayout updatesCard=card(settings,"ATUALIZAÇÕES");updatesCard.addView(label("Novas versões são baixadas por rede sem cobrança. O Android pede confirmação para instalar.",15,MUTED));button(updatesCard,"Verificar agora",()->new AlertDialog.Builder(this).setTitle("Verificar atualização").setMessage("O download pode usar cerca de 75 MB, inclusive com dados móveis.").setPositiveButton("Verificar",(d,w)->worker.submit(()->{try{String result=UpdateWorker.check(this);runOnUiThread(()->status.setText(result));}catch(Exception e){runOnUiThread(()->status.setText(e.getMessage()));}})).setNegativeButton("Cancelar",null).show());button(updatesCard,"Instalar versão baixada",()->UpdateWorker.install(this));
        LinearLayout support=card(settings,"DIAGNÓSTICO E PRIVACIDADE");button(support,"Ver diagnóstico",this::diagnostics);button(support,"Permissões do aplicativo",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));support.addView(label("A chamada Ciel é processada localmente. O serviço de voz do Android pode enviar o comando ao provedor. A última transcrição fica neste aparelho. Abrir apps com a tela bloqueada pode exigir desbloqueio.",14,MUTED));
    }
    private void voiceConsent(Runnable accepted){new AlertDialog.Builder(this).setTitle("Usar o reconhecimento do Android?").setMessage("O provedor de voz configurado no seu Android pode processar sua fala pela internet. A Ciel não salva o áudio. A chamada Ciel continua local.").setPositiveButton("Usar",(d,w)->{getSharedPreferences("ciel",MODE_PRIVATE).edit().putBoolean("voiceConsent",true).putBoolean("systemSpeech",true).apply();speechMode.setText(speechModeText());accepted.run();}).setNegativeButton("Cancelar",null).show();}
    void foregroundVoice(){
        if(!getSharedPreferences("ciel",MODE_PRIVATE).getBoolean("voiceConsent",false)||!getSharedPreferences("ciel",MODE_PRIVATE).getBoolean("systemSpeech",false)){voiceConsent(this::foregroundVoice);return;}
        stopService(new Intent(this,ListeningService.class));
        // The platform activity owns capture and readiness; no local model or parallel recorder.
        new Handler(Looper.getMainLooper()).post(()->{try{status.setText("Abrindo o microfone do Android…");startActivityForResult(new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,"pt-BR").putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"Diga seu comando para a Ciel"),70);}catch(ActivityNotFoundException e){status.setText("Nenhum aplicativo de voz disponível. Abra o diagnóstico nas configurações.");}});
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=70)return;if(result!=RESULT_OK||data==null){status.setText("Escuta cancelada ou sem fala reconhecida. Toque para tentar novamente.");return;}var texts=data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);if(texts==null||texts.isEmpty()){status.setText("O Android não retornou uma transcrição.");return;}acceptForeground(texts.get(0));}
    void acceptForeground(String text){getSharedPreferences("ciel",MODE_PRIVATE).edit().putString("lastHeard",text).apply();lastHeard.setText(text);status.setText("Executando seu pedido…");String stripped=Command.afterWake(text);Commands.execute(this,Command.parse(stripped==null?text:stripped),message->{if(!isDestroyed()){status.setText(message);getSharedPreferences("ciel",MODE_PRIVATE).edit().putString("status",message).apply();}});}
    private void diagnostics(){String info="Aparelho: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\nMicrofone: "+(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED?"permitido":"não permitido")+"\nServiço de voz: "+Settings.Secure.getString(getContentResolver(),"voice_recognition_service")+"\nÚltimo estado: "+status.getText();new AlertDialog.Builder(this).setTitle("Diagnóstico da Ciel").setMessage(info).setPositiveButton("Fechar",null).setNeutralButton("Copiar",(d,w)->getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Diagnóstico Ciel",info))).show();}
    private String speechModeText(){return getSharedPreferences("ciel",MODE_PRIVATE).getBoolean("systemSpeech",false)?"Comandos: serviço de voz do Android (pode usar internet).":"Comandos: modelo local limitado. Use a voz do Android se a transcrição estiver errada.";}
    private void startAudio(String action){
        pendingAudioAction=action;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS},10);return;}
        try{status.setText("Preparando o microfone…");startForegroundService(new Intent(this,ListeningService.class).setAction(action));}catch(Exception e){status.setText("Não foi possível iniciar: "+e.getMessage());}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==10){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startAudio(pendingAudioAction);else status.setText("Microfone negado. Permita o microfone nas configurações para usar voz.");}}
    private void handle(Intent i){if(i.getBooleanExtra("installUpdate",false)){i.removeExtra("installUpdate");UpdateWorker.install(this);}if(i.getData()==null)return;var uri=i.getData();i.setData(null);status.setText("Conectando Spotify…");worker.submit(()->{try{spotify.callback(uri);runOnUiThread(()->status.setText("Spotify conectado. Abra o Spotify e reproduza uma música uma vez neste celular."));}catch(Exception e){runOnUiThread(()->status.setText(e.getMessage()));}});}
    @Override public void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handle(i);}
    @Override public void onResume(){super.onResume();lastHeard.setText(getSharedPreferences("ciel",MODE_PRIVATE).getString("lastHeard","Sua fala aparecerá aqui."));IntentFilter filter=new IntentFilter(ListeningService.STATUS);registerReceiver(updates,filter,Context.RECEIVER_NOT_EXPORTED);}
    @Override public void onPause(){unregisterReceiver(updates);super.onPause();}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
