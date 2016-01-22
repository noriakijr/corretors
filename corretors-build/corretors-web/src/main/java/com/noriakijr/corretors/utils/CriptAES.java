package com.noriakijr.corretors.utils;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CriptAES {

	private final String ALGORITMO = "AES/CTR/NoPadding";
    private Key chaveAES;
    private IvParameterSpec ivps;
 
    public CriptAES(String key, String iv) {
        byte[] ivArray = Conversor.asciiToArray(iv, false);
        ivps = new IvParameterSpec(ivArray);
        byte[] chaveArray = Conversor.hexStringToArray(key);
        chaveAES = new SecretKeySpec(chaveArray, "AES");
    }
 
    // método para gerar chave
    // java.security.Key
    public static Key gerarChaveAES(String key) {
        byte[] keyArray = Conversor.hexStringToArray(key);
        return new SecretKeySpec(keyArray, "AES");
    }
 
    public String encriptar(String texto) throws Exception {
    	Conversor.arrayToHexString(Conversor.asciiToArray(texto, false));
        Cipher c = Cipher.getInstance(ALGORITMO); // vamos precisar da mesma string para decriptar
        c.init(Cipher.ENCRYPT_MODE, chaveAES, ivps);
        byte[] textoArray = Conversor.hexStringToArray(texto);
        byte[] msg = c.doFinal(textoArray);
        return Conversor.arrayToHexString(msg);
    }
 
    public String desencriptar(String textoCifrado) throws Exception {
        Cipher c = Cipher.getInstance(ALGORITMO); // vamos precisar da mesma string para decriptar
        c.init(Cipher.DECRYPT_MODE, chaveAES, ivps);
        byte[] textoArray = Conversor.hexStringToArray(textoCifrado);
        byte[] msg = c.doFinal(textoArray);
        return Conversor.arrayToHexString(msg);
    }
}
