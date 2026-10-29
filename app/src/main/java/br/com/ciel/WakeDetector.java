package br.com.ciel;

import java.io.*;
import java.util.*;

/** Local, user-enrolled acoustic templates. Does not depend on an ASR dictionary. */
public final class WakeDetector {
    public static final int RATE=16000;
    private static final int FFT=512, FRAME=400, HOP=160, BANDS=26, COEFFS=12;
    private final List<float[][]> templates=new ArrayList<>();
    private double threshold=1.0;
    public int size(){return templates.size();}
    public boolean ready(){return size()>=3;}
    public void clear(){templates.clear();threshold=1.0;}
    public void enroll(short[] samples){
        float[][] features=features(samples);
        if(features.length<12||features.length>180)throw new IllegalArgumentException("Fale somente Ciel, sem uma frase longa, perto do microfone.");
        templates.add(features);
        if(ready()){
            double worst=0;
            for(int i=0;i<size();i++)for(int j=i+1;j<size();j++)worst=Math.max(worst,distance(templates.get(i),templates.get(j)));
            if(worst>0.30){clear();throw new IllegalArgumentException("As três chamadas ficaram muito diferentes. Calibre novamente em local silencioso.");}
            threshold=Math.min(0.18,Math.max(0.12,worst*1.35));
        }
    }
    public double score(short[] samples){
        float[][] f=features(samples);double best=Double.POSITIVE_INFINITY;
        for(float[][] t:templates){double ratio=(double)f.length/t.length;if(ratio>=0.55&&ratio<=1.8)best=Math.min(best,distance(f,t));}
        return best;
    }
    public boolean matches(short[] samples){return ready()&&score(samples)<=threshold;}
    public void save(File file)throws IOException{
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(file))){out.writeInt(0x4349454c);out.writeInt(size());out.writeDouble(threshold);for(float[][] t:templates){out.writeInt(t.length);for(float[] frame:t)for(float v:frame)out.writeFloat(v);}}
    }
    public void load(File file)throws IOException{
        clear();if(!file.exists())return;
        List<float[][]> loaded=new ArrayList<>();
        try(DataInputStream in=new DataInputStream(new FileInputStream(file))){
            if(in.readInt()!=0x4349454c)throw new IOException("Calibração inválida.");int count=in.readInt();double limit=in.readDouble();
            if(count!=3||!Double.isFinite(limit)||limit<0.12||limit>0.18)throw new IOException("Calibração incompleta.");
            for(int i=0;i<count;i++){int frames=in.readInt();if(frames<12||frames>180)throw new IOException("Calibração inválida.");float[][] t=new float[frames][COEFFS];for(float[] frame:t)for(int k=0;k<COEFFS;k++){frame[k]=in.readFloat();if(!Float.isFinite(frame[k]))throw new IOException("Calibração inválida.");}loaded.add(t);}
            templates.addAll(loaded);threshold=limit;
        }
    }
    public static float[][] features(short[] samples){
        // Trim leading/trailing silence, retain a small margin around the spoken name.
        int first=samples.length,last=0;
        for(int start=0;start+HOP<=samples.length;start+=HOP){double energy=0;for(int i=start;i<start+HOP;i++)energy+=(double)samples[i]*samples[i];if(Math.sqrt(energy/HOP)>180){first=Math.min(first,start);last=start+HOP;}}
        if(last<=first)return new float[0][COEFFS];
        first=Math.max(0,first-HOP*2);last=Math.min(samples.length,last+HOP*2);
        int count=Math.max(0,1+(last-first-FRAME)/HOP);float[][] result=new float[count][COEFFS];
        int[] bins=new int[BANDS+2];double lo=mel(100),hi=mel(7600);
        for(int i=0;i<bins.length;i++)bins[i]=(int)Math.floor((FFT+1)*hz(lo+(hi-lo)*i/(BANDS+1))/RATE);
        for(int f=0;f<count;f++){
            double[] re=new double[FFT],im=new double[FFT];int start=first+f*HOP;
            for(int i=0;i<FRAME;i++){double value=samples[start+i]/32768.0;double previous=start+i>0?samples[start+i-1]/32768.0:0;re[i]=(value-0.97*previous)*(0.54-0.46*Math.cos(2*Math.PI*i/(FRAME-1)));}
            fft(re,im);double[] logs=new double[BANDS];
            for(int b=0;b<BANDS;b++){
                double power=0;for(int k=bins[b];k<bins[b+2];k++){double weight=k<bins[b+1]?(double)(k-bins[b])/Math.max(1,bins[b+1]-bins[b]):(double)(bins[b+2]-k)/Math.max(1,bins[b+2]-bins[b+1]);power+=weight*(re[k]*re[k]+im[k]*im[k]);}logs[b]=Math.log(Math.max(power,1e-12));
            }
            for(int c=0;c<COEFFS;c++){double value=0;for(int b=0;b<BANDS;b++)value+=logs[b]*Math.cos(Math.PI*(c+1)*(b+0.5)/BANDS);result[f][c]=(float)(value*Math.sqrt(2.0/BANDS)/6.0);}
        }
        // Partial cepstral centering reduces microphone/channel differences without discarding identity.
        for(int c=0;c<COEFFS&&count>0;c++){double mean=0;for(float[] f:result)mean+=f[c];mean/=count;for(float[] f:result)f[c]-=(float)(mean*0.5);}
        return result;
    }
    private static double mel(double hz){return 2595*Math.log10(1+hz/700);}
    private static double hz(double mel){return 700*(Math.pow(10,mel/2595)-1);}
    private static double distance(float[][] a,float[][] b){
        if(a.length==0||b.length==0)return Double.POSITIVE_INFINITY;
        double[] prev=new double[b.length+1];Arrays.fill(prev,Double.POSITIVE_INFINITY);prev[0]=0;
        for(int i=1;i<=a.length;i++){double[] row=new double[b.length+1];Arrays.fill(row,Double.POSITIVE_INFINITY);for(int j=1;j<=b.length;j++){double cost=0;for(int k=0;k<COEFFS;k++){double d=a[i-1][k]-b[j-1][k];cost+=d*d;}row[j]=Math.sqrt(cost/COEFFS)+Math.min(prev[j-1],Math.min(prev[j],row[j-1]));}prev=row;}
        return prev[b.length]/Math.max(a.length,b.length);
    }
    private static void fft(double[] real,double[] imag){
        for(int i=1,j=0;i<FFT;i++){int bit=FFT>>1;for(; (j&bit)!=0;bit>>=1)j^=bit;j^=bit;if(i<j){double t=real[i];real[i]=real[j];real[j]=t;}}
        for(int len=2;len<=FFT;len<<=1){double angle=-2*Math.PI/len;for(int i=0;i<FFT;i+=len)for(int j=0;j<len/2;j++){double c=Math.cos(angle*j),s=Math.sin(angle*j);int p=i+j,q=p+len/2;double r=real[q]*c-imag[q]*s,v=real[q]*s+imag[q]*c;real[q]=real[p]-r;imag[q]=imag[p]-v;real[p]+=r;imag[p]+=v;}}
    }
    /** Segments a short spoken call after 400ms of silence; never retains ambient audio. */
    public static final class Segmenter {
        private final short[] buffer=new short[RATE*3],prefix=new short[RATE/10];private int used,silent,prefixUsed;private boolean speech;
        public void reset(){used=0;silent=0;prefixUsed=0;speech=false;}
        public short[] accept(short[] samples,int count){
            double energy=0;for(int i=0;i<count;i++)energy+=(double)samples[i]*samples[i];boolean voiced=Math.sqrt(energy/Math.max(1,count))>250;
            if(!speech&&!voiced){int kept=Math.min(prefixUsed,prefix.length-count);if(kept>0)System.arraycopy(prefix,prefixUsed-kept,prefix,0,kept);int take=Math.min(count,prefix.length);System.arraycopy(samples,count-take,prefix,Math.max(0,kept),take);prefixUsed=Math.max(0,kept)+take;return null;}
            if(!speech&&prefixUsed>0){System.arraycopy(prefix,0,buffer,0,prefixUsed);used=prefixUsed;prefixUsed=0;}
            speech=true;
            if(used+count>buffer.length){reset();return null;}
            System.arraycopy(samples,0,buffer,used,count);used+=count;silent=voiced?0:silent+count;
            if(silent>=RATE*0.4){short[] result=Arrays.copyOf(buffer,used-silent+RATE/10);reset();return result;}
            return null;
        }
    }
}
