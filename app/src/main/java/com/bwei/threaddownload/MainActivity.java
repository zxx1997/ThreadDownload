package com.bwei.threaddownload;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.loopj.android.http.HttpGet;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpHead;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

/**
 * 多线程下载
 */
public class MainActivity extends AppCompatActivity {
    protected static final String TAG = "MainActivity";

    //下载线程的数量
    private final static int threadsize = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void download(View v){
        new Thread(){
            public void run() {
                try {
                     //创建HttpClient对象
                    HttpClient client = new DefaultHttpClient();
                    //http://c.hiphotos.baidu.com/image/pic/item/b90e7bec54e736d1e51217c292504fc2d46269f3.jpg
                    //https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1495040157701&di=e75c9d745a6af178d07e3e3b78e3cb3b&imgtype=0&src=http%3A%2F%2Fimg.pconline.com.cn%2Fimages%2Fupload%2Fupc%2Ftx%2Fwallpaper%2F1502%2F03%2Fc0%2F2703334_1422958313278_800x600.jpg
                    String uri = "http://c.hiphotos.baidu.com/image/pic/item/b90e7bec54e736d1e51217c292504fc2d46269f3.jpg";
                   //从头信息里获取文件大小 不获取内容
                    HttpHead request = new HttpHead(uri);
                    HttpResponse response = client.execute(request);
                    //response  只有响应头  没有响应体
                    if(response.getStatusLine().getStatusCode() == 200){
                        //从响应头里获取长度
                        Header[] headers = response.getHeaders("Content-Length");
                        String value = headers[0].getValue();
                        //一 获取服务器上文件的大小
                        int filelength = Integer.parseInt(value);
                        Log.i(TAG, "filelength:"+filelength);


                        //二 在sdcard创建和服务器大小一样的文件
                        String name = getFileName(uri);
                        File file = new File(Environment.getExternalStorageDirectory(),name);
                        //随机访问文件
                        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                        //设置文件的大小
                        raf.setLength(filelength);
                        //关闭
                        raf.close();

                        //计算每条线程的下载量
                        int block = (filelength%threadsize == 0)?(filelength/threadsize):(filelength/threadsize+1);

                        //开启三条线程执行下载
                        for(int threadid=0;threadid<threadsize;threadid++){
                            new DownloadThread(threadid, uri, file, block).start();
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }.start();
    }
    //线程下载类
    private class DownloadThread extends Thread{
        private int threadid;//线程的id
        private String uri;//下载的地址
        private File file;//下载文件
        private int block;//下载的块
        private int start;
        private int end;

        public DownloadThread(int threadid, String uri, File file, int block) {
            super();
            this.threadid = threadid;
            this.uri = uri;
            this.file = file;
            this.block = block;
            //计算下载的开始位置和结束位置
            start = threadid * block;
            end = (threadid + 1)*block -1;
        }



        //下载   状态码：200是普通的下载      206是分段下载        Range:范围
        @Override
        public void run() {
            super.run();
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                //跳转到起始位置
                raf.seek(start);

                //分段下载真实内容
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(uri);
                request.addHeader("Range", "bytes:"+start+"-"+end);//添加请求头
                HttpResponse response = client.execute(request);
                if(response.getStatusLine().getStatusCode() == 200){
                    InputStream inputStream = response.getEntity().getContent();
                    //把流写入到文件
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while((len = inputStream.read(buffer)) != -1){

                        //写数据
                        raf.write(buffer, 0, len);
                    }
                    inputStream.close();
                    raf.close();
                    Log.i(TAG, "第"+threadid+"条线程下载完成");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private String getFileName(String uri){
        return uri.substring(uri.lastIndexOf("/")+1);
    }
}
