package com.galaplat.comprehensive.bidding.activity.queue.msg;

import java.util.Map;

public class QueueMessage {
    protected Integer type; // 消息类型
    private Map<String,String> data;  // 消息字段

    public QueueMessage(Integer type, Map<String, String> data) {
        this.type = type;
        this.data = data;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "QueueMessage{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}