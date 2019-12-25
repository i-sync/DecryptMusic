package com.example.decryptmusic.Utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptUtil {
    private static final String TAG = DecryptUtil.class.getSimpleName();
    private byte[] CJ = new byte[32];
    private Cipher CK = null;
    private byte[] CL = null;
    private Cipher CM = null;
    private byte[] CN = null;

    @SuppressLint({"GetInstance"})
    public DecryptUtil(String str) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Password can not be null !");
        }
        try {
            byte[] l = l(str, "SHA-384");
            byte[] bArr = new byte[32];
            byte[] bArr2 = new byte[16];
            System.arraycopy(l, 0, bArr, 0, 32);
            System.arraycopy(l, 32, bArr2, 0, 16);
            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
            this.CM = Cipher.getInstance("AES/CBC/NoPadding");
            this.CM.init(2, secretKeySpec, new IvParameterSpec(bArr2));
            this.CN = new byte[4096];
        } catch (Exception e) {
            Log.e("DecryptUtil: 39", e.toString());
        }
    }

    private static byte[] l(String str, String str2) {
        byte[] bArr = null;
        if (str == null || str.length() <= 0) {
            return bArr;
        }
        try {
            MessageDigest instance = MessageDigest.getInstance(str2);
            instance.update(str.getBytes());
            return instance.digest();
        } catch (NoSuchAlgorithmException e) {
            //ThrowableExtension.printStackTrace(e);
            Log.e("DecryptUtil l: 54", e.toString());
            return bArr;
        }
    }

    public int m(ByteBuffer byteBuffer) {
        int i;
        try {
            int position = byteBuffer.position();
            int i2 = position + 4;
            int limit = byteBuffer.limit() - i2;
            int i3 = limit - (limit % 16);
            if ((byteBuffer.get(position + 2) & 1) > 0) {
                int i4 = 0;
                int i5 = i2;
                while (i4 < i3) {
                    this.CN[i4] = byteBuffer.get(i5);
                    i4++;
                    i5++;
                }
                i = this.CM.doFinal(this.CN, 0, i3, this.CN);
                try {
                    byteBuffer.limit(i2 + i);
                    for (int i6 = 0; i6 < i; i6++) {
                        byteBuffer.put(i2 + i6, this.CN[i6]);
                    }
                } catch (Exception e) {
                    Log.e("DecryptUtil m: 80", e.toString());
                    //ThrowableExtension.printStackTrace(e);
                    return i;
                }
            } else {
                byteBuffer.get(this.CJ, 0, this.CJ.length);
                Log.i(TAG, "decrypt: ignore frame ：" + p(this.CJ));
                i = 0;
            }
            try {
                byteBuffer.position(position);
            } catch (Exception e2) {
                //e = e2;
                //ThrowableExtension.printStackTrace(e);
                Log.e("DecryptUtil m: 94", e2.toString());
                return i;
            }
        } catch (Exception e3) {
            //e = e3;
            i = 0;
            //ThrowableExtension.printStackTrace(e);
            return i;
        }
        return i;
    }


    public static String p(byte[] bArr) {
        StringBuilder sb = new StringBuilder("");
        if (bArr == null || bArr.length <= 0) {
            return null;
        }
        for (byte b : bArr) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() < 2) {
                sb.append(0);
            }
            sb.append(hexString);
        }
        return sb.toString();
    }
}

