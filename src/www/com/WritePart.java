package www.com;

import java.io.*;
import java.nio.file.Files;
import java.util.Random;

public class WritePart implements Runnable {
    private final Random random;
    private final String name;

    public WritePart(String name) {
        this.name = name;
        //以名字作为SEED
        this.random = new Random(Long.parseLong(name.trim()));
    }

    @Override
    public void run() {
        //指定文件格式位置，准备写入
        File file = new File(Main.getTgrFile() + Main.getFORMAT() + name + Main.getTYPE());
        OutputStream out = null;

        try {
            out = Files.newOutputStream(file.toPath());
            BufferedOutputStream bos = new BufferedOutputStream(out);
            //利用缓存区和自动英文生成写入10000个字母
            for (int i = 0; i < Main.getEnWordsCount(); i++) {
                bos.write(enWordGenerator());
            }
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        Math.sqrt()
    }

    private char enWordGenerator() {
        //随机生成英文字母
        return random.nextInt(2) == 0
                ? (char)(random.nextInt(26) + 'A')
                : (char)(random.nextInt(26) + 'a');
    }
}
