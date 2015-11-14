import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
class RsaKeyGenerator {
    public static void main(String[] a) {
        if (a.length<1) {
            System.out.println("Usage:");
            System.out.println("java RsaKeyGenerator size");
            return;
        }
        int size = Integer.parseInt(a[0]);

        Random rnd = new Random();
        BigInteger p = BigInteger.probablePrime(size/2,rnd);
        BigInteger q = p.nextProbablePrime().nextProbablePrime().nextProbablePrime();
//        BigInteger p = BigInteger.valueOf(17);
//        BigInteger q = BigInteger.valueOf(13);
        BigInteger n = p.multiply(q);
        BigInteger m = (p.subtract(BigInteger.ONE)).multiply(
                q.subtract(BigInteger.ONE));
        List<BigInteger> ea = getCoprime(m);
//        BigInteger e = getCoprime(m);
        List<BigInteger> da =new  ArrayList<>();
        for (BigInteger e :ea){
            da.add(e.modInverse(m));
        }
//        BigInteger d = e.modInverse(m);

        File f = new File("RSAkeys.txt");
        if(f.exists())
            f.delete();

        System.out.println("p: " + p);
        writeLog("p: " + p);
        System.out.println("q: " + q);
        writeLog("q: " + q);
        System.out.println("m: " + m);
        writeLog("m: " + m);
        System.out.println("Modulus: " + n);
        writeLog("Modulus: " + n);
        System.out.println("Key size: " + n.bitLength());
        writeLog("Key size: " + n.bitLength());

        BigInteger ef = BigInteger.ONE;
        BigInteger df = BigInteger.ONE;
        for (int i = 0; i<7;i++){
            System.out.println("Public key"+i+": "+ea.get(i));
            writeLog("Public key"+i+": "+ea.get(i));
            System.out.println("Private key" + i + ": " + da.get(i));
            writeLog("Private key" + i + ": " + da.get(i));
            System.out.println("mod" + i + ": " + da.get(i).multiply(ea.get(i)).mod(m));
            writeLog("mod" + i + ": " + da.get(i).multiply(ea.get(i)).mod(m));
            ef = ef.multiply(ea.get(i));
            df = df.multiply(da.get(i));
        }

        System.out.println("Final Public key: "+ef);
        writeLog("Final Public key: "+ef);
        System.out.println("Final Private key: " + df);
        writeLog("Final Private key: " + df);
        System.out.println("Final key size : " + df.multiply(ef).bitLength());
        writeLog("Final key size : " + df.multiply(ef).bitLength());



//        System.out.println("mod: "+df.multiply(ef).mod(m));
//        System.out.println("gcd: "+m.gcd(ef));

//        BigInteger data =BigInteger.valueOf(1231);
//        System.out.println("original data: "+data);
//
//        BigInteger edata = data.modPow(ef,n);
//        BigInteger edata_s = data;
//
//        for (int i=0; i<7 ;i++){
//            edata_s = edata_s.modPow(ea.get(i),n);
//        }
//
//        System.out.println("enc: "+edata);
//        System.out.println("enc_s: "+edata_s);
//
//        BigInteger ddata = edata.modPow(df,n);
//        BigInteger ddata_s = edata_s;
//
//        for (int i=0; i<7 ;i++){
//            ddata_s = ddata_s.modPow(da.get(i),n);
//        }
//
//        System.out.println("dec: "+ddata);
//        System.out.println("dec_s: "+ddata_s);

    }
    public static List<BigInteger> getCoprime(BigInteger m) {
        List<BigInteger> ea = new ArrayList<>();

        Random rnd = new Random();
        int length = m.bitLength()-1;
        BigInteger e = BigInteger.probablePrime(length,rnd);
        int n = 0;
        while (n <7) {
            while (!(m.gcd(e)).equals(BigInteger.ONE)) {
                e = BigInteger.probablePrime(length, rnd);
            }
            ea.add(e);
            e = BigInteger.probablePrime(length, rnd);
            n++;
        }
        return ea;
    }

    public static void writeLog(String str)
    {
        try
        {
            String path="RSAkeys.txt";
            File file=new File(path);
            if(!file.exists())
                file.createNewFile();
            FileOutputStream out=new FileOutputStream(file,true);
            StringBuffer sb=new StringBuffer();
            sb.append(str+"\n");
            out.write(sb.toString().getBytes("utf-8"));
            out.close();
        }
        catch(IOException ex)
        {
            System.out.println(ex.getStackTrace());
        }
    }
}