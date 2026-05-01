package com.server.server.config;




import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;


@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener
{
    /**
     * Used to manually send messages to WebSocket/STOMP destinations.
     * In controller methods, @SendTo can automatically broadcast the return value.
     * But this class is an event listener, not a @MessageMapping controller method,
     * so we use messageTemplate.convertAndSend(...) to broadcast messages manually.
     *
     * @RequiredArgsConstructor generates a constructor for this final field.
     * Spring then uses constructor injection to provide the SimpMessageSendingOperations
     * bean, similar to @Autowired but through the constructor.
     */
    private final SimpMessageSendingOperations messageTemplate;

    /**
     * Called automatically when Spring publishes a SessionDisconnectEvent.
     *
     * @EventListener tells Spring that this method should listen for application events.
     * Because this method takes a SessionDisconnectEvent parameter, Spring calls it only
     * for disconnect events, not for every type of event.
     *
     * The event contains the message/headers for the disconnected WebSocket session.
     * We wrap the event message with StompHeaderAccessor so we can read the STOMP headers
     * and session attributes.
     *
     * The username was previously saved in the session attributes when the user joined:
     * sessionAttributes["username"] = senderName.
     *
     * If a username exists, we create a LEAVE message and broadcast it to "/topic/public"
     * so all remaining clients subscribed to that destination know that the user left.
     */
    @EventListener
    public void handleWebSocketListener(SessionDisconnectEvent event)
    {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Object IDAttribute = headerAccessor
                .getSessionAttributes()
                .get("ID");

        if(IDAttribute == null)
        {
            return;
        }

        int ID = (int) IDAttribute;

        if(ID != -1)
        {
            log.info("User disconnected: {}", ID);

            var packet = Packet.builder()
                    .action(Action.LEAVE)
                    .ID(ID)
                    .build();

            messageTemplate.convertAndSend("/match/public", packet);
        }
    }
}
