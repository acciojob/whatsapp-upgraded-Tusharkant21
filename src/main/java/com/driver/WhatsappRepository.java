package com.driver;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class WhatsappRepository {
    private HashSet<String> userMobileMap = new HashSet<>();
    private HashMap<Message, User> messageSenderMap = new HashMap<>();
    private HashMap<Group, User> groupAdminMap = new HashMap<>();
    private HashMap<Group, List<User>> groupUsersMap = new HashMap<Group, List<User>>();
    private HashMap<Group, List<Message>> groupMessageMap = new HashMap<Group, List<Message>>();
    private int customGroupCount = 0;
    private int messageId = 0;

//    public WhatsappRepository() {
//        this.userMobileMap = userMobileMap;
//        this.messageSenderMap = messageSenderMap;
//        this.groupAdminMap = groupAdminMap;
//        this.groupUsersMap = groupUsersMap;
//        this.groupMessageMap = groupMessageMap;
//        this.customGroupCount = customGroupCount;
//        this.messageId = messageId;
//    }

    public Group createGroup(List<User> users){
        if(users.size()==2){
            Group group = new Group(users.get(1).getName(), 2);
            groupAdminMap.put(group, users.get(0));
            groupUsersMap.put(group, users);
            groupMessageMap.put(group, new ArrayList<Message>());
            return group;
        }
        this.customGroupCount += 1;
        Group group = new Group(new String("Group "+this.customGroupCount), users.size());
        groupAdminMap.put(group, users.get(0));
        groupUsersMap.put(group, users);
        groupMessageMap.put(group, new ArrayList<Message>());
        return group;
    }


    public String createUser(String name, String mobile) throws Exception {
        //Validation
        if(userMobileMap.contains(mobile)){
            throw new Exception("User already exists");
        }
        userMobileMap.add(mobile);
        User user = new User(name, mobile);
        return "SUCCESS";
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        if(groupAdminMap.containsKey(group)){
            if(groupAdminMap.get(group).equals(approver)){
                List<User> participants = groupUsersMap.get(group);
                Boolean userFound = false;
                for(User participant: participants){
                    if(participant.equals(user)){
                        userFound = true;
                        break;
                    }
                }
                if(userFound){
                    groupAdminMap.put(group, user);
                    return "SUCCESS";
                }
                throw new Exception("User is not a participant");
            }
            throw new Exception("Approver does not have rights");
        }
        throw new Exception("Group does not exist");
    }

    public int createMessage(String content){
        this.messageId += 1;
        Message message = new Message(messageId, content);
        return message.getId();
    }
    public int sendMessage(Message message, User sender, Group group) throws Exception{
        if(groupAdminMap.containsKey(group)){
            List<User> users = groupUsersMap.get(group);
            Boolean userFound = false;
            for(User user: users){
                if(user.equals(sender)){
                    userFound = true;
                    break;
                }
            }
            if(userFound){
                messageSenderMap.put(message, sender);
                List<Message> messages = groupMessageMap.get(group);
                messages.add(message);
                groupMessageMap.put(group, messages);
                return messages.size();
            }
            throw new Exception("You are not allowed to send message");
        }
        throw new Exception("Group does not exist");
    }

    public String findMessage(Date start, Date end, int K) throws Exception{
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        List<Message> messages = new ArrayList<>();
        for(Group group: groupMessageMap.keySet()){
            messages.addAll(groupMessageMap.get(group));
        }
        List<Message> filteredMessages = new ArrayList<>();
        for(Message message: messages){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                filteredMessages.add(message);
            }
        }
        if(filteredMessages.size() < K){
            throw new Exception("K is greater than the number of messages");
        }
        Collections.sort(filteredMessages, new Comparator<Message>(){
            public int compare(Message m1, Message m2){
                return m2.getTimestamp().compareTo(m1.getTimestamp());
            }
        });
        return filteredMessages.get(K-1).getContent();
    }

    public int removeUser(User user) throws Exception{
        Boolean userFound = false;
        Group userGroup = null;
        for(Group group: groupUsersMap.keySet()){
            List<User> participants = groupUsersMap.get(group);
            for(User participant: participants){
                if(participant.equals(user)){
                    if(groupAdminMap.get(group).equals(user)){
                        throw new Exception("Cannot remove admin");
                    }
                    userGroup = group;
                    userFound = true;
                    break;
                }
            }
            if(userFound){
                break;
            }
        }
        if(userFound){
            List<User> users = groupUsersMap.get(userGroup);
            List<User> updatedUsers = new ArrayList<>();
            for(User participant: users){
                if(participant.equals(user))
                    continue;
                updatedUsers.add(participant);
            }
            groupUsersMap.put(userGroup, updatedUsers);

            List<Message> messages = groupMessageMap.get(userGroup);
            List<Message> updatedMessages = new ArrayList<>();
            for(Message message: messages){
                if(messageSenderMap.get(message).equals(user))
                    continue;
                updatedMessages.add(message);
            }
            groupMessageMap.put(userGroup, updatedMessages);

            HashMap<Message, User> updatedSenderMap = new HashMap<>();
            for(Message message: messageSenderMap.keySet()){
                if(messageSenderMap.get(message).equals(user))
                    continue;
                updatedSenderMap.put(message, messageSenderMap.get(message));
            }
            messageSenderMap = updatedSenderMap;
            return updatedUsers.size()+updatedMessages.size()+updatedSenderMap.size();
        }
        throw new Exception("User not found");
    }
}
