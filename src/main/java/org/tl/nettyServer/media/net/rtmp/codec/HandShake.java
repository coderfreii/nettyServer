package org.tl.nettyServer.media.net.rtmp.codec;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.message.Constants;
import org.tl.nettyServer.media.util.Tools;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Arrays;

@Slf4j
public class HandShake {
    public static final byte[] GENUINE_FMS_KEY = {
            (byte) 0x47, (byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62,
            (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c,
            (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x69, (byte) 0x61, (byte) 0x20, (byte) 0x53, (byte) 0x65,
            (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
            (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Media Server 001
            (byte) 0xf0, (byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68, (byte) 0xbe, (byte) 0xe8, (byte) 0x2e, (byte) 0x00, (byte) 0xd0, (byte) 0xd1,
            (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57, (byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29, (byte) 0x80, (byte) 0x6f, (byte) 0xab,
            (byte) 0x93, (byte) 0xb8, (byte) 0xe6, (byte) 0x36,
            (byte) 0xcf, (byte) 0xeb, (byte) 0x31, (byte) 0xae}; // 68


    public static final byte[] GENUINE_FP_KEY = {
            (byte) 0x47, (byte) 0x65, (byte) 0x6E, (byte) 0x75, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x20, (byte) 0x41, (byte) 0x64, (byte) 0x6F, (byte) 0x62,
            (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6C,
            (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x50, (byte) 0x6C, (byte) 0x61, (byte) 0x79, (byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x30,
            (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Player 001
            (byte) 0xF0, (byte) 0xEE,
            (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, (byte) 0xBE, (byte) 0xE8, (byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, (byte) 0x02, (byte) 0x9E,
            (byte) 0x7E, (byte) 0x57, (byte) 0x6E, (byte) 0xEC,
            (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB, (byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB,
            (byte) 0x31, (byte) 0xAE}; // 62


    private byte handshakeType;
    private boolean fp9Handshake = false;
    private int algorithm = 1;
    private int swfSize;

    private byte[] outgoingPublicKey;
    private byte[] incomingPublicKey;

    private byte[] s1;
    private int digestPosServer;


    static {
        // add bouncycastle security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public void setHandshakeType(byte handshakeType) {
        this.handshakeType = handshakeType;
    }

    public BufFacade decodeClientRequest1(BufFacade in) {
        //c1 客户端的握手内容
        byte[] c1 = new byte[Constants.HANDSHAKE_SIZE];
        in.readBytes(c1);

        //S1
        byte[] handshakeBytes = createHandshakeBytesForS1();
        s1 = new byte[Constants.HANDSHAKE_SIZE];
        System.arraycopy(handshakeBytes, 0, s1, 0, Constants.HANDSHAKE_SIZE);
        if (log.isDebugEnabled()) {
            log.debug("Flash player version {}", Hex.encodeHexString(Arrays.copyOfRange(c1, 4, 8)));
        }

        // check for un-versioned handshake
        fp9Handshake = (c1[4] & 0xff) != 0;
        if (!fp9Handshake) {
            return generateUnversionedHandshake(c1, s1);
        }

        // make sure this is a client we can communicate with


        // handle encryption setup
        if (useEncryption()) {
            // start off with algorithm 1 if we're type 6, 8, or 9
            algorithm = 1;
            // get the DH offset in the handshake bytes, generates DH keypair, and adds the public key to handshake bytes
            int clientDHOffset = getDHOffset(algorithm, c1, 0);
            log.trace("Incoming DH offset: {}", clientDHOffset);
            // get the clients public key
            outgoingPublicKey = new byte[KEY_LENGTH];
            System.arraycopy(c1, clientDHOffset, outgoingPublicKey, 0, KEY_LENGTH);
            log.debug("Client public key: {}", Hex.encodeHexString(outgoingPublicKey));
            // get the servers dh offset
            int serverDHOffset = getDHOffset(algorithm, handshakeBytes, 0);
            log.trace("Outgoing DH offset: {}", serverDHOffset);
            // create keypair
            KeyPair keys = generateKeyPair();
            // get public key
            incomingPublicKey = getPublicKey(keys);
            log.debug("Server public key: {}", Hex.encodeHexString(incomingPublicKey));
            // add to handshake bytes
            System.arraycopy(incomingPublicKey, 0, handshakeBytes, serverDHOffset, KEY_LENGTH);
            // create the RC4 ciphers
            //进行RC4加密算法
            initRC4Encryption(getSharedSecret(outgoingPublicKey, keyAgreement));
        }

        // create the server digest
        //根据握手内容和算法算出一个偏移量
        digestPosServer = getDigestOffset(algorithm, handshakeBytes, 0);
        // calculate the server hash and add to the handshake bytes (S1)
        // 计算服务端hash同时添加到握手数据中
        calculateDigest(digestPosServer, handshakeBytes, 0, GENUINE_FMS_KEY, 36, s1, digestPosServer);
        log.debug("Server digest: {}", Hex.encodeHexString(Arrays.copyOfRange(s1, digestPosServer, digestPosServer + Constants.DIGEST_LENGTH)));

        // get the client digest
        log.trace("Trying algorithm: {}", algorithm);
        int digestPosClient = getDigestOffset(algorithm, c1, 0);
        log.debug("Client digest position offset: {}", digestPosClient);
        if (!verifyDigest(digestPosClient, c1, GENUINE_FP_KEY, 30)) {
            // try a different position
            algorithm ^= 1;
            log.trace("Trying algorithm: {}", algorithm);
            digestPosClient = getDigestOffset(algorithm, c1, 0);
            log.debug("Client digest position offset: {}", digestPosClient);
            if (!verifyDigest(digestPosClient, c1, GENUINE_FP_KEY, 30)) {
                log.warn("Client digest verification failed");
                return null;
            }
        }
        // digest verification passed
        log.debug("Client digest: {}", Hex.encodeHexString(Arrays.copyOfRange(c1, digestPosClient, digestPosClient + Constants.DIGEST_LENGTH)));

        // swfVerification bytes are the sha256 hmac hash of the decompressed swf, the key is the last 32 bytes of the server handshake
        // swf的key是服务端最后32字节 ，验证解压后的swf是否为sha256 hmac hash
        if (swfSize > 0) {
            //how in the heck do we generate a hash for a swf when we don't know which one is requested
            //当我们不知道是那一个请求的时候，是怎样生成swf的hash值的
            byte[] swfHash = new byte[Constants.DIGEST_LENGTH];
            calculateSwfVerification(s1, swfHash, swfSize);
            log.debug("Swf digest: {}", Hex.encodeHexString(swfHash));
        }
        // calculate the response
        byte[] digestResp = new byte[Constants.DIGEST_LENGTH];
        byte[] signatureResponse = new byte[Constants.DIGEST_LENGTH];
        // compute digest key
        // 计算概要密匙
        calculateHMAC_SHA256(c1, digestPosClient, Constants.DIGEST_LENGTH, GENUINE_FMS_KEY, GENUINE_FMS_KEY.length, digestResp, 0);
        log.debug("Digest response (key): {}", Hex.encodeHexString(digestResp));
        calculateHMAC_SHA256(c1, 0, (Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH), digestResp, digestResp.length, signatureResponse, 0);
        log.debug("Signature response: {}", Hex.encodeHexString(signatureResponse));

        //加密
        if (useEncryption()) {
            switch (handshakeType) {
                case RTMPConnection.RTMP_ENCRYPTED_XTEA:
                    log.debug("RTMPE type 8 XTEA");
                    // encrypt signatureResp
                    for (int i = 0; i < Constants.DIGEST_LENGTH; i += 8) {
                        //encryptXtea(signatureResp, i, digestResp[i] % 15);
                    }
                    break;
                case RTMPConnection.RTMP_ENCRYPTED_BLOWFISH:
                    log.debug("RTMPE type 9 Blowfish");
                    // encrypt signatureResp
                    for (int i = 0; i < Constants.DIGEST_LENGTH; i += 8) {
                        //encryptBlowfish(signatureResp, i, digestResp[i] % 15);
                    }
                    break;
            }
        }

        //copy一份c1作为s2 并将签名复制进去
        byte[] s2 = Arrays.copyOfRange(c1, 0, c1.length);
        System.arraycopy(signatureResponse, 0, s2, (Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH), Constants.DIGEST_LENGTH);
        // create output buffer for S0+S1+S2
        // 为S0+S1+S2创建输出数据
        BufFacade s0s1s2 = BufFacade.buffer(Constants.HANDSHAKE_SIZE * 2 + 1); // 3073
        // set handshake with encryption type
        s0s1s2.writeByte(handshakeType); // 1
        s0s1s2.writeBytes(s1); // 1536
        s0s1s2.writeBytes(s2); // 1536
        // clear original base bytes
        handshakeBytes = null;
        if (log.isTraceEnabled()) {
            log.trace("S0+S1+S2 size: {}", s0s1s2.capacity());
        }
        return s0s1s2;
    }

    public static final byte RTMP_NON_ENCRYPTED = (byte) 0x03;


    public boolean useEncryption() {
        switch (handshakeType) {
            case RTMPConnection.RTMP_ENCRYPTED:
            case RTMPConnection.RTMP_ENCRYPTED_XTEA:
            case RTMPConnection.RTMP_ENCRYPTED_BLOWFISH:
                return true;
        }
        return false;
    }

    private BufFacade generateUnversionedHandshake(byte[] input, byte[] output) {
        log.debug("Using old style (un-versioned) handshake");
        BufFacade outputBuff = BufFacade.buffer((Constants.HANDSHAKE_SIZE * 2) + 1); // 3073
        // non-encrypted
        outputBuff.writeByte(RTMPConnection.RTMP_NON_ENCRYPTED);
        // set server uptime in seconds
        outputBuff.writeInt(0); //0x01
        outputBuff.setIndex(outputBuff.readerIndex(), Constants.HANDSHAKE_SIZE + 1);
        outputBuff.writeBytes(input);

        outputBuff.markReaderIndex();
        outputBuff.skipBytes(1);
        outputBuff.readBytes(output);
        return outputBuff;
    }

    public boolean verifyDigest(int digestPos, byte[] handshakeMessage, byte[] key, int keyLen) {
        byte[] calcDigest = new byte[Constants.DIGEST_LENGTH];
        calculateDigest(digestPos, handshakeMessage, 0, key, keyLen, calcDigest, 0);
        if (!Arrays.equals(Arrays.copyOfRange(handshakeMessage, digestPos, (digestPos + Constants.DIGEST_LENGTH)), calcDigest)) {
            return false;
        }
        return true;
    }

    public int getDigestOffset(int algorithm, byte[] handshake, int bufferOffset) {
        switch (algorithm) {
            case 1:
                return getDigestOffset2(handshake, bufferOffset);
            default:
            case 0:
                return getDigestOffset1(handshake, bufferOffset);
        }
    }

    protected int getDigestOffset2(byte[] handshake, int bufferOffset) {
        bufferOffset += 772;
        int offset = handshake[bufferOffset] & 0xff; // & 0x0ff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = Math.abs((offset % 728) + 776);
        if (res + Constants.DIGEST_LENGTH > 1535) {
            log.error("Invalid digest offset calc: {}", res);
        }
        return res;
    }

    protected int getDigestOffset1(byte[] handshake, int bufferOffset) {
        bufferOffset += 8;
        int offset = handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = (offset % 728) + 12;
        if (res + Constants.DIGEST_LENGTH > 771) {
            log.error("Invalid digest offset calc: {}", res);
        }
        return res;
    }

    protected static final int KEY_LENGTH = 128;

    public int getDHOffset(int algorithm, byte[] handshake, int bufferOffset) {
        switch (algorithm) {
            case 1:
                return getDHOffset2(handshake, bufferOffset);
            default:
            case 0:
                return getDHOffset1(handshake, bufferOffset);
        }
    }

    protected int getDHOffset1(byte[] handshake, int bufferOffset) {
        bufferOffset += 1532;
        int offset = handshake[bufferOffset] & 0xff; // & 0x0ff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = (offset % 632) + 772;
        if (res + KEY_LENGTH > 1531) {
            log.error("Invalid DH offset");
        }
        return res;
    }

    protected int getDHOffset2(byte[] handshake, int bufferOffset) {
        bufferOffset += 768;
        int offset = handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        bufferOffset++;
        offset += handshake[bufferOffset] & 0xff;
        int res = (offset % 632) + 8;
        if (res + KEY_LENGTH > 767) {
            log.error("Invalid DH offset");
        }
        return res;
    }

    byte[] createHandshakeBytesForS1() {
        BufFacade<Object> buffer = BufFacade.buffer(Constants.HANDSHAKE_SIZE);
        // s1 time
        buffer.writeInt(0);
        // s1 zero
        //buffer.writeInt(0);
        buffer.writeByte(4);
        buffer.writeByte(0);
        buffer.writeByte(0);
        buffer.writeByte(1);
        // s1 random bytes
        buffer.writeBytes(Tools.generateRandomData(Constants.HANDSHAKE_SIZE - 8));
        return buffer.array();
    }

    public void calculateDigest(int digestPosServer, byte[] handshakeMessage, int handshakeOffset, byte[] key, int keyLen, byte[] digest, int digestOffset) {
        int messageLen = Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH; // 1504
        byte[] message = new byte[messageLen];
        // copy bytes from handshake message starting at handshake offset into message start at index 0 and up-to digest position length
        System.arraycopy(handshakeMessage, handshakeOffset, message, 0, digestPosServer);
        // copy bytes from handshake message starting at handshake offset plus digest position plus digest length
        // into message start at digest position and up-to message length minus digest position
        System.arraycopy(handshakeMessage, handshakeOffset + digestPosServer + Constants.DIGEST_LENGTH, message, digestPosServer, messageLen - digestPosServer);
        //以上将除了digest的全部写入message
        calculateHMAC_SHA256(message, 0, messageLen, key, keyLen, digest, digestOffset);
    }

    public void calculateHMAC_SHA256(byte[] message, int messageOffset, int messageLen, byte[] key, int keyLen, byte[] digest, int digestOffset) {
        byte[] calcDigest;
        try {
            Mac hmac = Mac.getInstance("Hmac-SHA256", BouncyCastleProvider.PROVIDER_NAME);
            hmac.init(new SecretKeySpec(Arrays.copyOf(key, keyLen), "HmacSHA256"));
            byte[] actualMessage = Arrays.copyOfRange(message, messageOffset, messageOffset + messageLen);
            calcDigest = hmac.doFinal(actualMessage);
            System.arraycopy(calcDigest, 0, digest, digestOffset, Constants.DIGEST_LENGTH);
        } catch (InvalidKeyException e) {
            log.error("Invalid key", e);
        } catch (Exception e) {
            log.error("Hash calculation failed", e);
        }
    }

    /**
     * "Second Oakley Default Group" from RFC2409, section 6.2.
     */
    protected static final byte[] DH_MODULUS_BYTES = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f,
            (byte) 0xda, (byte) 0xa2, (byte) 0x21, (byte) 0x68, (byte) 0xc2, (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b,
            (byte) 0x80, (byte) 0xdc, (byte) 0x1c, (byte) 0xd1, (byte) 0x29, (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a, (byte) 0x67,
            (byte) 0xcc, (byte) 0x74, (byte) 0x02, (byte) 0x0b, (byte) 0xbe, (byte) 0xa6, (byte) 0x3b, (byte) 0x13, (byte) 0x9b, (byte) 0x22,
            (byte) 0x51, (byte) 0x4a, (byte) 0x08, (byte) 0x79, (byte) 0x8e, (byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95,
            (byte) 0x19, (byte) 0xb3, (byte) 0xcd, (byte) 0x3a, (byte) 0x43, (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d,
            (byte) 0xf2, (byte) 0x5f, (byte) 0x14, (byte) 0x37, (byte) 0x4f, (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d, (byte) 0x51,
            (byte) 0xc2, (byte) 0x45, (byte) 0xe4, (byte) 0x85, (byte) 0xb5, (byte) 0x76, (byte) 0x62, (byte) 0x5e, (byte) 0x7e, (byte) 0xc6,
            (byte) 0xf4, (byte) 0x4c, (byte) 0x42, (byte) 0xe9, (byte) 0xa6, (byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff,
            (byte) 0x5c, (byte) 0xb6, (byte) 0xf4, (byte) 0x06, (byte) 0xb7, (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb,
            (byte) 0x5a, (byte) 0x89, (byte) 0x9f, (byte) 0xa5, (byte) 0xae, (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c, (byte) 0x4b,
            (byte) 0x1f, (byte) 0xe6, (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xec, (byte) 0xe6, (byte) 0x53, (byte) 0x81,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    protected static final BigInteger DH_MODULUS = new BigInteger(1, DH_MODULUS_BYTES);
    protected static final BigInteger DH_BASE = BigInteger.valueOf(2);
    private KeyAgreement keyAgreement;

    protected KeyPair generateKeyPair() {
        KeyPair keyPair = null;
        DHParameterSpec keySpec = new DHParameterSpec(DH_MODULUS, DH_BASE);
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(keySpec);
            keyPair = keyGen.generateKeyPair();
            keyAgreement = KeyAgreement.getInstance("DH");
            // key agreement is initialized with "this" ends private key
            keyAgreement.init(keyPair.getPrivate());
        } catch (Exception e) {
            log.error("Error generating keypair", e);
        }
        return keyPair;
    }

    protected byte[] getPublicKey(KeyPair keyPair) {
        DHPublicKey incomingPublicKey = (DHPublicKey) keyPair.getPublic();
        BigInteger dhY = incomingPublicKey.getY();
        if (log.isDebugEnabled()) {
            log.debug("Public key: {}", Hex.encodeHexString(BigIntegers.asUnsignedByteArray(dhY)));
        }
        return Arrays.copyOfRange(BigIntegers.asUnsignedByteArray(dhY), 0, KEY_LENGTH);
    }

    protected byte[] getSharedSecret(byte[] publicKeyBytes, KeyAgreement agreement) {
        BigInteger otherPublicKeyInt = new BigInteger(1, publicKeyBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            KeySpec otherPublicKeySpec = new DHPublicKeySpec(otherPublicKeyInt, DH_MODULUS, DH_BASE);
            PublicKey otherPublicKey = keyFactory.generatePublic(otherPublicKeySpec);
            agreement.doPhase(otherPublicKey, true);
        } catch (Exception e) {
            log.error("Exception getting the shared secret", e);
        }
        byte[] sharedSecret = agreement.generateSecret();
        log.debug("Shared secret [{}]: {}", sharedSecret.length, Hex.encodeHexString(sharedSecret));
        return sharedSecret;
    }


    private Cipher cipherOut;
    private Cipher cipherIn;

    protected void initRC4Encryption(byte[] sharedSecret) {
        log.debug("Shared secret: {}", Hex.encodeHexString(sharedSecret));
        // create output cipher
        log.debug("Outgoing public key [{}]: {}", outgoingPublicKey.length, Hex.encodeHexString(outgoingPublicKey));
        byte[] rc4keyOut = new byte[32];
        // digest is 32 bytes, but our key is 16
        calculateHMAC_SHA256(outgoingPublicKey, 0, outgoingPublicKey.length, sharedSecret, KEY_LENGTH, rc4keyOut, 0);
        log.debug("RC4 Out Key: {}", Hex.encodeHexString(Arrays.copyOfRange(rc4keyOut, 0, 16)));
        try {
            cipherOut = Cipher.getInstance("RC4");
            cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rc4keyOut, 0, 16, "RC4"));
        } catch (Exception e) {
            log.warn("Encryption cipher creation failed", e);
        }
        // create input cipher
        log.debug("Incoming public key [{}]: {}", incomingPublicKey.length, Hex.encodeHexString(incomingPublicKey));
        // digest is 32 bytes, but our key is 16
        byte[] rc4keyIn = new byte[32];
        calculateHMAC_SHA256(incomingPublicKey, 0, incomingPublicKey.length, sharedSecret, KEY_LENGTH, rc4keyIn, 0);
        log.debug("RC4 In Key: {}", Hex.encodeHexString(Arrays.copyOfRange(rc4keyIn, 0, 16)));
        try {
            cipherIn = Cipher.getInstance("RC4");
            cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rc4keyIn, 0, 16, "RC4"));
        } catch (Exception e) {
            log.warn("Decryption cipher creation failed", e);
        }
    }


    public Cipher getCipherIn() {
        return cipherIn;
    }

    public Cipher getCipherOut() {
        return cipherOut;
    }

    private boolean unvalidatedConnectionAllowed = true;

    public boolean decodeClientRequest2(BufFacade in) {
        if (log.isTraceEnabled()) {
            log.debug("decodeClientRequest2: {}", Hex.encodeHexString(in.array()));
        }
        byte[] c2 = new byte[Constants.HANDSHAKE_SIZE];
        in.readBytes(c2);

        // 在client1中做过判断
        if (fp9Handshake) {
            // client signature c2[HANDSHAKE_SIZE - DIGEST_LENGTH]
            byte[] digest = new byte[Constants.DIGEST_LENGTH];
            byte[] signature = new byte[Constants.DIGEST_LENGTH];
            log.debug("Client sent signature: {}", Hex.encodeHexString(Arrays.copyOfRange(c2, (Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH), (Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH) + Constants.DIGEST_LENGTH)));
            // verify client response
            calculateHMAC_SHA256(s1, digestPosServer, Constants.DIGEST_LENGTH, GENUINE_FP_KEY, GENUINE_FP_KEY.length, digest, 0);
            calculateHMAC_SHA256(c2, 0, Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH, digest, Constants.DIGEST_LENGTH, signature, 0);
            if (useEncryption()) {
                switch (handshakeType) {
                    case RTMPConnection.RTMP_ENCRYPTED_XTEA:
                        log.debug("RTMPE type 8 XTEA");
                        // encrypt signature
                        for (int i = 0; i < Constants.DIGEST_LENGTH; i += 8) {
                            //encryptXtea(signature, i, digest[i] % 15);
                        }
                        break;
                    case RTMPConnection.RTMP_ENCRYPTED_BLOWFISH:
                        log.debug("RTMPE type 9 Blowfish");
                        // encrypt signature
                        for (int i = 0; i < Constants.DIGEST_LENGTH; i += 8) {
                            //encryptBlowfish(signature, i, digest[i] % 15);
                        }
                        break;
                }
                // update 'encoder / decoder state' for the RC4 keys both parties *pretend* as if handshake part 2 (1536 bytes) was encrypted
                // effectively this hides / discards the first few bytes of encrypted session which is known to increase the secure-ness of RC4
                // RC4 state is just a function of number of bytes processed so far that's why we just run 1536 arbitrary bytes through the keys below
                byte[] dummyBytes = new byte[Constants.HANDSHAKE_SIZE];
                cipherIn.update(dummyBytes);
                cipherOut.update(dummyBytes);
            }
            // show some information
            if (log.isDebugEnabled()) {
                log.debug("Digest key: {}", Hex.encodeHexString(digest));
                log.debug("Signature calculated: {}", Hex.encodeHexString(signature));
            }
            byte[] sentSignature = Arrays.copyOfRange(c2, (Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH), (Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH) + Constants.DIGEST_LENGTH);
            if (log.isDebugEnabled()) {
                log.debug("Client sent signature: {}", Hex.encodeHexString(sentSignature));
            }
            if (!Arrays.equals(signature, sentSignature)) {
                log.warn("Client not compatible");
                if (unvalidatedConnectionAllowed) {
                    // accept and unvalidated handshake; used to deal with ffmpeg
                    //为了处理ffmpeg,可以接受未验证的握手
                    log.debug("Unvalidated client allowed to proceed");
                    return true;
                } else {
                    return false;
                }
            } else {
                log.debug("Compatible client, handshake complete");
            }
        } else {
            if (!Arrays.equals(s1, c2)) {
                log.info("Client signature doesn't match!");
            }
        }
        return true;
    }

    private byte[] swfVerificationBytes;

    public void calculateSwfVerification(byte[] handshakeMessage, byte[] swfHash, int swfSize) {
        // SHA256 HMAC hash of decompressed SWF, key are the last 32 bytes of the server handshake
        byte[] swfHashKey = new byte[Constants.DIGEST_LENGTH];
        System.arraycopy(handshakeMessage, Constants.HANDSHAKE_SIZE - Constants.DIGEST_LENGTH, swfHashKey, 0, Constants.DIGEST_LENGTH);
        byte[] bytesFromServerHash = new byte[Constants.DIGEST_LENGTH];
        calculateHMAC_SHA256(swfHash, 0, swfHash.length, swfHashKey, Constants.DIGEST_LENGTH, bytesFromServerHash, 0);
        // construct SWF verification pong payload
        ByteBuffer swfv = ByteBuffer.allocate(42);
        swfv.put((byte) 0x01);
        swfv.put((byte) 0x01);
        swfv.putInt(swfSize);
        swfv.putInt(swfSize);
        swfv.put(bytesFromServerHash);
        swfv.flip();
        swfVerificationBytes = new byte[42];
        swfv.get(swfVerificationBytes);
        log.debug("initialized swf verification response from swfSize: {} swfHash:\n{}\n{}", swfSize, Hex.encodeHexString(swfHash), Hex.encodeHexString(swfVerificationBytes));
    }
}
