package br.com.ciel;

import android.content.*;
import android.os.*;
import android.speech.*;
import java.util.ArrayList;

/** One explicit command per session, never continuous cloud recognition. Main-thread only. */
final class SystemCommandRecognizer {
    interface Callback {void ready();void partial(String text);void result(String text);void error(String message);}
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private Callback callback;
    private long generation;
    SystemCommandRecognizer(Context context){this.context=context;}
    void start(Callback target){
        cancel();callback=target;long session=generation;
        if(!SpeechRecognizer.isRecognitionAvailable(context)){finish(null,"Nenhum serviço de voz do Android está disponível. Ative ou atualize o serviço de voz do Google nas configurações do celular.",session);return;}
        try{
            recognizer=SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle params){if(session==generation&&callback!=null)callback.ready();}
                @Override public void onBeginningOfSpeech(){}
                @Override public void onRmsChanged(float rms){}
                @Override public void onBufferReceived(byte[] buffer){}
                @Override public void onEndOfSpeech(){}
                @Override public void onError(int code){finish(null,errorMessage(code),session);}
                @Override public void onResults(Bundle results){ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);finish(matches==null||matches.isEmpty()?null:matches.get(0),null,session);}
                @Override public void onPartialResults(Bundle results){if(session!=generation||callback==null)return;ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(matches!=null&&!matches.isEmpty())callback.partial(matches.get(0));}
                @Override public void onEvent(int type,Bundle params){}
            });
            Intent request=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
            recognizer.startListening(request);
            main.postDelayed(()->finish(null,"O serviço de voz demorou a responder. Confira a conexão e tente novamente.",session),20000);
        }catch(Exception e){finish(null,"Não foi possível iniciar a voz do Android: "+e.getMessage(),session);}
    }
    private void finish(String text,String error,long session){
        if(session!=generation||callback==null)return;
        Callback target=callback;cancel();
        if(text!=null&&!text.isBlank())target.result(text);
        else target.error(error==null?"O Android não reconheceu a fala. Tente novamente perto do microfone.":error);
    }
    void cancel(){generation++;callback=null;main.removeCallbacksAndMessages(null);if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}}
    private static String errorMessage(int code){return switch(code){
        case SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Falha de rede no serviço de voz. Confira a internet e tente novamente.";
        case SpeechRecognizer.ERROR_AUDIO -> "O Android não conseguiu captar áudio. Feche outras conversas por voz e tente novamente.";
        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "O serviço de voz não tem acesso ao microfone. Confira as permissões da Ciel e do serviço de voz.";
        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O serviço de voz está ocupado. Encerre a outra conversa por voz e tente novamente.";
        case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Português do Brasil não está disponível no serviço de voz. Confira o idioma nas configurações de voz do Android.";
        case SpeechRecognizer.ERROR_NO_MATCH,SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não foi possível reconhecer o comando. Diga, por exemplo: aumente o volume.";
        default -> "O serviço de voz falhou (código "+code+"). Tente novamente.";
    };}
}
