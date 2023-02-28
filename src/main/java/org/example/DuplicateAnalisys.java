package org.example;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicateAnalisys {

    public void findDuplicates() {
        //TODO get This
        Stream<StatItem> items = Stream.empty();

        Map<Long, List<StatItem>> dupSearchItems = items.collect(Collectors.groupingBy(item -> item.length()));
        for(Long l : dupSearchItems.keySet()) {
            if(dupSearchItems.get(l).size() == 1) {
                //No Duplicates, remove key
                dupSearchItems.remove(l);
            }
        }

        Map<Byte[],List<StatItem>> duplicatesList = new HashMap<>();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA3-256");
            for(Long mapKey : dupSearchItems.keySet()) {
                for(StatItem item : dupSearchItems.get(mapKey)) {
                    duplicatesList.getOrDefault(FileDigester.digestFile(item.p()),new ArrayList<>());
                }
            }



        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

}
