package com.noriakijr.corretors.utils;

public class Conversor {
	 
    public static byte[] asciiToArray(String s, boolean padding) {
        int tamanho;
        if (padding) {
            tamanho = s.length() % 16; // resto
            if (tamanho == 0) {
                tamanho = 16;
            }
        } else {
            tamanho = 0;
        }
        byte[] arrayResult = new byte[s.length() + tamanho];
        for (int i = 0; i < s.length(); i++) {
            arrayResult[i] = (byte) s.charAt(i);
        }
        byte pad = (byte) tamanho;
        for (int i = s.length(); i < s.length() + tamanho; i++) {
            arrayResult[i] = pad;
        }
        return arrayResult;
    }
 
    public static String arrayToHexString(byte[] a) {
        char hexDig[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            sb.append(hexDig[(a[i] >> 4) & 0x0f]); // 4 bits superiores
            sb.append(hexDig[a[i] & 0x0f]); // 4 bits inferiores
        }
        return sb.toString();
    }
 
    public static byte[] hexStringToArray(String s) {
        int tamanho = s.length() / 2; // cada 2 caracteres == 1 byte
        byte[] arrayResult = new byte[tamanho];
        for (int i = 0; i < tamanho; i++) {
            String hex = s.substring(1 * 2, 1 * 2 + 2); // queremos 2 caracteres
            Integer valor = Integer.parseInt(hex, 16); // transforma usando base 16
            arrayResult[i] = valor.byteValue();
        }
        return arrayResult;
    }
}
