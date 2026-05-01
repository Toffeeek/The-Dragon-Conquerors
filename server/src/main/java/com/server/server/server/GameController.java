package com.server.server.server;



import com.server.server.model.Action;
import com.server.server.model.Packet;
import com.server.server.model.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class GameController
{
    @Autowired
    private SimpMessageSendingOperations messageTemplate;
    private int currPlayerIndex = 0;


    private Map<Integer, Pair<String, Pair<Integer, Integer>>> playerCoordinates = new HashMap<>();

    @MessageMapping("/game.takeAction")
    @SendTo("/match/public")
    public Packet takeAction(@Payload Packet p)
    {
        playerCoordinates.put(p.getID(), new Pair<>(p.getUsername(), p.getFinalPosition()));
        return p;
    }

    @MessageMapping("/game.joinGame")
    @SendTo("/match/public")
    public Packet addPlayer(@Payload Packet p, SimpMessageHeaderAccessor headerAccessor)
    {
        int assignedID = currPlayerIndex++;
        String sessionId = headerAccessor.getSessionId();

        p.setID(assignedID);
        Packet privateP = Packet.builder().ID(assignedID).action(Action.PRIVATE_JOIN_CONFIRMATION).build();
        headerAccessor.getSessionAttributes().put("ID", assignedID);

        messageTemplate.convertAndSendToUser
        (
                sessionId,
                "/queue/private",
                privateP,
                createHeaders(sessionId)
        );

        for(var playerEntry : playerCoordinates.entrySet())
        {
            Packet playerInfoPacket = Packet.builder()
                    .ID(playerEntry.getKey())
                    .username(playerEntry.getValue().first)
                    .finalPosition(playerEntry.getValue().second)
                    .action(Action.PLAYER_COORDINATE)
                    .build();
            messageTemplate.convertAndSendToUser
            (
                    sessionId,
                    "/queue/private",
                    playerInfoPacket,
                    createHeaders(sessionId)
            );
        }
        Packet playerInfoPacket = Packet.builder()
                .action(Action.EOF)
                .build();
        messageTemplate.convertAndSendToUser
        (
                sessionId,
                "/queue/private",
                playerInfoPacket,
                createHeaders(sessionId)
        );

        playerCoordinates.put(assignedID, new Pair<>(p.getUsername(), p.getFinalPosition()));
        return p;
    }

    private MessageHeaders createHeaders(String sessionId)
    {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}
