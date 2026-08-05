package com.server.server.server;



import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Pair;
import com.shared.shared.model.PlayerState;
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
//    private Map<Integer, Pair<String, Vector2>> playerCoordinates = new HashMap<>();
    private Map<Integer, PlayerState> players = new HashMap<>();

//    private ArrayList<CharacterClass> playerClasses = new ArrayList<>();


    @MessageMapping("/game.takeAction")
    @SendTo("/match/public")
    public Packet takeAction(@Payload Packet p)
    {
//        playerCoordinates.put(p.getID(), new Pair<>(p.getUsername(), p.getFinalPosition()));

        if(p.getPlayer() == null)
        {
            System.out.println("\n\nNO PLAYER IN THE PACKET\n\n");
        }

        players.put(p.getPlayer().getID(), p.getPlayer());
        updatePlayerState(p);

        if(p.getAction() == Action.PRIMARY ||
            p.getAction() == Action.SECONDARY ||
            p.getAction() == Action.ULTIMATE)
        {
            var killedPlayersId = new ArrayList<>(p.getKilledPlayersId());
            for(int id : killedPlayersId)
            {
                players.get(id).setDead(true);
            }
        }

        if(p.getAction() == Action.END_TURN)
        {
            while(true)
            {
                activePlayerId = (activePlayerId + 1) % totalPlayers;

                if(!players.get(activePlayerId).isDead())
                    break;
            }
        }
        p.setActivePlayerID(activePlayerId);

        return p;
    }

    @MessageMapping("/game.joinGame")
    @SendTo("/match/public")
    public Packet addPlayer(@Payload Packet p, SimpMessageHeaderAccessor headerAccessor)
    {
        System.out.println("[SERVER] addPLayer()");
        if(totalPlayers == 0)
        {
            activePlayerId = 0;
        }

        int assignedID = totalPlayers++;
        String sessionId = headerAccessor.getSessionId();

        p.getPlayer().setID(assignedID);

//        PlayerState player = PlayerState.builder().ID(assignedID).build();
        Packet privateP = Packet.builder()
            .player(PlayerState.builder().ID(assignedID).build())
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

        for(var playerEntry : players.entrySet())
        {
            Packet playerInfoPacket = Packet.builder()
                    .player(players.get(playerEntry.getKey()))
                    .action(Action.PLAYER_COORDINATE)
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

        players.put(assignedID, p.getPlayer());
        p.setPlayer(players.get(assignedID));
        return p;
    }

    private void updatePlayerState(Packet p) {
        PlayerState player = players.get(p.getPlayer().getID());
        if (player == null) return;

        if (p.getPlayer().getUsername() != null) player.setUsername(p.getPlayer().getUsername());
        if (p.getPlayer().getPosition() != null) player.setPosition(p.getPlayer().getPosition());
        if (p.getPlayer().getCharacterClass() != null) player.setCharacterClass(p.getPlayer().getCharacterClass());
    }

    private MessageHeaders createHeaders(String sessionId)
    {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}
