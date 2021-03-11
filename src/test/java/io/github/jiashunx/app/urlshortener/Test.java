package io.github.jiashunx.app.urlshortener;

import io.github.jiashunx.app.urlshortener.model.SnowflakeId;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jiashunx
 */
public class Test {

    @org.junit.Test
    public void test() {
        int size = 1000000;
        List<String> idList = new ArrayList<>(size );
        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            idList.add(String.valueOf(SnowflakeId.nextId()));
        }
        long endTimeMillis = System.currentTimeMillis();
        System.out.println("costTime: " + (endTimeMillis - startTimeMillis));
        Set<String> set = new HashSet<>(idList);
        Assert.assertEquals(set.size(), idList.size());
    }

}
