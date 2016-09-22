package com.mwzhang.bgp.bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.mwzhang.bgp.core.IpPrefix;
import com.mwzhang.java.util.io.FileOp;
import com.mwzhang.java.util.io.Output;
import org.joda.time.DateTime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mingwei Zhang on 1/26/15.
 * <p/>
 * Creating a bloom filter for prefixes.
 */
public class PrefixBloomFilter {

    public static BloomFilter createStringBloomFilter() {
        return BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 1000000);
    }

    public static BloomFilter createStringBloomFilter(int numberOfInserts) {
        return BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), numberOfInserts);
    }

    public static boolean saveBloomFilter(BloomFilter bloomFilter, String folder, String filename) {

        try {
            bloomFilter.writeTo(new FileOutputStream(FileOp.getFile(folder, filename, true)));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static BloomFilter loadBloomFilter(String folder, String filename) {
        BloomFilter bloomFilter = null;
        try {
            bloomFilter = BloomFilter.readFrom(new FileInputStream(FileOp.getFile(folder, filename, false)), Funnels.stringFunnel(Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
            bloomFilter = null;
        }

        return bloomFilter;
    }

    /**
     * Given an IP prefix a reverse starting date,
     * return a list of dates that the updates of the IP prefix appeared.
     * The size of the list is limited by the input count variable.
     *
     * @param prefix the IP prefix of interests
     * @param end    the reverse starting date for searching
     * @param count  the size limit of searching
     * @return a list of DateTime objects that represents the dates when updates of the prefix appeared
     */
    public static List<DateTime> getPrefixDates(IpPrefix prefix, DateTime end, int count) {

        int currentYear = -1;
        BloomFilter filter = null;

        List<DateTime> dates = new ArrayList<>();

        DateTime day = new DateTime(end);

        for (; ; day = day.minusDays(1)) {

            if (day.getYear() != currentYear) {
                // reload filter if necessary
                currentYear = day.getYear();
                filter = PrefixBloomFilter.loadBloomFilter("./", String.format("rrc00_%d.bloom", currentYear));
            }
            if (filter == null)
                break;

            String date = String.format("%d%02d%02d", day.getYear(), day.getMonthOfYear(), day.getDayOfMonth());
            Output.pl(date);
            if (filter.mightContain(prefix + date)) {
                dates.add(new DateTime(day));
            }

            if (count > 0 && dates.size() >= count) {
                break;
            }
        }

        return dates;
    }


    public static void main(String[] args) {

        String prefix = args[0];
        String[] lastday = args[1].split("-");
        DateTime date = new DateTime(Integer.valueOf(lastday[0]), Integer.valueOf(lastday[1]), Integer.valueOf(lastday[2]), 0, 0);
        int count = args.length > 2 ? Integer.valueOf(args[2]) : -1;

        String[] lst = prefix.split("/");
        IpPrefix prefix1 = new IpPrefix(lst[0], Integer.valueOf(lst[1]));

        List<DateTime> dates = getPrefixDates(prefix1, date, count);

    }

}
