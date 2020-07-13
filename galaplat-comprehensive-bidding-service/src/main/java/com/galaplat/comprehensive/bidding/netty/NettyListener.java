package com.galaplat.comprehensive.bidding.netty;

import com.galaplat.comprehensive.bidding.activity.ActivityMap;
import com.galaplat.comprehensive.bidding.activity.CurrentActivity;
import com.galaplat.comprehensive.bidding.activity.queue.QueueHandler;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtGoodsDO;
import com.galaplat.comprehensive.bidding.dao.dvos.JbxtActivityDVO;
import com.galaplat.comprehensive.bidding.service.IJbxtActivityService;
import com.galaplat.comprehensive.bidding.service.IJbxtGoodsService;
import com.galaplat.comprehensive.bidding.utils.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NettyListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private WebSocketServer websocketServer;

    private boolean isInit = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (!isInit) {
            websocketServer.start();

            IJbxtActivityService iJbxtActivityServiceBeans = SpringUtil.getBean(IJbxtActivityService.class);
            ActivityMap activityMap = SpringUtil.getBean(ActivityMap.class);
            List<JbxtActivityDVO> lists = iJbxtActivityServiceBeans.findAll();
            for (int i = 0; i < lists.size(); i++) {
                JbxtActivityDVO jbxtActivityDVO = lists.get(i);
                if (jbxtActivityDVO.getStatus() == 3) {
                    IJbxtGoodsService iJbxtGoodsService = SpringUtil.getBean(IJbxtGoodsService.class);
                    JbxtGoodsDO activeGoods = iJbxtGoodsService.selectActiveGoods(jbxtActivityDVO.getCode());
                    if (activeGoods != null) {
                        CurrentActivity currentActivity = new CurrentActivity(jbxtActivityDVO.getCode(), activeGoods.getGoodsId().toString(), activeGoods.getTimeNum() * 60, 1);
                        activityMap.put(jbxtActivityDVO.getCode(),currentActivity);
                        currentActivity.start();
                    }

                }
            }

            QueueHandler queueHandler = QueueHandler.getInstance();
            queueHandler.start();

            this.isInit = true;
        }

        if(event.getApplicationContext().getParent() == null) {
            try {
                System.out.println("ws.start");
               //websocketServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}