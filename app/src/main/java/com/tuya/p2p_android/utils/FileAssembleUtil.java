package com.tuya.p2p_android.utils;


import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.tuya.p2p.Download_Album_Head;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @ClassName FileAssembleUtil
 * @Description P2P数据拼接类 图片 视频
 * @Author faker
 * @Date 09/07/21 2:58 PM
 * @Version 1.0
 *
 **/

public class FileAssembleUtil {

    public static FileAssembleUtil getInstance(String videoFileDir,  String imageFileDir){
        return new FileAssembleUtil(videoFileDir, imageFileDir);
    }

    private FileAssembleUtil(String videoFileDir, String imageFileDir) {
        this.videoFileDir = videoFileDir;
        this.imageFileDir = imageFileDir;
    }


    private static final String TAG = "FileAssembleUtil";


    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    //用户记录该类最后一次处理数据的时间
    public long lastWorkTime;

    //记录当前读取长度
    private int currentReadLength;

    //文件后缀
    private String fileSuffix;
    //视频路径
    private String videoFileDir;
    //图片路径
    private String imageFileDir;

    //最后文件输出路径
    private String finalFileDir;

    //最后文件输出名称
    private String finalFileName;

    // 图片所属的用户ID
    private String userId;

    public static final int STATE_IDLE = 0;
    public static final int STATE_START = 1;
    public static final int STATE_PROCESS = 2;
    public static final int STATE_ONCE = 3;
    public static final int STATE_DONE = 4;
    public static final int STATE_CANCEL = 5;

    public static final int KEY_FILE_TYPE_VIDEO = 1;
    public static final int KEY_FILE_TYPE_AUDIO = 2;
    public static final int KEY_FILE_TYPE_IMAGE = 3;

