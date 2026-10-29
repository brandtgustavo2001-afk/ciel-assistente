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
    private EditText client;
    private Spotify spotify;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){status.setText(i.getStringExtra("text"));}};
    @Override public void onCreate(Bundle saved){super.onCreate(saved);spotify=new Spotify(this);render();handle(getIntent());}
    private TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,16,0,16);return t;}
    private void button(LinearLayout root,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setPadding(12,16,12,16);root.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());}
    private void render(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(11,18,31));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,36,28,28);scroll.addView(root);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        root.addView(label("Ciel",42,Color.rgb(132,226,211)));
        root.addView(label("Seu assistente por voz",20,Color.WHITE));
        root.addView(label("PRÉVIA EXPERIMENTAL · 0.1\nReconhecimento local em português. Ativação por nome e funcionamento com a tela apagada ainda precisam de teste no seu Poco.",15,Color.LTGRAY));
        status=label(getSharedPreferences("ciel",MODE_PRIVATE).getString("status","Escuta desligada."),18,Color.WHITE);root.addView(status);
        button(root,"Ativar escuta",()->{
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS},10);return;}
            if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);return;}
            try{startForegroundService(new Intent(this,ListeningService.class));}catch(Exception e){status.setText("Não foi possível iniciar: "+e.getMessage());}
        });
        button(root,"Desligar microfone",()->stopService(new Intent(this,ListeningService.class)));
        root.addView(label("Experimente falar",23,Color.WHITE));
        root.addView(label("Ciel, abra o WhatsApp\nCiel, crie alarme para sete horas\nCiel, toque a música Evidências\nCiel, pause a música\nCiel, próxima música",17,Color.LTGRAY));
        root.addView(label("Spotify",23,Color.WHITE));
        root.addView(label("Cadastre o aplicativo no Spotify for Developers e use o retorno:\nbr.com.ciel://oauth/callback\nNão precisa de Client Secret. Sua senha fica no Spotify.",15,Color.LTGRAY));
        client=new EditText(this);client.setSingleLine(true);client.setTextColor(Color.WHITE);client.setHintTextColor(Color.GRAY);client.setHint("Client ID do Spotify");client.setText(spotify.client());root.addView(client);
        button(root,"Conectar Spotify",()->{try{spotify.setClient(client.getText().toString());startActivity(new Intent(Intent.ACTION_VIEW,spotify.authorize()));}catch(Exception e){status.setText(e.getMessage());}});
        button(root,"Desconectar Spotify",()->{spotify.disconnect();status.setText("Conta Spotify desconectada.");});
        button(root,"Configurações do aplicativo",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));
        root.addView(label("Como funciona",23,Color.WHITE));
        root.addView(label("Ative a escuta com o app aberto antes de apagar a tela. A notificação informa quando o microfone está ativo. O modo encerra após 8 horas e não reinicia sozinho.\n\nO áudio é processado localmente e não é salvo. A busca de músicas vai para o Spotify. Não há integração de conversa com ChatGPT nesta prévia.\n\nAbrir aplicativos e criar alarmes exige tocar numa notificação e pode exigir desbloqueio. Comandos do Spotify usam o aparelho ativo da sua conta. Fechar Spotify pausa a reprodução; não força o encerramento do processo.",15,Color.LTGRAY));
    }
    private void handle(Intent i){if(i.getData()==null)return;var uri=i.getData();i.setData(null);status.setText("Conectando Spotify…");worker.submit(()->{try{spotify.callback(uri);runOnUiThread(()->status.setText("Spotify conectado. Abra o Spotify e reproduza uma música uma vez neste celular."));}catch(Exception e){runOnUiThread(()->status.setText(e.getMessage()));}});}
    @Override public void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handle(i);}
    @Override public void onResume(){super.onResume();IntentFilter filter=new IntentFilter(ListeningService.STATUS);registerReceiver(updates,filter,Context.RECEIVER_NOT_EXPORTED);}
    @Override public void onPause(){unregisterReceiver(updates);super.onPause();}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
