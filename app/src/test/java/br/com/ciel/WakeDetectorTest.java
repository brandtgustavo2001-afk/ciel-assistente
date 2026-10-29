package br.com.ciel;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.*;
import java.io.*;

public class WakeDetectorTest {
    static short[] audio(String name)throws Exception{
        try(InputStream in=WakeDetectorTest.class.getResourceAsStream("/wake/"+name+".wav")){byte[] data=in.readAllBytes();ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);b.position(12);while(b.remaining()>=8){int tag=b.getInt(),size=b.getInt();if(tag==0x61746164){short[] pcm=new short[size/2];for(int i=0;i<pcm.length;i++)pcm[i]=b.getShort();return pcm;}b.position(b.position()+size+(size&1));}throw new IOException("No PCM data");}
    }
    private WakeDetector trained()throws Exception{WakeDetector d=new WakeDetector();for(String s:new String[]{"ciel1","ciel2","ciel3"})d.enroll(audio(s));return d;}
    @Test public void detectsEnrolledNameWithoutAsrDictionary()throws Exception{WakeDetector d=trained();System.out.println("Ciel held-out score="+d.score(audio("ciel-test")));assertTrue(d.matches(audio("ciel-test")));}
    @Test public void rejectsSilenceAndDifferentPhrases()throws Exception{WakeDetector d=trained();assertFalse(d.matches(new short[16000]));for(String s:new String[]{"hello","pause","volume","silvio","se"}){System.out.println(s+" score="+d.score(audio(s)));assertFalse("False activation on "+s,d.matches(audio(s)));}}
    @Test public void templatesSurviveRestart()throws Exception{WakeDetector d=trained();File f=File.createTempFile("ciel","wake");try{d.save(f);WakeDetector loaded=new WakeDetector();loaded.load(f);assertTrue(loaded.matches(audio("ciel-test")));}finally{f.delete();}}
    @Test public void enrollmentNeedsThreeSamples()throws Exception{WakeDetector d=new WakeDetector();d.enroll(audio("ciel1"));assertFalse(d.matches(audio("ciel1")));}
    static short[] segmented(short[] pcm){WakeDetector.Segmenter s=new WakeDetector.Segmenter();short[] chunk=new short[320];s.accept(chunk,320);short[] result=null;for(int i=0;i<pcm.length+16000;i+=320){java.util.Arrays.fill(chunk,(short)0);if(i<pcm.length)System.arraycopy(pcm,i,chunk,0,Math.min(320,pcm.length-i));short[] detected=s.accept(chunk,320);if(detected!=null){assertNull("Only one utterance expected",result);result=detected;}}assertNotNull(result);return result;}
    @Test public void microphoneSegmentationPreservesName()throws Exception{WakeDetector d=new WakeDetector();for(String s:new String[]{"ciel1","ciel2","ciel3"})d.enroll(segmented(audio(s)));assertTrue(d.matches(segmented(audio("ciel-test"))));assertFalse(d.matches(segmented(audio("se"))));}
    @Test public void toleratesLowerPlaybackLevel()throws Exception{short[] quieter=audio("ciel-test");for(int i=0;i<quieter.length;i++)quieter[i]=(short)(quieter[i]*0.5);assertTrue(trained().matches(quieter));}
    @Test public void silentEnrollmentFails(){try{new WakeDetector().enroll(new short[16000]);fail("Silence cannot be a wake template");}catch(IllegalArgumentException expected){}}
    @Test public void corruptedTemplateIsRejected()throws Exception{File f=File.createTempFile("ciel","wake");try{try(DataOutputStream out=new DataOutputStream(new FileOutputStream(f))){out.writeInt(0x4349454c);out.writeInt(Integer.MAX_VALUE);out.writeDouble(0.15);}try{new WakeDetector().load(f);fail("Unbounded allocation should be rejected");}catch(IOException expected){}}finally{f.delete();}}
}
