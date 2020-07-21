package com.galaplat.comprehensive.bidding.netty;

import com.alibaba.fastjson.JSON;
import com.galaplat.comprehensive.bidding.activity.ActivityThreadManager;
import com.galaplat.comprehensive.bidding.activity.ActivityThread;
import com.galaplat.comprehensive.bidding.activity.queue.MessageQueue;
import com.galaplat.comprehensive.bidding.activity.queue.QueueMessage;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtActivityDO;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtBiddingDO;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtUserDO;
import com.galaplat.comprehensive.bidding.netty.pojo.RequestMessage;
import com.galaplat.comprehensive.bidding.service.IJbxtActivityService;
import com.galaplat.comprehensive.bidding.service.IJbxtBiddingService;
import com.galaplat.comprehensive.bidding.service.IJbxtUserService;
import com.galaplat.comprehensive.bidding.utils.SpringUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class EventInHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 用来保存所有的客户端连接
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd hh:MM");

    private final Logger LOGGER = LoggerFactory.getLogger(EventInHandler.class);

    private final AdminChannelMap adminChannelMap = SpringUtil.getBean(AdminChannelMap.class);
    private final UserChannelMap userChannelMapBean = SpringUtil.getBean(UserChannelMap.class);
    private final IJbxtUserService iJbxtUserService = SpringUtil.getBean(IJbxtUserService.class);
    private final IJbxtBiddingService iJbxtBiddingService = SpringUtil.getBean(IJbxtBiddingService.class);
    private final IJbxtActivityService iJbxtActivityService = SpringUtil.getBean(IJbxtActivityService.class);
    private final ActivityThreadManager activityManager = SpringUtil.getBean(ActivityThreadManager.class);
    private final MessageQueue messageQueue = SpringUtil.getBean(MessageQueue.class);

    private boolean eventInterceptor(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        return false;
    }

    // 当Channel中有新的事件消息会自动调用
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 当接收到数据后会自动调用

        // 获取客户端发送过来的文本消息
        final String text = msg.text();
        //System.out.println("接收到消息数据为：" + text);
        final RequestMessage message = JSON.parseObject(text, RequestMessage.class);

        // {type: 101, data: {userCode: 22343423, activityCode: 23426783345}}
        // {type: 102, data: {adminCode: 22343423}}
        // {type: 213, data: {bidPrice: 32.345, goodsId: 234}}
        // {type: 300, data: {activityCode: 2234343423}}
        // {type: 302, data: {activityCode: 2234343423, goodsId: 23425346}}
        switch (message.getType()) {
                // 建立供应商客户端连接的消息
            case 101: {
                final String userCode = message.getData().get("userCode");
                final String focusActivity = message.getData().get("activityCode");
                final JbxtActivityDO activityEntity = iJbxtActivityService.findOneByCode(focusActivity);

                final JbxtUserDO jbxtUserDO = iJbxtUserService.selectByuserCodeAndActivityCode(userCode, focusActivity);
                if (jbxtUserDO != null) { //验证该供应商是否存在
                    userChannelMapBean.put(userCode, ctx.channel());
                    userChannelMapBean.put(userCode,focusActivity);
                    userChannelMapBean.putChannelToUser(ctx.channel(), userCode);

                    LOGGER.info("channelRead0: "+"建立用户:" + userCode + "与通道" + ctx.channel().id() + "的关联");

                    if (activityEntity!= null && activityEntity.getStatus() == 4) { //如果当前用户聚焦的活动为已结束则提示供应商端退出登录
                        final Map<String, String> map216 = new HashMap();
                        map216.put("activityCode", focusActivity);
                        map216.put("userCode", userCode);
                        messageQueue.offer(new QueueMessage(216, map216));

                        return; //返回不同步数据
                    }

                    final ActivityThread currentActivity = activityManager.get(focusActivity);
                    if (currentActivity != null) { //如果当前供应商聚焦的竞品活动存在 则同步数据
                        //同步数据
                        final QueueMessage queueMessage = new QueueMessage(211,message.getData());
                        messageQueue.offer(queueMessage);
                    }

                } else { //断开该链接
                    LOGGER.info("channelRead00: 非法连接"+ctx);
                    ctx.channel().close();
                }
            }
            break;
                // 建立管理员客户端连接的消息
            case 102: {
                final String adminCode = message.getData().get("adminCode");
                final AdminInfo adminInfo = new AdminInfo("",ctx.channel());
                adminChannelMap.put(adminCode, adminInfo);

                LOGGER.info("channelRead0: "+"建立（admin）用户:" + adminCode + "与通道" + ctx.channel().id() + "的关联");
            }
             break;
                //处理供应商端提交竞价
            case 213: {
                handler213Problem(message, ctx);
            }
            break;
                //处理管理端主动请求(建立对目标活动关联)
            case 300: {
              handler300Problem(message, ctx);
            }
            break;
                //处理管理端主动请求获取某个竞品数据时
            case 302: {
                final String adminCode = adminChannelMap.getAdminIdByChannelId(ctx.channel().id());
                final AdminInfo adminInfo = adminChannelMap.get(adminCode);
                final String focusActivity = adminInfo.getFocusActivity();
                if (focusActivity == null || "".equals(focusActivity)) { //处理adminCode 不存在的问题
                    LOGGER.info("channelRead0(msg): focusActivity is null or empty");
                    return;
                }

                final AdminInfo tAdminInfo1 = adminChannelMap.get(adminCode);
                message.getData().put("adminCode", adminCode);

                final QueueMessage queueMessage = new QueueMessage(300, message.getData());
                messageQueue.offer(queueMessage);
            }
        }

    }

    private void handler213Problem(RequestMessage message, ChannelHandlerContext ctx) {
        final String tStr1 = message.getData().get("bidPrice");
        if (tStr1 == null || "".equals(tStr1)) {
            LOGGER.info("channelRead0-case213: bidPrice is null or empty");
            return;
        }
        final BigDecimal bidPrice ;
        try {
            bidPrice = new BigDecimal(tStr1);
        } catch (Exception e) {
            LOGGER.info("channelRead0-case213(ERROR): "+e.getMessage());
            return;
        }

        final String tStr2 = message.getData().get("goodsId");
        if (tStr2 == null || "".equals(tStr2)) {
            LOGGER.info("channelRead0-case213: goodsId is null or empty");
            return;
        }

        final Integer goodsId ;
        try {
            goodsId = Integer.parseInt(tStr2);
        }catch (NumberFormatException e) {
            LOGGER.info("channelRead0-case213(ERROR): "+e.getMessage());
            return;
        }

        final String userCode = userChannelMapBean.getUserByChannel(ctx.channel());
        if (userCode == null)  {
            LOGGER.info("channelRead0-case213: userCode is null");
            return;
        }
        message.getData().put("userCode", userCode);
        final String activityCode = userChannelMapBean.getUserFocusActivity(userCode);
        message.getData().put("activityCode", activityCode);

        //验证当前用提交价格 是否大于自己的上一次提交竞价
        final JbxtBiddingDO lastUserMinBid = iJbxtBiddingService.selectMinBidTableBy(userCode, goodsId, activityCode);
        if (lastUserMinBid != null) {
            int compareRes = bidPrice.compareTo(lastUserMinBid.getBid());
            if (compareRes== 0 || compareRes == 1) {
                LOGGER.info("channelRead0-case213: 当前竞价("+bidPrice+")大于或等于历史竞价("+lastUserMinBid.getBid()+")");
                return;
            }
        }

        //
        final QueueMessage queueMessage = new QueueMessage(213,message.getData());
        messageQueue.offer(queueMessage);
    }

    private void handler300Problem(RequestMessage message, ChannelHandlerContext ctx) {
        final String activityCode = message.getData().get("activityCode");
        final String adminCode = adminChannelMap.getAdminIdByChannelId(ctx.channel().id());
        final AdminInfo tAdminInfo1 = adminChannelMap.get(adminCode);
        tAdminInfo1.setFocusActivity(activityCode); //设置当前管理员聚焦的活动
        LOGGER.info("Admin foucus "+activityCode+" 活动");

        final Map<String, String > map = new HashMap<>();
        map.put("activityCode", activityCode);
        map.put("adminCode", adminCode);


        final ActivityThread currentActivity = activityManager.get(activityCode);
        if (currentActivity != null) { //如果当前管理端聚焦的竞品活动存在 则同步数据
            //同步数据
            QueueMessage queueMessage = new QueueMessage(300, map);
            messageQueue.offer(queueMessage);
        }

    }


    // 当有新的客户端连接服务器之后，会自动调用这个方法
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("handlerAdded(msg): "+ ctx+" 建立连接");
        // 将新的通道加入到clients
       // clients.add(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.info("exceptionCaught(error): "+cause.getMessage());
        this.removeChannel(ctx);
    }

    private void removeChannel(ChannelHandlerContext ctx) {
        final String userCode = userChannelMapBean.getUserByChannel(ctx.channel());
        if (userCode != null) {
            userChannelMapBean.remove(userCode);
            LOGGER.info("removeChannel(msg): Supplier("+userCode+") 离开");
        }
        final String adminCode = adminChannelMap.getAdminIdByChannelId(ctx.channel().id());
        if (adminCode != null) {
            adminChannelMap.remove(adminCode);
            LOGGER.info("removeChannel(msg): Admin("+adminCode+") 离开");
        }
        ctx.channel().close();
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.removeChannel(ctx);
    }
}