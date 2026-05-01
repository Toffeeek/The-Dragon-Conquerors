package com.client.client;

import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Pair;
import lombok.Setter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class NetworkClient
{
    String url;
    private WebSocketStompClient stompClient;
    private StompSession session;

    @Setter
    private Consumer<Packet> packetHandler;

    public NetworkClient(String url)
    {
        this.url = url;
    }


    public void connect() throws Exception {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());

        session = stompClient.connectAsync(
            url,
            new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("Connected to server");
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.out.println("Transport error: " + exception.getMessage());
                }
            }
        ).get();

        session.subscribe("/match/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers)
            {
                return Packet.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload)
            {
                Packet p = (Packet) payload;
                System.out.println("Received public packet: " + p.getAction());
                handlePacket(p);
            }
        });

        session.subscribe("/user/queue/private", new StompFrameHandler()
        {
            @Override
            public Type getPayloadType(StompHeaders headers)
            {
                return Packet.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload)
            {
                Packet p = (Packet) payload;
                System.out.println("Received private packet: " + p.getAction());
                handlePacket(p);
            }
        });
    }

    private void handlePacket(Packet packet)
    {
        if(packetHandler != null)
        {
            packetHandler.accept(packet);
        }
    }

    public void send(Packet packet)
    {
        if (session != null && session.isConnected())
        {
            session.send("/app/game.takeAction", packet);
        }
    }

    public void join(String username, int x, int y)
    {
        if (session != null && session.isConnected())
        {
            Packet joinPacket = Packet.builder()
                .username(username)
                .finalPosition(new Pair<>(x,y))
                .action(Action.JOIN)
                .build();
            session.send("/app/game.joinGame", joinPacket);
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected())
        {
            session.disconnect();
        }
    }
}
