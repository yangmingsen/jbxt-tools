package com.galaplat.comprehensive.bidding.netty;

import com.galaplat.comprehensive.bidding.activity.ActivityInfoMap;
import com.galaplat.comprehensive.bidding.activity.ActivityTask;
import com.galaplat.comprehensive.bidding.activity.ActivityThreadManager;
import com.galaplat.comprehensive.bidding.activity.queue.QueueHandlerThreadSingleton;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtGoodsDO;
import com.galaplat.comprehensive.bidding.dao.dvos.JbxtActivityDVO;
import com.galaplat.comprehensive.bidding.service.IJbxtActivityService;
import com.galaplat.comprehensive.bidding.service.IJbxtGoodsService;
import com.galaplat.comprehensive.bidding.utils.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NettyListener implements ApplicationListener<ContextRefreshedEvent> {


    private final Logger LOGGER = LoggerFactory.getLogger(NettyListener.class);

    @Autowired
    private WebSocketServer websocketServer;

    @Autowired
    private ActivityInfoMap activityInfoMap;

    @Autowired
    private ActivityThreadManager activityThreadManager;

    private boolean isInit = false;

    //@Autowired
    private  SpringUtil springUtil;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        boolean isTimeToRun = false;
        if (event.getApplicationContext().getClass().equals(AnnotationConfigServletWebServerApplicationContext.class)) {
            isTimeToRun = true;
        }
        if (!isInit && isTimeToRun) {
            websocketServer.start();
            LOGGER.info("onApplicationEvent(msg): WebSocket started");
            final IJbxtActivityService iJbxtActivityServiceBeans = springUtil.getBean(IJbxtActivityService.class);
            final ActivityThreadManager activityMap = springUtil.getBean(ActivityThreadManager.class);
            final List<JbxtActivityDVO> lists = iJbxtActivityServiceBeans.findAll();
            for (JbxtActivityDVO jbxtActivityDVO : lists) {
                if (jbxtActivityDVO.getStatus() == 3) {
                    final IJbxtGoodsService iJbxtGoodsService = springUtil.getBean(IJbxtGoodsService.class);
                    final JbxtGoodsDO activeGoods = iJbxtGoodsService.selectActiveGoods(jbxtActivityDVO.getCode());
                    if (activeGoods != null) {
                        //final ActivityThread currentActivity = new ActivityThread(jbxtActivityDVO.getCode(), activeGoods.getGoodsId().toString(), activeGoods.getTimeNum() * 60, 1);
                       final ActivityTask.Builder builder = new ActivityTask.Builder();

                       builder.activityCode(jbxtActivityDVO.getCode()).
                               goodsId(activeGoods.getGoodsId()).
                               initTime(activeGoods.getTimeNum() * 60).
                               supplierNum(jbxtActivityDVO.getSupplierNum()).
                               delayedCondition(activeGoods.getLastChangTime()).
                               allowDelayedLength(activeGoods.getPerDelayTime()).
                               allowDelayedTime(activeGoods.getDelayTimes());

                        ActivityTask activityTask = builder.build();
                        activityMap.put(jbxtActivityDVO.getCode(), activityTask);
                        activityThreadManager.doTask(activityTask);
//                        currentActivity.start();
                        LOGGER.info("启动 " + jbxtActivityDVO.getCode() + " 活动");
                    }

                }
            }

            QueueHandlerThreadSingleton queueHandlerThreadSingleton = QueueHandlerThreadSingleton.getInstance();
            queueHandlerThreadSingleton.start();

            this.isInit = true;
        }

    }
}
