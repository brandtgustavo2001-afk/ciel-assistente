package br.com.ciel;

import android.app.Instrumentation;
import android.content.*;
import android.media.AudioManager;
import android.speech.RecognizerIntent;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.io.*;
import static org.junit.Assert.*;

/** Android runtime tests. Controlled recognizer results test routing, NOT speech accuracy. */
@RunWith(AndroidJUnit4.class)
public class VoiceUiTest {
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private Context context;
    @Before public void setup(){context=inst.getTargetContext();context.getSharedPreferences("ciel",0).edit().putBoolean("voiceConsent",true).remove("lastHeard").commit();}
    @Test public void platformVoiceResultExecutesRealVolumeCommand()throws Exception{
        Intent result=new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,new ArrayList<>(java.util.List.of("aumente o volume")));
        var monitor=inst.addMonitor(new IntentFilter(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),new Instrumentation.ActivityResult(-1,result),true);
        AudioManager audio=context.getSystemService(AudioManager.class);int original=audio.getStreamVolume(AudioManager.STREAM_MUSIC);int initial=Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2);audio.setStreamVolume(AudioManager.STREAM_MUSIC,initial,0);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(MainActivity::foregroundVoice);inst.waitForIdleSync();
            long until=android.os.SystemClock.elapsedRealtime()+8000;while(android.os.SystemClock.elapsedRealtime()<until&&!context.getSharedPreferences("ciel",0).contains("lastHeard"))Thread.sleep(100);
            assertEquals("Native recognizer activity must be requested",1,monitor.getHits());
            assertEquals("aumente o volume",context.getSharedPreferences("ciel",0).getString("lastHeard",""));
            assertEquals(initial+1,audio.getStreamVolume(AudioManager.STREAM_MUSIC));
            scenario.onActivity(a->assertEquals("aumente o volume",((TextView)a.getWindow().getDecorView().findViewWithTag("last-heard")).getText().toString()));
            screenshot("home");
        }finally{inst.removeMonitor(monitor);audio.setStreamVolume(AudioManager.STREAM_MUSIC,original,0);}
    }
    @Test public void cancelledRecognitionDoesNotRunCommand(){
        var monitor=inst.addMonitor(new IntentFilter(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),new Instrumentation.ActivityResult(0,null),true);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(MainActivity::foregroundVoice);inst.waitForIdleSync();assertFalse(context.getSharedPreferences("ciel",0).contains("lastHeard"));
        }finally{inst.removeMonitor(monitor);}
    }
    @Test public void settingsAreSeparateAndHomeIsReadable()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{android.view.View button=findText(a.getWindow().getDecorView(),"Configurações");assertNotNull(button);button.performClick();});inst.waitForIdleSync();screenshot("settings");
        }
    }
    private android.view.View findText(android.view.View v,String text){if(v instanceof TextView t&&text.equals(t.getText().toString()))return v;if(v instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++){var found=findText(group.getChildAt(i),text);if(found!=null)return found;}return null;}
    private void screenshot(String name)throws Exception{var bitmap=inst.getUiAutomation().takeScreenshot();assertNotNull(bitmap);File dir=new File(context.getExternalFilesDir(null),"screenshots");dir.mkdirs();try(var out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