    //刷新用
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };

    /**
     * 处理数据
     * 注意 ：命令跟数据是异步两个线程
     * @param state 接受状态
     * @param data 数据
     */
    public void handlerData(int state, byte[] data, Download_Album_Head downloadAlbumHead, String userId) {
       this.userId = userId;
        switch (state) {
            case STATE_START:
                Log.v(TAG, "handlerData: new file start.");
                currentReadLength = 0;//必须
                startAssemble(data, downloadAlbumHead);
                break;
            case STATE_PROCESS:
                processAssemble(data, downloadAlbumHead);
                break;
            case STATE_ONCE:
                onceAssemble(data, "", downloadAlbumHead);
                break;
            case STATE_DONE:
                Log.v(TAG, "handlerData: receive file finish");
                int fileType = TextUtils.equals(finalFileDir, videoFileDir)?KEY_FILE_TYPE_VIDEO:KEY_FILE_TYPE_IMAGE;
                endAssemble(fileType,userId ,downloadAlbumHead, data);
                break;
            case STATE_CANCEL:
                //do something
                Log.v(TAG, "handlerData: cancel receive file.");
                fileType = TextUtils.equals(finalFileDir, videoFileDir)?KEY_FILE_TYPE_VIDEO:KEY_FILE_TYPE_IMAGE;
                cancelAssemble(fileType,userId ,downloadAlbumHead, data);
                break;
        }
        lastWorkTime = System.currentTimeMillis();
    }


    private void startAssemble(byte[] data, Download_Album_Head downloadAlbumHead){
        Log.d(TAG, "startAssemble >> file mete:" + downloadAlbumHead.toString());
        if(TextUtils.isEmpty(downloadAlbumHead.fileName)){
            Log.e(TAG, "startAssemble: app send file name is nulLog.");
            return;
        }

        //后缀
        try {
            fileSuffix = downloadAlbumHead.fileName.substring(downloadAlbumHead.fileName.lastIndexOf("."));
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.d(TAG, "startAssemble:  fileSuffix :" + fileSuffix);
        writeToFile(false , data, 0, data.length);

    }

    private void processAssemble(byte[] data, Download_Album_Head downloadAlbumHead){
        if (isValidAssemble()) {
            writeToFile(true , data, 0, data.length);
        }else {
            Log.d(TAG, "processAssemble: Invalid data. >> reqId :" + downloadAlbumHead.reqId);
        }

    }

    private void onceAssemble(byte[] data, String userId, Download_Album_Head downloadAlbumHead){
        if(TextUtils.isEmpty(downloadAlbumHead.fileName)){
            Log.e(TAG, "onceAssemble:  app send file name is nulLog.");
            return;
        }
        //后缀
        fileSuffix = downloadAlbumHead.fileName.substring(downloadAlbumHead.fileName.lastIndexOf("."));
        Log.d(TAG, "onceAssemble: fileSuffix :" + fileSuffix);
        writeToFile(false, data, 0, data.length);

        // 判断文件接受的完整性，文件的长度? fileEnd? 标志
        if (currentReadLength == downloadAlbumHead.fileSize) {
            // TODO: 2021/10/20  文件接受完成, 写入数据库
        }

        fileSuffix = null;
        finalFileDir = null;
        finalFileName = null;

    }

    private void endAssemble(int fileType, String userId,Download_Album_Head downloadAlbumHead, byte[] data){
        if (!isValidAssemble()) {
            Log.e(TAG, "endAssemble: Invalid data. >> reqId :" + downloadAlbumHead.reqId);
            return;
        }
        Log.d(TAG, "endAssemble: userID >>" + userId + ", fileName:" + downloadAlbumHead.fileName);
        writeToFile(true , data, 0, data.length);
        // 判断文件接受的完整性，文件的长度?fileEnd? 标志
        release();//最后用完刷新，并释放
        if (currentReadLength == downloadAlbumHead.fileSize) {
            // TODO: 2021/10/20  文件接受完成, 写入数据库
        }

        fileSuffix = null;
        finalFileDir = null;
        finalFileName = null;

    }

    private void cancelAssemble(int fileType, String userId, Download_Album_Head downloadAlbumHead, byte[] data){
        if (!isValidAssemble()) {
            Log.e(TAG, "endAssemble: Invalid data. >> reqId :" + downloadAlbumHead.reqId);
            return;
        }
        writeToFile(true, data, 0, data.length);
        Log.d(TAG, "cancelAssemble: already receive length:"+ currentReadLength + "; total length:" + downloadAlbumHead.fileSize);
        release();//最后用完刷新，并释放
        if(currentReadLength != downloadAlbumHead.fileSize){
            //not receive all data of the file,delete half file
            if (!TextUtils.isEmpty(finalFileName)) {
                File file = new File(finalFileDir, finalFileName);
                Log.d(TAG, "handlerData: 取消传输最后文件本地实际长度： "+ file.length() + ";p2p数据长度: " + downloadAlbumHead.fileSize);
                file.delete();
            }
        }else {
            // TODO: 2021/10/20  传输取消， 写入数据库
        }

        fileSuffix = null;
        finalFileDir = null;
        finalFileName = null;

    }

    public void release(){
        BufferedOutputStream bufferOut = bos;
        bos = null;
        if (bufferOut != null) {
            try {
                bufferOut.flush();
                bufferOut.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileOutputStream fileOut = fos;
        fos = null;
        if (fileOut != null) {
            try {
                fileOut.flush();
                fileOut.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        file = null;
    }

    /**
     * 写入文件
     * @param append
     * @param innerData
     * @param off
     * @param len
     */
    //避免每次都创建实例，读写速度慢
    BufferedOutputStream bos = null;
    FileOutputStream fos = null;
    File file = null;
    private void writeToFile(boolean append, byte[] innerData, int off, int len) {
        //记录已读长度
        currentReadLength += len;
        try {
            //首次
            if (!append) {
                byte[] fileHead = new byte[4];
                System.arraycopy(innerData, 0, fileHead, 0 ,fileHead.length);
                int fileType = getFileType(fileHead);
                Log.d(TAG, "writeToFile: app send file type is " + fileType);
                createFileDirAndName(fileType);
            }

            File dir = new File(finalFileDir);
            if (!dir.exists()) {//判断文件目录是否存在
                dir.mkdirs();
            }
            if (file == null) {
                file = new File(finalFileDir, finalFileName);
            }
            if (!file.exists()) {
                file.createNewFile();
            }

            if (currentReadLength >> 4 == 5) {
                Log.v(TAG, "writeToFile-file:" + file);
            }
            if (fos == null) {
                fos = new FileOutputStream(file, true);
            }
//            fos = new FileOutputStream(file, append);
            if (bos == null) {
                bos = new BufferedOutputStream(fos);
            }
            bos.write(innerData, off, len);
            //减少io写入频率
            if (currentReadLength >= 80 * len) {
                bos.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            if (bos != null) {
//                try {
//                    bos.flush();
//                    bos.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//
//            if (fos != null) {
//                try {
//                    fos.flush();
//                    fos.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
        }
    }

    /**
     * 根据fileType创建存储路径和文件名称
     * @param fileType
     */
    private void createFileDirAndName(int fileType) {
        switch (fileType) {
            case KEY_FILE_TYPE_VIDEO:
                finalFileDir = videoFileDir;
                finalFileName = "VIDEO_" + (int)(Math.random()*100) + "_" + System.currentTimeMillis() + fileSuffix;
                break;
            case KEY_FILE_TYPE_IMAGE:
                finalFileDir = imageFileDir;
                finalFileName = "IMAGE_"+ (int)(Math.random()*100) + "_" + System.currentTimeMillis() + fileSuffix;
                break;
        }
        Log.v(TAG, "createFileDirAndName-finalFileDir:" + finalFileDir + ",finalFileName:" + finalFileName);
    }


    /**
     * 传输是否合法，主要是防止接受到脏数据
     * 需要判断文件名及路径是否存在，否则数据无法正确保存
     * @return
     */
    public boolean isValidAssemble() {
        if (TextUtils.isEmpty(finalFileName) || TextUtils.isEmpty(finalFileDir)) {
            return false;
        }
        return true;
    }




    /**
     * 根据byte数组，生成文件
     */
    private void getFile(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                dir.mkdirs();
            }

            file = new File(filePath, fileName);

            if (!file.exists() && file.isFile()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }






    /*** 根据文件路径获取文件头信息
     * @param fileHead
     * 文件路径
     ** @return 文件头信息*/
    public int getFileType(byte[] fileHead) {
        String bytes = bytesToHexString(fileHead);
        Log.d(TAG, "getFileType: file head info is " + bytes);
        if (bytes != null) {
            if (bytes.contains("FFD8FF")) {
//                return "jpg";
                return KEY_FILE_TYPE_IMAGE;//jpg; jpeg
            } else if (bytes.contains("89504E47")) {
//                return "png";
                return KEY_FILE_TYPE_IMAGE;
            } else if (bytes.contains("47494638")) {
//                return "gif";
                return KEY_FILE_TYPE_IMAGE;
            } else if (bytes.contains("49492A00")) {
//                return "tif";
                return KEY_FILE_TYPE_IMAGE;
            } else if (bytes.contains("424D")) {
//                return "bmp";
                return KEY_FILE_TYPE_IMAGE;
            }
        }
        return KEY_FILE_TYPE_VIDEO;
    }

    /**
     * byte数组转换成16进制字符串
     *
     * @param src
     * @return
     */
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            // 以十六进制（基数 16）无符号整数形式返回一个整数参数的字符串表示形式，并转换为大写
            String hv = Integer.toHexString(src[i] & 0xFF).toUpperCase();
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }





}
