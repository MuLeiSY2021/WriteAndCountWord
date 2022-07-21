package www.com;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static sun.nio.ch.IOStatus.EOF;

public class CountAllFileEnWord implements Runnable, CallbackEnWordCounts {
    private final ConcurrentHashMap<Character, Integer> enWordsCountHashMap = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ThreadPoolExecutor poolExecutor;
    private final int fileNum;

    public CountAllFileEnWord(int fileNum) {
        this.poolExecutor = new ThreadPoolExecutor(
                10,
                fileNum,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        this.fileNum = fileNum;
        //预热哈希表
        for (int i = 0; i < 26 * 2; i++) {
            char c = (char) (i > 25 ? 'A' + i - 26 : 'a' + i);
            this.enWordsCountHashMap.put(c, 0);
        }
    }

    @Override
    public void run() {
        try {
            //然后，统计每个文件中，不同字母的个数。
            //希望的结果是:Map<字母，数量>

            //执行计算代码
            calculation();

            //分配完毕后关闭线程池
            this.poolExecutor.shutdown();

            //等待
            if (!this.poolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.out.println("ERROR!");
            }

            //输出结果
            System.out.println(CountEnWordsTools.toString(this.enWordsCountHashMap, "CountAllFileEnWord"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void calculation() {
        //预热线程池
        poolExecutor.prestartAllCoreThreads();
        //分出子线程执行计算工作
        for (int i = 0; i < fileNum; i++) {
            poolExecutor.submit(new CountFileEnWord(this,
                    new File(Main.getTgrFile() + Main.getFORMAT() + i + Main.getTYPE())));
        }
    }

    @Override
    public void calculationResult(ConcurrentHashMap<Character, Integer> enWordsCountHashMap) {
        //回调函数加锁防止并行错误
        lock.lock();
        //把相同函数封装成工具类
        CountEnWordsTools.calculationResult(this.enWordsCountHashMap, enWordsCountHashMap);
        lock.unlock();
    }

    public static class CountEnWordsTools {
        //输出函数
        public static String toString(ConcurrentHashMap<Character, Integer> enWordsCountHashMap, String functionName) {
            long sum = 0;
            for (int i = 0; i < 26 * 2; i++) {
                char c = (char) (i > 25 ? 'A' + i - 26 : 'a' + i);
                sum += enWordsCountHashMap.get(c);
            }
            return functionName + enWordsCountHashMap + '\n' + "All Count" + sum;
        }

        //计算回调结果函数
        public static void calculationResult(ConcurrentHashMap<Character, Integer> srcEnWords, ConcurrentHashMap<Character, Integer> enWords) {
            for (int i = 0; i < 26 * 2; i++) {
                char c = (char) (i > 25 ? 'A' + i - 26 : 'a' + i);
                if (enWords.get(c) == null) {
                    continue;
                }
                srcEnWords.put(c, srcEnWords.get(c) + enWords.get(c));
            }
        }
    }

    private static class CountFileEnWord implements Runnable, CallbackEnWordCounts {
        private static final int MIN_SIZE = 1000;
        private final ConcurrentHashMap<Character, Integer> enWordsCountHashMap = new ConcurrentHashMap<>();
        private final ReentrantLock lock = new ReentrantLock();
        private final ThreadPoolExecutor poolExecutor;
        private final CallbackEnWordCounts father;
        private final File tgrFile;
        private final int round;

        private CountFileEnWord(CallbackEnWordCounts father, File tgrFile) {
            this.father = father;
            this.tgrFile = tgrFile;
            //设定循环次数，如果太少则只开一个线程
            this.round = (int) (tgrFile.length() / MIN_SIZE) == 0 ? 1 : (int) (tgrFile.length() / MIN_SIZE);
            this.poolExecutor = new ThreadPoolExecutor(
                    10,
                    round,
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());
            for (int i = 0; i < 26 * 2; i++) {
                char c = (char) (i > 25 ? 'A' + i - 26 : 'a' + i);
                this.enWordsCountHashMap.put(c, 0);
            }
        }

        @Override
        public void run() {
            try {
                //执行计算代码
                calculation();

                //分配完毕后关闭线程池
                this.poolExecutor.shutdown();

                //等待
                if (!this.poolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.out.println("ERROR!");
                }

                //输出结果
                System.out.println(CountEnWordsTools.toString(this.enWordsCountHashMap, "CountFileEnWord"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            father.calculationResult(this.enWordsCountHashMap);
        }

        @Override
        public void calculation() {
            try(FileInputStream in = new FileInputStream(tgrFile)) {
                //提前预热线程
                poolExecutor.prestartAllCoreThreads();

                //读出文本信息给子线程分配计算任务
                for (int i = 0; i < round; i++) {
                    byte[] enWords = new byte[MIN_SIZE];
                    if (in.read(enWords) == EOF) {
                        break;
                    }
                    poolExecutor.submit(new CountEnWord(enWords, this));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void calculationResult(ConcurrentHashMap<Character, Integer> enWordsCountHashMap) {
            //回调函数加锁防止并行错误
            lock.lock();
            //把相同函数封装成工具类
            CountEnWordsTools.calculationResult(this.enWordsCountHashMap, enWordsCountHashMap);
            lock.unlock();
        }

        private static class CountEnWord implements Runnable {
            private final ConcurrentHashMap<Character, Integer> enWordsCountHashMap = new ConcurrentHashMap<>();
            private final CallbackEnWordCounts father;
            private final byte[] enWords;

            public CountEnWord(byte[] enWords, CallbackEnWordCounts father) {
                this.enWords = enWords;
                this.father = father;
            }

            @Override
            public void run() {
                //遍历存入哈希表
                for (byte word : enWords) {
                    char c = (char) word;
                    enWordsCountHashMap.put(c,
                            this.enWordsCountHashMap.get(c) == null ? 1 : this.enWordsCountHashMap.get(c) + 1);
                }

                //输出自己结果
                System.out.println(CountEnWordsTools.toString(this.enWordsCountHashMap, "CountEnWord"));

                //回调结果计算函数
                father.calculationResult(this.enWordsCountHashMap);
            }
        }
    }
}