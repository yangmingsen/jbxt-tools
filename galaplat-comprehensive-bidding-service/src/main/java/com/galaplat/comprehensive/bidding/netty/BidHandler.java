package com.galaplat.comprehensive.bidding.netty;

import com.alibaba.fastjson.JSON;
import com.galaplat.comprehensive.bidding.activity.AdminChannelMap;
import com.galaplat.comprehensive.bidding.activity.AdminInfo;
import com.galaplat.comprehensive.bidding.activity.queue.PushQueue;
import com.galaplat.comprehensive.bidding.activity.queue.QueueMessage;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtUserDO;
import com.galaplat.comprehensive.bidding.netty.pojo.RequestMessage;
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

import java.text.SimpleDateFormat;
import java.util.*;

public class BidHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 用来保存所有的客户端连接
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd hh:MM");

    private Logger LOGGER = LoggerFactory.getLogger(BidHandler.class);

    private AdminChannelMap adminChannelMap = SpringUtil.getBean(AdminChannelMap.class);
    private UserChannelMap userChannelMapBean = SpringUtil.getBean(UserChannelMap.class);
    private IJbxtUserService iJbxtUserService = SpringUtil.getBean(IJbxtUserService.class);
    private PushQueue pushQueue = SpringUtil.getBean(PushQueue.class);

    // 当Channel中有新的事件消息会自动调用
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 当接收到数据后会自动调用

        // 获取客户端发送过来的文本消息
        String text = msg.text();
        System.out.println("接收到消息数据为：" + text);
        RequestMessage message = JSON.parseObject(text, RequestMessage.class);

        // {type: 101, data: {userCode: 22343423, activityCode: 23426783345}}
        // {type: 102, data: {adminCode: 22343423}}
        // {type: 300, data: {activityCode: 2234343423}}
        // {type: 213, data: {bidPrice: 32.345, goodsId: 234}}
        switch (message.getType()) {
            // 建立供应商客户端连接的消息
            case 101: {
                String userCode = message.getData().get("userCode");
                String focusActivity = message.getData().get("activityCode");

                JbxtUserDO jbxtUserDO = iJbxtUserService.selectByuserCodeAndActivityCode(userCode, focusActivity);
                if (jbxtUserDO != null) { //验证该供应商是否存在
                    userChannelMapBean.put(userCode, ctx.channel());
                    userChannelMapBean.put(userCode,focusActivity);
                    userChannelMapBean.putChannelToUser(ctx.channel(), userCode);

                    System.out.println("建立用户:" + userCode + "与通道" + ctx.channel().id() + "的关联");
                    userChannelMapBean.print();

                    //同步数据
                    QueueMessage queueMessage = new QueueMessage(211,message.getData());
                    pushQueue.offer(queueMessage);
                } else { //断开该链接
                    ctx.channel().close();
                }
            }
            break;

            // 建立管理员客户端连接的消息
            case 102: {
                String adminCode = message.getData().get("adminCode");
                String focusActivity = "";
                AdminInfo adminInfo = new AdminInfo(focusActivity,ctx.channel());
                adminChannelMap.put(adminCode, adminInfo);
                System.out.println("建立（admin）用户:" + adminCode + "与通道" + ctx.channel().id() + "的关联");
                adminChannelMap.print();
            }
             break;

            case 213: { //处理供应商端提交竞价
                String tStr1 = message.getData().get("bidPrice");
                if (tStr1 == null || "".equals(tStr1)) {
                    LOGGER.info("BidHandler(channelRead0-case213): bidPrice is null or empty");
                    return;
                }
                String tStr2 = message.getData().get("goodsId");
                if (tStr2 == null || "".equals(tStr2)) {
                    LOGGER.info("BidHandler(channelRead0-case213): goodsId is null or empty");
                    return;
                }
                String userCode = userChannelMapBean.getUserByChannel(ctx.channel());
                if (userCode == null)  {
                    LOGGER.info("BidHandler(channelRead0-case213): userCode is null");
                    return;
                }
                message.getData().put("userCode", userCode);
                String activityCode = userChannelMapBean.getUserFocusActivity(userCode);
                message.getData().put("activityCode", activityCode);

                //
                QueueMessage queueMessage = new QueueMessage(213,message.getData());
                pushQueue.offer(queueMessage);
            }
            break;

                //处理管理端主动请求
            case 300: {
              handler300Problem(message,ctx);
            }
            break;
        }

    }

    private void handler300Problem(RequestMessage message, ChannelHandlerContext ctx) {
        String activityCode = message.getData().get("activityCode");
        String adminCode = adminChannelMap.getAdminIdByChannelId(ctx.channel().id());
        AdminInfo tAdminInfo1 = adminChannelMap.get(adminCode);
        tAdminInfo1.setFocusActivity(activityCode); //设置当前管理员聚焦的活动

        Map<String, String > map = new HashMap<>();
        map.put("activityCode", activityCode);
        map.put("adminCode", adminCode);

        QueueMessage queueMessage = new QueueMessage(300, map);
        pushQueue.offer(queueMessage);
    }


    // 当有新的客户端连接服务器之后，会自动调用这个方法
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // 将新的通道加入到clients
       // clients.add(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //UserChannelMap.removeByChannelId(ctx.channel().id().asLongText());
        ctx.channel().close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("关闭通道");
        //UserChannelMap.removeByChannelId(ctx.channel().id().asLongText());
        //UserChannelMap.print();
    }
}