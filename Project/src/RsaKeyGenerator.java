/* RsaKeyGenerator.java
 * Copyright (c) 2013 by Dr. Herong Yang, herongyang.com
 */
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

        System.out.println("p: "+p);
        System.out.println("q: "+q);
        System.out.println("m: "+m);
        System.out.println("Modulus: "+n);
        System.out.println("Key size: "+n.bitLength());

        BigInteger ef = BigInteger.ONE;
        BigInteger df = BigInteger.ONE;
        for (int i = 0; i<7;i++){
            System.out.println("Public key"+i+": "+ea.get(i));
            System.out.println("Private key"+i+": "+da.get(i));
            System.out.println("mod"+i+": "+da.get(i).multiply(ea.get(i)).mod(m));
            ef = ef.multiply(ea.get(i));
            df = df.multiply(da.get(i));
        }

        System.out.println("Final Public key: "+ef);
        System.out.println("Final Private key: "+df);
        System.out.println("Final key size : "+df.multiply(ef).bitLength());



        System.out.println("mod: "+df.multiply(ef).mod(m));


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
}