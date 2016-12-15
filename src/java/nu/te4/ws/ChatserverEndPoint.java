/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.te4.ws;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;


/**
 *
 * @author linusgoransson
 */



@ServerEndpoint("/chatserver")
public class ChatserverEndPoint {

    
    static Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void open(Session session) throws IOException {
        sessions.add(session);
    }

    @OnClose
    public void close(Session session) throws IOException {
        sessions.remove(session);
        Iterator<Session> users = sessions.iterator();
        while (users.hasNext()) {
            Session user = users.next();
            user.getBasicRemote().sendText(buildJsonUsers());
            user.getBasicRemote().sendText(
                    buildJsonData("Chatt", 
                            (String) session.getUserProperties()
                                    .get("username") + " lämnade chatten :("));
        }
    }

    @OnMessage
    public void onMessage(String message, Session userSession) throws IOException {
    List<String> words = Arrays.asList("fan","helvete"); 
    for (String word : words) {
     Pattern rx = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
     message = rx.matcher(message).replaceAll(new String(new char[word.length()]).replace('\0', '*'));
    }
    Pattern p = Pattern.compile("([a-z\\d])\\1\\1", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(message);
    Pattern p2 = Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$", Pattern.CASE_INSENSITIVE);
    Matcher m2 = p2.matcher(message);
    
    
        String username = (String) userSession.getUserProperties().get("username");
        
        if (username == null) { 
            userSession.getUserProperties().put("username", message);
            String returnMessage = buildJsonData("Chatt", "Ditt användarnamn är: " + message);
            userSession.getBasicRemote().sendText(returnMessage);
            Iterator<Session> users = sessions.iterator();
            
            while (users.hasNext()) {
                users.next().getBasicRemote().sendText(buildJsonUsers());
            }
        } else { 
            Iterator<Session> iterator = sessions.iterator();
            if(m2.find()){
                String returnMessage = buildJsonData("AntiSpam", "Inga länkar");
                userSession.getBasicRemote().sendText(returnMessage);
            }else if (m.find()) {
                String returnMessage = buildJsonData("AntiSpam", "För många av tecknet "+ m.group(1) +" i rad");
                userSession.getBasicRemote().sendText(returnMessage);
            }else if(message.length() > 50){
                String returnMessage = buildJsonData("AntiSpam", "För långt meddelande (Max 50 tecken)");
                userSession.getBasicRemote().sendText(returnMessage);
                
            }else if(message.equals("")){
                String returnMessage = buildJsonData("AntiSpam", "Inga toma meddelanden!");
                userSession.getBasicRemote().sendText(returnMessage);
            }else{
             while(iterator.hasNext()) {
                iterator.next().getBasicRemote().sendText(buildJsonData(username, message));
            }   
            }
            
        }
            
        
       
        

    }
    
    

    private String buildJsonData(String username, String message) {
        JsonObject object = Json.createObjectBuilder().add("username", username).add("message", message).build();
        return object.toString();
    }

    private String buildJsonUsers() {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        Iterator<Session> users = sessions.iterator();
        while (users.hasNext()) {
            try {
                jsonArrayBuilder.add(
                        Json.createObjectBuilder()
                        .add("username", (String) users.next().getUserProperties().get("username"))
                        .build());
            } catch (Exception e) {
                System.out.println("Error "+e.getMessage());
            }
        }
        return jsonArrayBuilder.build().toString();
    }
    
}
