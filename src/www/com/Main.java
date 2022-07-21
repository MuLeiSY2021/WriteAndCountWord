package www.com;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    //文本文件数量
    static final int FILE_NUM = 10;
    //单个文件英文字母数量
    static final int EN_WORDS_COUNT = 10000;
    //文本文件夹地址
    static final String TGR_FILE = "src/enWordTexts/";
    //文本文件命名格式
    static final String FORMAT = "text";
    //文本文件类型
    static final String TYPE = ".txt";

    public static void main(String[] args) {
        // 作业：
        //开10个线程，每个线程随机往一个文件中随机写入10000字母（10个文件），

        try {
            //启动写入文件线程
            ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                    10,
                    FILE_NUM,
                    60,TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(10));
            poolExecutor.prestartAllCoreThreads();

            //提交写入任务
            for (int i = 0; i < FILE_NUM; i++) {
                poolExecutor.submit(new WritePart(Integer.toString(i)));
            }

            //时线程池关闭
            poolExecutor.shutdown();

            //等待线程池执行结束后执行后续代码
            if(poolExecutor.awaitTermination(60,TimeUnit.SECONDS)){
                System.out.println("ERROR!");
            }

            //提示语
            if(poolExecutor.isShutdown()) {
                System.out.println("Write All Complete");
            }

            //开启读取线程
            Thread thread = new Thread(new CountAllFileEnWord(FILE_NUM));
            thread.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getTgrFile() {
        return TGR_FILE;
    }

    public static int getEnWordsCount() {
        return EN_WORDS_COUNT;
    }

    public static String getFORMAT() {
        return FORMAT;
    }

    public static String getTYPE() {
        return TYPE;
    }
}
