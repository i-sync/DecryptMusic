package com.example.decryptmusic.Utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.media.AudioAttributesCompat;

public class MediaPlayUtil {
    private static final String TAG = MediaPlayUtil.class.getSimpleName();

    private static AudioAttributesCompat CO = new AudioAttributesCompat.Builder().setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC).setUsage(AudioAttributesCompat.USAGE_MEDIA).build();
    private AudioAttributes audioAttributes = new AudioAttributes.Builder().setContentType(CO.getContentType()).setFlags(CO.getFlags()).setUsage(CO.getUsage()).build();

    private FileOutputStream fos;
    private BufferedOutputStream bos;

    private String sourcePath;
    private String destPath;
    private String sourceFilePath;
    private String destFilePath;
    private DecryptUtil mDecryptUtil = null;

    public boolean mRunning = false;
    public int mDuration = 0;


    private byte[] mByte = new byte[32];

    private AudioTrack mAudioTrack;
    private ByteBuffer mByteBuffer;
    private Handler mHandler = new Handler();

    public TextView txt_info;

    public MediaPlayUtil(String sourcePath, String destPath)
    {
        this.sourcePath = sourcePath;
        this.destPath = destPath;
    }

    public MediaPlayUtil(String sourcePath, String destPath, String mayday)
    {
        this.sourcePath = sourcePath;
        this.destPath = destPath;
        mDecryptUtil = new DecryptUtil(mayday);
    }

    public void setMayday(String mayday)
    {
        mDecryptUtil = new DecryptUtil(mayday);
    }

    public void setFileName(String fileName)
    {
        this.sourceFilePath = this.sourcePath + fileName;
        this.destFilePath = this.destPath + fileName;
    }

    private void initDecryptAudioStream()
    {
        //init decrypt stream
        try {
            File destFile = new File(destFilePath);
            if(!destFile.exists())
            {
                destFile.createNewFile();
            }
            fos = new FileOutputStream(new File(destFilePath));
            bos = new BufferedOutputStream(fos);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e("MediaPlayUtil", e.getMessage());
        }
    }

    private void releaseDecryptAudioStream()
    {
        try {
            if (bos != null) {
                bos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    bos=null;
                }
            }
        }

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            fos=null;
        }
    }

    private void releaseAudioTrack()
    {
        if(mAudioTrack == null)
            return;
        mAudioTrack.stop();
        mAudioTrack = null;
    }

    public void doPlayDecoder()
    {
        // step 1：创建一个媒体分离器
        MediaExtractor extractor = new MediaExtractor();

        // step 2：为媒体分离器装载媒体文件路径
        try {
            extractor.setDataSource(sourceFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // step 3：获取并选中指定类型的轨道
        // 媒体文件中的轨道数量 （一般有视频，音频，字幕等）
        int trackCount = extractor.getTrackCount();
        // mime type 指示需要分离的轨道类型
        String extractMimeType = "audio/";
        MediaFormat trackFormat = null;
        // 记录轨道索引id，MediaExtractor 读取数据之前需要指定分离的轨道索引
        int trackID = -1;
        for (int i = 0; i < trackCount; i++) {
            trackFormat = extractor.getTrackFormat(i);
            if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(extractMimeType)) {
                trackID = i;
                break;
            }
        }
        // 媒体文件中存在指定轨道
        // step 4：选中指定类型的轨道
        if (trackID != -1)
            extractor.selectTrack(trackID);

        // step 5：根据 MediaFormat 创建解码器
        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(trackFormat,null,null,0);
            mediaCodec.start();

            this.mDuration = (int)(getLongFormatInfo(trackFormat, "durationUs") / 1000);
            int sampleRate = getIntFormatInfo(trackFormat, "sample-rate", 44100);
            int channelCount = getIntFormatInfo(trackFormat, "channel-count", 2) == 2? 12:4;
            int pcmEncoding = getIntFormatInfo(trackFormat, "pcm-encoding", 2);

            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelCount, pcmEncoding);
            AudioFormat build = new AudioFormat.Builder().setEncoding(pcmEncoding).setSampleRate(sampleRate).setChannelMask(channelCount).build();
            this.mAudioTrack = new AudioTrack.Builder().setAudioAttributes(this.audioAttributes).setAudioFormat(build).setBufferSizeInBytes(minBufferSize).setTransferMode(AudioTrack.MODE_STREAM).build();

            this.mAudioTrack.play();
            this.mAudioTrack.flush();

            this.mByteBuffer = ByteBuffer.allocateDirect(minBufferSize);
            this.mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // step 6: 向解码器喂入数据
        // step 7: 从解码器吐出数据
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        final
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int flags = 0;
        do {
            if(flags == 0) {
                int inIndex = mediaCodec.dequeueInputBuffer(10000);
                if(inIndex >= 0) {
                    mByteBuffer.clear();
                    int sampleDataSize = extractor.readSampleData(mByteBuffer, 0);
                    if(sampleDataSize < 0) {
                        Log.i(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, 4);
                        flags = 1;
                    }
                    else {
                        mByteBuffer.rewind().position(0).limit(sampleDataSize);
                        mDecryptUtil.m(mByteBuffer);
                        mByteBuffer.get(mByte, 0, mByte.length);
                        mByteBuffer.clear();
                        ByteBuffer codecInputBuffer = inputBuffers[inIndex];
                        codecInputBuffer.clear();
                        for(int i = 0; i < sampleDataSize; ++i) {
                            codecInputBuffer.put(i, mByteBuffer.get(i));
                        }

                        mediaCodec.queueInputBuffer(inIndex, 0, sampleDataSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }


            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            switch(outIndex) {
                case -3: {
                    Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = mediaCodec.getOutputBuffers();
                    break;
                }
                case -2: {
                    MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    Log.i(TAG, "New format " + mediaFormat);
                    mAudioTrack.setPlaybackRate(mediaFormat.getInteger("sample-rate"));
                    break;
                }
                case -1: {
                    Log.i(TAG, "dequeueOutputBuffer timed out!");
                    break;
                }
                default: {
                    ByteBuffer outputBuffer = outputBuffers[outIndex];
                    mAudioTrack.write(outputBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING, extractor.getSampleTime());
                    outputBuffer.clear();
                    mediaCodec.releaseOutputBuffer(outIndex, false);
                    break;
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    txt_info.setText(String.format("正在解码中...pts:%s", bufferInfo.presentationTimeUs / 1000));
                }
            });
        }
        while((bufferInfo.flags & 4) == 0 && mRunning);

        Log.i(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");

        // step 8: 释放资源
        // 释放分离器，释放后 extractor 将不可用
        extractor.release();
        // 释放解码器
        mediaCodec.release();

        //releaseAudioTrack
        releaseAudioTrack();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txt_info.setText("解码完成");
            }
        });
    }

    public void doDecryptDecoder()
    {
        // init file stream
        initDecryptAudioStream();

        // step 1：创建一个媒体分离器
        MediaExtractor extractor = new MediaExtractor();

        // step 2：为媒体分离器装载媒体文件路径
        try {
            extractor.setDataSource(sourceFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // step 3：获取并选中指定类型的轨道
        // 媒体文件中的轨道数量 （一般有视频，音频，字幕等）
        int trackCount = extractor.getTrackCount();
        // mime type 指示需要分离的轨道类型
        String extractMimeType = "audio/";
        MediaFormat trackFormat = null;
        // 记录轨道索引id，MediaExtractor 读取数据之前需要指定分离的轨道索引
        int trackID = -1;
        for (int i = 0; i < trackCount; i++) {
            trackFormat = extractor.getTrackFormat(i);
            if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(extractMimeType)) {
                trackID = i;
                break;
            }
        }
        // 媒体文件中存在指定轨道
        // step 4：选中指定类型的轨道
        if (trackID != -1)
            extractor.selectTrack(trackID);

        // step 5：根据 MediaFormat 创建解码器
        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(trackFormat,null,null,0);
            mediaCodec.start();

            this.mDuration = (int)(getLongFormatInfo(trackFormat, "durationUs") / 1000);
            int sampleRate = getIntFormatInfo(trackFormat, "sample-rate", 44100);
            int channelCount = getIntFormatInfo(trackFormat, "channel-count", 2) == 2? 12:4;
            int pcmEncoding = getIntFormatInfo(trackFormat, "pcm-encoding", 2);

            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelCount, pcmEncoding);
            AudioFormat build = new AudioFormat.Builder().setEncoding(pcmEncoding).setSampleRate(sampleRate).setChannelMask(channelCount).build();
            this.mAudioTrack = new AudioTrack.Builder().setAudioAttributes(this.audioAttributes).setAudioFormat(build).setBufferSizeInBytes(minBufferSize).setTransferMode(AudioTrack.MODE_STREAM).build();

            this.mAudioTrack.play();
            this.mAudioTrack.flush();

            this.mByteBuffer = ByteBuffer.allocateDirect(minBufferSize);
            this.mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // step 6: 向解码器喂入数据
        // step 7: 从解码器吐出数据
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        final
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int flags = 0;
        do {
            if(flags == 0) {
                int inIndex = mediaCodec.dequeueInputBuffer(10000);
                if(inIndex >= 0) {
                    mByteBuffer.clear();
                    int sampleDataSize = extractor.readSampleData(mByteBuffer, 0);
                    if(sampleDataSize < 0) {
                        Log.i(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, 4);
                        flags = 1;
                    }
                    else {
                        mByteBuffer.rewind().position(0).limit(sampleDataSize);
                        mDecryptUtil.m(mByteBuffer);
                        mByteBuffer.get(mByte, 0, mByte.length);
                        mByteBuffer.clear();
                        ByteBuffer codecInputBuffer = inputBuffers[inIndex];
                        codecInputBuffer.clear();
                        for(int i = 0; i < sampleDataSize; ++i) {
                            codecInputBuffer.put(i, mByteBuffer.get(i));
                        }

                        //write data to file
                        byte[] tmpByte = new byte[sampleDataSize];
                        codecInputBuffer.get(tmpByte,0, sampleDataSize);
                        try {
                            bos.write(tmpByte);
                        }
                        catch (IOException e)
                        {
                            Log.e("BOS ERROR", e.getMessage());
                        }
                        //end

                        mediaCodec.queueInputBuffer(inIndex, 0, sampleDataSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }


            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            switch(outIndex) {
                case -3: {
                    Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = mediaCodec.getOutputBuffers();
                    break;
                }
                case -2: {
                    MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    Log.i(TAG, "New format " + mediaFormat);
                    mAudioTrack.setPlaybackRate(mediaFormat.getInteger("sample-rate"));
                    break;
                }
                case -1: {
                    Log.i(TAG, "dequeueOutputBuffer timed out!");
                    break;
                }
                default: {
                    ByteBuffer outputBuffer = outputBuffers[outIndex];
                    //mAudioTrack.write(outputBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING, extractor.getSampleTime());
                    outputBuffer.clear();

                    mediaCodec.releaseOutputBuffer(outIndex, false);
                    break;
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    txt_info.setText(String.format("正在解码中...pts:%s", bufferInfo.presentationTimeUs / 1000));
                }
            });
        }
        while((bufferInfo.flags & 4) == 0 && mRunning);

        Log.i(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
        // step 8: 释放资源
        releaseDecryptAudioStream();
        // 释放分离器，释放后 extractor 将不可用
        extractor.release();
        // 释放解码器
        mediaCodec.release();

        releaseAudioTrack();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txt_info.setText("解码完成");
            }
        });
    }

    public void doNormalDecoder()
    {
        // step 1：创建一个媒体分离器
        MediaExtractor extractor = new MediaExtractor();

        // step 2：为媒体分离器装载媒体文件路径
        try {
            extractor.setDataSource(destFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // step 3：获取并选中指定类型的轨道
        // 媒体文件中的轨道数量 （一般有视频，音频，字幕等）
        int trackCount = extractor.getTrackCount();
        // mime type 指示需要分离的轨道类型
        String extractMimeType = "audio/";
        MediaFormat trackFormat = null;
        // 记录轨道索引id，MediaExtractor 读取数据之前需要指定分离的轨道索引
        int trackID = -1;
        for (int i = 0; i < trackCount; i++) {
            trackFormat = extractor.getTrackFormat(i);
            if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(extractMimeType)) {
                trackID = i;
                break;
            }
        }
        // 媒体文件中存在指定轨道
        // step 4：选中指定类型的轨道
        if (trackID != -1)
            extractor.selectTrack(trackID);

        // step 5：根据 MediaFormat 创建解码器
        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(trackFormat,null,null,0);
            mediaCodec.start();

            this.mDuration = (int)(getLongFormatInfo(trackFormat, "durationUs") / 1000);
            int sampleRate = getIntFormatInfo(trackFormat, "sample-rate", 44100);
            int channelCount = getIntFormatInfo(trackFormat, "channel-count", 2) == 2? 12:4;
            int pcmEncoding = getIntFormatInfo(trackFormat, "pcm-encoding", 2);

            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelCount, pcmEncoding);
            AudioFormat build = new AudioFormat.Builder().setEncoding(pcmEncoding).setSampleRate(sampleRate).setChannelMask(channelCount).build();
            this.mAudioTrack = new AudioTrack.Builder().setAudioAttributes(this.audioAttributes).setAudioFormat(build).setBufferSizeInBytes(minBufferSize).setTransferMode(AudioTrack.MODE_STREAM).build();

            this.mAudioTrack.play();
            this.mAudioTrack.flush();

            this.mByteBuffer = ByteBuffer.allocateDirect(minBufferSize);
            this.mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // step 6: 向解码器喂入数据
        // step 7: 从解码器吐出数据
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        final
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int flags = 0;
        do {
            if(flags == 0) {
                int inIndex = mediaCodec.dequeueInputBuffer(10000);
                if(inIndex >= 0) {
                    ByteBuffer codecInputBuffer = inputBuffers[inIndex];
                    codecInputBuffer.clear();
                    int sampleDataSize = extractor.readSampleData(codecInputBuffer, 0);
                    if(sampleDataSize < 0) {
                        Log.i(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, 4);
                        flags = 1;
                    }
                    else {
                        mediaCodec.queueInputBuffer(inIndex, 0, sampleDataSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }


            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            switch(outIndex) {
                case -3: {
                    Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = mediaCodec.getOutputBuffers();
                    break;
                }
                case -2: {
                    MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    Log.i(TAG, "New format " + mediaFormat);
                    mAudioTrack.setPlaybackRate(mediaFormat.getInteger("sample-rate"));
                    break;
                }
                case -1: {
                    Log.i(TAG, "dequeueOutputBuffer timed out!");
                    break;
                }
                default: {
                    ByteBuffer outputBuffer = outputBuffers[outIndex];
                    mAudioTrack.write(outputBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING, extractor.getSampleTime());
                    outputBuffer.clear();
                    mediaCodec.releaseOutputBuffer(outIndex, false);
                    break;
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    txt_info.setText(String.format("正在解码中...pts:%s", bufferInfo.presentationTimeUs / 1000));
                }
            });
        }
        while((bufferInfo.flags & 4) == 0 && mRunning);

        Log.i(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");

        // step 8: 释放资源
        // 释放分离器，释放后 extractor 将不可用
        extractor.release();
        // 释放解码器
        mediaCodec.release();

        releaseAudioTrack();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txt_info.setText("解码完成");
            }
        });
    }

    public static int getIntFormatInfo(MediaFormat mediaFormat, String strKey, int defaultValue) {
        try {
            return mediaFormat.containsKey(strKey)? mediaFormat.getInteger(strKey) : defaultValue;
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static long getLongFormatInfo(MediaFormat mediaFormat, String strKey) {
        try {
            return mediaFormat.containsKey(strKey) ? mediaFormat.getLong(strKey) : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
