package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@ChannelHandler.Sharable
public class GlntNettyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Pattern MESSAGE = Pattern.compile("[^a-zA-Z\\s]");

    private final ServerConfig config;
    private final SimpMessagingTemplate webSocket;
    private final ObjectMapper objectMapper;

    public GlntNettyHandler(SimpMessagingTemplate webSocket, ObjectMapper objectMapper, ServerConfig config) {
        this.config = config;
        this.webSocket = webSocket;
        this.objectMapper = objectMapper;
    }

    // 연결 성공
    // webSocket 에 연결된 client 에게 연결 성공 되었다고 전송
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        webSocket.convertAndSend("/subscribe/connect", "connect");
    }

    // 연결 종료
    // webSocket 에 연결된 client 에게 연결 종료 되었다고 전송
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String remoteAddr = channel.remoteAddress().toString();
        webSocket.convertAndSend("/subscribe/disconnect", "disconnect");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String message = byteBufToString(msg);
        if (message.contains("GATE")) {
            receiveBreakerMessage(message);
            return;
        }

        // 정산기.
        receivePayStationMessage(ctx.channel(), message);

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP ERR => remoteAddress : {}", ctx.channel().remoteAddress(), cause);
    }

    // Netty ByteBuf convert to string
    private String byteBufToString(ByteBuf byteBuf) {
        ByteBuf readbyte = byteBuf;
        int length = readbyte.readableBytes();
        byte[] read = new byte[length];
        for(int i=0; i<length; i++) {
            read[i] = readbyte.getByte(i);
        }
        return new String(read, CharsetUtil.UTF_8);
    }

    // 차단기에서 메세지 수신
    public void receiveBreakerMessage(String message) {
        String msg = MESSAGE.matcher(message).replaceAll("");
        log.info("차단기 메세지 수신 : {}", msg);
    }

    // 정산기에서 메세지 수신
    public void receivePayStationMessage(Channel channel, String message) {
        Map<String, Object> receiveData = objectMapper.convertValue(message, new TypeReference<Map<String, Object>>() {});
        String type = Objects.toString(receiveData.get("type"), "");
        switch (type) {
            case "vehicleListSearch": // 차량 목록 조회
                // send gpms
                /**
                 */
            case "adjustmentRequest": // 정산 요청 응답
                /**
                 */
            case "payment": // 결제 응답 (결제 결과)
                // 127.0.0.1:7979
                String host = channel.remoteAddress().toString();
                FacilityInfo facilityInfo = config.findFacilityInfoByPort(host);
                //facilityInfo.getFacilitiesId(); pathvariable URI

                /**
                 * send GPMS
                 *
                 * String host = ctx.channel().remoteAddress().toString();
                 * FacilityInfo facilityInfo = config.findFacilityInfoByPort(host);
                 * facilityInfo.getFacilitiesId();
                 * pathvariable parameter 붙여서 보내기.
                 *
                 *
                 */
            case "paymentFailure":
            case "healthCheck":
            default:
                break;
        }
    }
}
