/**
 * Created by Liheng on 2015/11/14.
 */
import java.math.BigInteger;
import java.io.*;
import java.util.Arrays;

class RsaKeyDecryption {
    private BigInteger n, d;
    public static void main(String[] a) {
//        if (a.length<3) {
//            System.out.println("Usage:");
//            System.out.println("java RsaKeyDecryption key input output");
//            return;
//        }
//        String keyFile = a[0];
//        String input = a[1];
//        String output = a[2];
        RsaKeyDecryption encryptor3 = new RsaKeyDecryption(3);
        encryptor3.initdecrypt("output.txt", "interput.txt");
        RsaKeyDecryption encryptor5 = new RsaKeyDecryption(5);
        encryptor5.interdecrypt("interput.txt");
        RsaKeyDecryption encryptor4 = new RsaKeyDecryption(4);
        encryptor4.interdecrypt("interput.txt");
        RsaKeyDecryption encryptor6 = new RsaKeyDecryption(6);
        encryptor6.interdecrypt("interput.txt");
        RsaKeyDecryption encryptor1 = new RsaKeyDecryption(1);
        encryptor1.interdecrypt("interput.txt");
        RsaKeyDecryption encryptor2 = new RsaKeyDecryption(2);
        encryptor2.interdecrypt("interput.txt");
        RsaKeyDecryption encryptor = new RsaKeyDecryption(0);
        encryptor.findecrypt("interput.txt", "decrypt.txt");
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
    public void initdecrypt(String intput, String output) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        try {
            FileInputStream fis = new FileInputStream(intput);
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(output),"utf-8");
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
                    fos.write(clearText.toString()+"\n");
                }
            }
            fis.close();
            fos.close();
            System.out.println("Decryption block count: "+blocks);
        } catch (Exception ex) {
            ex.printStackTrace();
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

    public void interdecrypt(String file) {
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            while (line != null && line.length() != 0) {
                BigInteger ciperText = new BigInteger(line);
                BigInteger clearText = ciperText.modPow(d, n);
                output = output + clearText + "\n";
                line = in.readLine();
                OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(file),"utf-8");
                fos.write(output+"\n");
                fos.close();
            }
            }catch(Exception ex){
                ex.printStackTrace();
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
            BufferedReader in = new BufferedReader(new FileReader(input));
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
}
