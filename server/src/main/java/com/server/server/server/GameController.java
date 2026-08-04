package com.server.server.server;



import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GameController
{
    @Autowired
    private SimpMessageSendingOperations messageTemplate;
    private int totalPlayers = 0;
    private int activePlayerId;


    // ID : {username, (x,y)}
    private Map<Integer, Pair<String, Vector2>> playerCoordinates = new HashMap<>();

    // ID : Player Object
    private Map<Integer, Player> players = new HashMap<>();
    private ArrayList<CharacterClass> playerClasses = new ArrayList<>();


    @MessageMapping("/game.takeAction")
    @SendTo("/match/public")
    public Packet takeAction(@Payload Packet p)
    {
        playerCoordinates.put(p.getID(), new Pair<>(p.getUsername(), p.getFinalPosition()));

        if(p.getAction() == Action.PRIMARY ||
            p.getAction() == Action.SECONDARY ||
            p.getAction() == Action.ULTIMATE)
        {
            var killedPlayersId = new ArrayList<>(p.getKilledPlayersId());
        }

        if(p.getAction() == Action.END_TURN)
        {
            activePlayerId = (activePlayerId + 1) % totalPlayers;

        }
        p.setActivePlayerID(activePlayerId);

        return p;
    }

    @MessageMapping("/game.joinGame")
    @SendTo("/match/public")
    public Packet addPlayer(@Payload Packet p, SimpMessageHeaderAccessor headerAccessor)
    {
        playerClasses.add(p.getCharacterClass());

        if(totalPlayers == 0)
        {
            activePlayerId = 0;
        }

        int assignedID = totalPlayers++;
        String sessionId = headerAccessor.getSessionId();

        p.setID(assignedID);
        Packet privateP = Packet.builder()
            .ID(assignedID)
            .activePlayerID(activePlayerId)
            .action(Action.PRIVATE_JOIN_CONFIRMATION)
            .build();
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
                    .characterClass(playerClasses.get(playerEntry.getKey()))
                    .activePlayerID(activePlayerId)
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
