package com.galaplat.comprehensive.bidding.activity;

import com.galaplat.comprehensive.bidding.dao.dvos.JbxtActivityDVO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ActivityMap {
    private Map<String, JbxtActivityDVO> activityDVOMap = new HashMap<>();

    public JbxtActivityDVO get(String activityCode) {
        return this.activityDVOMap.get(activityCode);
    }

    public void put(String activityCode, JbxtActivityDVO value) {
        this.activityDVOMap.put(activityCode, value);
    }

}
