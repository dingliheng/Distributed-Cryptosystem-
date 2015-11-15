package com.distributedsys; /**
 * Created by Liheng on 2015/11/14.
 */
import java.math.BigInteger;
import java.io.*;

class RsaKeyDecryption {
    private BigInteger n, d;
    public static void main(String[] a) {
//        if (a.length<3) {
//            System.out.println("Usage:");
//            System.out.println("java com.distributedsys.RsaKeyDecryption key input output");
//            return;
//        }
//        String keyFile = a[0];
//        String input = a[1];
//        String output = a[2];
        String interput;
        RsaKeyDecryption decryptor3 = new RsaKeyDecryption(3);
        interput = decryptor3.initdecrypt("output.txt");  // "initdencrypt" method
        RsaKeyDecryption decryptor5 = new RsaKeyDecryption(5);
        interput = decryptor5.interdecrypt(interput);    // "interdencrypt" method
        RsaKeyDecryption decryptor4 = new RsaKeyDecryption(4);
        interput = decryptor4.interdecrypt(interput);
        RsaKeyDecryption decryptor6 = new RsaKeyDecryption(6);
        interput = decryptor6.interdecrypt(interput);
        RsaKeyDecryption decryptor1 = new RsaKeyDecryption(1);
        interput = decryptor1.interdecrypt(interput);
        RsaKeyDecryption decryptor2 = new RsaKeyDecryption(2);
        interput =  decryptor2.interdecrypt(interput);
        RsaKeyDecryption decryptor = new RsaKeyDecryption(0);
        decryptor.findecrypt(interput, "decrypt.txt");    // "findencrypt" method
    }

    // Reading in RSA private key
    RsaKeyDecryption(int keyNum) {
        try {
            BufferedReader in = new BufferedReader(new FileReader("RSAkeys.txt"));
            String line = in.readLine();
            while (line!=null) {
                if (line.indexOf("Modulus: ")>=0) {
                    n = new BigInteger(line.substring(9));
                }
                if (line.indexOf("Private key"+keyNum+": ")>=0) {
                    d = new BigInteger(line.substring(14));
                }
                line = in.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("--- Reading private key ---");
        System.out.println("Modulus: "+n);
        System.out.println("Key size: "+n.bitLength());
        System.out.println("Private key" + keyNum + ": " + d);
    }

    // Decrypting cipher text
    public String initdecrypt(String intput) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        try {
            FileInputStream fis = new FileInputStream(intput);
//            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(output),"utf-8");
            StringWriter outString = new StringWriter();
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            long blocks = 0;
            int dataSize = 0;
            while (fis.read(cipherTextBlock)>0) {
                blocks++;
                BigInteger cipherText = new BigInteger(1,cipherTextBlock);
                BigInteger clearText = cipherText.modPow(d,n);
                System.out.println(clearText);
//                byte[] clearTextData = clearText.toByteArray();
//                putBytesBlock(clearTextBlock,clearTextData);
                dataSize = clearTextSize;
                if (fis.available()==0) {
                    dataSize = getDataSize(clearTextBlock);
                }
                if (dataSize>=0) {
                    outString.write(clearText.toString()+"\n");
                }
            }
            fis.close();
            outString.close();

            System.out.println("Decryption block count: "+blocks);
            return outString.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Putting bytes data into a block
    public static void putBytesBlock(byte[] block, byte[] data) {
        int bSize = block.length;
        int dSize = data.length;
        int i = 0;
        while (i<dSize && i<bSize) {
            block[bSize-i-1] = data[dSize-i-1];
            i++;
        }
        while (i<bSize) {
            block[bSize-i-1] = (byte)0x00;
            i++;
        }
    }

    // Getting data size from a padded block
    public static int getDataSize(byte[] block) {
        int bSize = block.length;
        int padValue = block[bSize-1];
        return (bSize-padValue)%bSize;
    }

    public String interdecrypt(String string) {
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(string));
            String line = in.readLine();
            StringWriter outString = new StringWriter();
            while (line != null && line.length() != 0) {
                BigInteger ciperText = new BigInteger(line);
                BigInteger clearText = ciperText.modPow(d, n);
                output = output + clearText + "\n";
                line = in.readLine();
//                OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(file),"utf-8");
                outString.write(output+"\n");
                outString.close();

            }
            return outString.toString();
            }catch(Exception ex){
                ex.printStackTrace();
                return null;
            }
        }

    public void findecrypt(String input, String outputfile) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(input));
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(outputfile),"utf-8");
            byte[] clearTextBlock = new byte[clearTextSize];
            long blocks = 0;
            int dataSize = 0;
            String line = in.readLine();
            while (line != null && line.length() != 0) {
                blocks++;
                BigInteger cipherText = new BigInteger(line);
                System.out.println(cipherText);
                BigInteger clearText = cipherText.modPow(d, n);
                System.out.println(clearText);
                byte[] clearTextData = clearText.toByteArray();
                putBytesBlock(clearTextBlock, clearTextData);
                String str = new String(clearTextData, "utf-8");
                output = output+str;
                line = in.readLine();
            }
            char s = output.charAt(output.length() - 1);
            String match = "("+s+")*$";
            output = output.replaceAll(match, "");
            fos.write(output);
            System.out.println(output);
            in.close();
            fos.close();
            System.out.println("Decryption block count: "+blocks);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public String findecrypt(String input) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(input));
//            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(outputfile),"utf-8");
            StringWriter fos = new StringWriter();
            byte[] clearTextBlock = new byte[clearTextSize];
            long blocks = 0;
            int dataSize = 0;
            String line = in.readLine();
            while (line != null && line.length() != 0) {
                blocks++;
                BigInteger cipherText = new BigInteger(line);
                System.out.println(cipherText);
                BigInteger clearText = cipherText.modPow(d, n);
                System.out.println(clearText);
                byte[] clearTextData = clearText.toByteArray();
                putBytesBlock(clearTextBlock, clearTextData);
                String str = new String(clearTextData, "utf-8");
                output = output+str;
                line = in.readLine();
            }
            char s = output.charAt(output.length() - 1);
            String match = "("+s+")*$";
            output = output.replaceAll(match, "");
            fos.write(output);
            System.out.println(output);
            in.close();
            fos.close();
            System.out.println("Decryption block count: "+blocks);
            return fos.toString();
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}
