package com.server.server.config;

import com.server.server.matchmaking.RoomRegistry;
import com.server.server.selection.*;
import com.shared.shared.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GameChannelInterceptorTest {
    private final RoomRegistry rooms = new RoomRegistry(new EnvironmentVoteResolver());
    private final GameChannelInterceptor guard = new GameChannelInterceptor(rooms);
    private final Map<String,Object> attributes = new HashMap<>();
    private Message<byte[]> frame(StompCommand command, String destination, int size) {
        var headers = StompHeaderAccessor.create(command); headers.setSessionId("one");
        headers.setSessionAttributes(attributes); headers.setDestination(destination); headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[size],headers.getMessageHeaders());
    }
    @Test void blocksBrokerInjectionAndForeignSubscriptions() {
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SEND,"/match/rooms/room-1",1),null));
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SUBSCRIBE,"/match/rooms/room-1",1),null));
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SUBSCRIBE,"/user/other/queue/private",1),null));
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SEND,"/app/game.takeAction",1),null));
    }
    @Test void allowsOnlyPrivateAndAssignedRoomSubscriptions() {
        var assigned = rooms.assign(Packet.builder().teamIndex(1).race(Race.HUMAN).characterClass(CharacterClass.MAGE).build(),"one");
        assertNotNull(guard.preSend(frame(StompCommand.SUBSCRIBE,"/user/queue/private",1),null));
        assertNotNull(guard.preSend(frame(StompCommand.SUBSCRIBE,assigned.getRoom().destination(),1),null));
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SUBSCRIBE,assigned.getRoom().destination(),1),null));
        assertNotNull(guard.preSend(frame(StompCommand.SEND,"/app/game.sync",1),null));
    }
    @Test void enforcesPayloadAndRateLimits() {
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SEND,"/app/game.joinGame",16385),null));
        for (int i=0;i<60;i++) assertNotNull(guard.preSend(frame(StompCommand.SEND,"/app/game.joinGame",1),null));
        assertThrows(MessageDeliveryException.class,()->guard.preSend(frame(StompCommand.SEND,"/app/game.joinGame",1),null));
    }
    @Test void rejectsOversizedAndControlCharacterNames() {
        Packet selection = Packet.builder().teamIndex(1).race(Race.HUMAN).characterClass(CharacterClass.MAGE).username("x".repeat(33)).build();
        assertNotNull(LobbyStateService.validateSelection(selection));
        selection.setUsername("bad\nname"); assertNotNull(LobbyStateService.validateSelection(selection));
        selection.setUsername("Good name"); assertNull(LobbyStateService.validateSelection(selection));
    }
}
