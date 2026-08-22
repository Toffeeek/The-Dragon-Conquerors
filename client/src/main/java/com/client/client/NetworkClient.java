// File Location: client/src/main/java/com/client/client/NetworkClient.java
package com.client.client;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.world.Environment;
import com.shared.shared.model.ability.AbilityType;
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
    private StompSession.Subscription roomSubscription;
    private String roomId;

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
                    if(NetworkClient.this.session != null && NetworkClient.this.session.isConnected()) {
                        System.out.println("Transport error: " + exception.getMessage());
                    }
                }
            }
        ).get();

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
                if (p.getAction() == Action.PRIVATE_JOIN_CONFIRMATION
                    && p.getRoomId() != null && !p.getRoomId().isBlank()) {
                    subscribeToRoom(p.getRoomId());
                    session.send("/app/game.roomReady", Packet.builder()
                        .ID(p.getID()).roomId(p.getRoomId()).action(Action.ROOM_READY).build());
                }
                handlePacket(p);
            }
        });
    }

    private synchronized void subscribeToRoom(String assignedRoomId) {
        if (assignedRoomId.equals(roomId) && roomSubscription != null) return;
        if (roomSubscription != null) roomSubscription.unsubscribe();
        roomId = assignedRoomId;
        roomSubscription = session.subscribe("/match/rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Packet.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Packet packet = (Packet) payload;
                if (!assignedRoomId.equals(packet.getRoomId())) return;
                System.out.println("Received " + assignedRoomId + " packet: PlayerID "
                    + packet.getID() + ":" + packet.getAction());
                handlePacket(packet);
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
            packet.setRoomId(roomId);
            session.send("/app/game.takeAction", packet);
        }
    }

    public void join(String username, Vector2 startingPosition, CharacterClass characterClass)
    {
        join(username, startingPosition, 0, characterClass, Race.HUMAN);
    }

    public void join(String username, Vector2 startingPosition, int teamIndex,
                     CharacterClass characterClass, Race race)
    {
        if (session != null && session.isConnected())
        {
            roomId = null;
            Packet joinPacket = Packet.builder()
                .username(username)
                .finalPosition(startingPosition)
                .teamIndex(teamIndex)
                .characterClass(characterClass)
                .race(race)
                .action(Action.JOIN)
                .build();
            session.send("/app/game.joinGame", joinPacket);
        }
    }

    public void voteEnvironment(int playerId, Environment environment)
    {
        if (session != null && session.isConnected() && playerId >= 0 && environment != null)
        {
            Packet votePacket = Packet.builder()
                .ID(playerId)
                .roomId(roomId)
                .environment(environment)
                .action(Action.ENVIRONMENT_VOTE)
                .build();
            session.send("/app/game.voteEnvironment", votePacket);
        }
    }

    public void move(int playerId, Vector2 destination)
    {
        send(Packet.builder()
            .ID(playerId)
            .finalPosition(destination == null ? null : new Vector2(destination))
            .action(Action.MOVE)
            .build());
    }

    public void useAbility(int playerId, AbilityType ability, int targetPlayerId,
                           Vector2 targetPosition)
    {
        send(Packet.builder()
            .ID(playerId)
            .ability(ability)
            .targetPlayerID(targetPlayerId)
            .targetPosition(targetPosition == null ? null : new Vector2(targetPosition))
            .action(Action.USE_ABILITY)
            .build());
    }

    public void endTurn(int playerId)
    {
        send(Packet.builder().ID(playerId).action(Action.END_TURN).build());
    }

    public void disconnect() {
        if (session != null && session.isConnected())
        {
            if (roomSubscription != null) roomSubscription.unsubscribe();
            session.disconnect();
        }
        roomSubscription = null;
        roomId = null;
    }
}
