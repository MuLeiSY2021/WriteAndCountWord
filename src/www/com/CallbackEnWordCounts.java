package www.com;

import java.util.concurrent.ConcurrentHashMap;

public interface CallbackEnWordCounts {
    //计算的结果回调函数
    void calculationResult(ConcurrentHashMap<Character, Integer> enWordsCountHashMap);

    //父函数的计算函数
    void calculation();
}
