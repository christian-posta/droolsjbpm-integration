/*
 * Copyright 2010 salaboy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */

package org.drools.grid.timer.impl;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.drools.grid.GridServiceDescription;
import org.drools.grid.MessageReceiverHandlerFactoryService;
import org.drools.grid.internal.responsehandlers.BlockingMessageResponseHandler;
import org.drools.grid.io.Conversation;
import org.drools.grid.io.ConversationManager;
import org.drools.grid.io.MessageReceiverHandler;
import org.drools.grid.io.impl.CommandImpl;
import org.drools.grid.service.directory.Address;
import org.drools.grid.timer.Scheduler;

/**
 *
 * @author salaboy
 */
public class SchedulerClient implements Scheduler,
    MessageReceiverHandlerFactoryService{

    private GridServiceDescription schedulerGsd;

    private ConversationManager    conversationManager;

    public SchedulerClient(GridServiceDescription schedulerGsd, ConversationManager conversationManager) {
        this.schedulerGsd = schedulerGsd;
        this.conversationManager = conversationManager;
    }
    
    
    
    public void scheduleJob(ScheduledJob job) {
        InetSocketAddress[] sockets = (InetSocketAddress[]) ((Address) schedulerGsd.getAddresses().get( "socket" )).getObject();
        CommandImpl cmd = new CommandImpl( "Scheduler.scheduleJob",
                                           Arrays.asList( new Object[]{ job } ) ); 
        sendMessage( this.conversationManager,
                     sockets,
                     this.schedulerGsd.getId(),
                     cmd );        
    }

    public void removeJob(String jobId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static Object sendMessage(ConversationManager conversationManager,
                                     InetSocketAddress[] sockets,
                                     String id,
                                     Object body) {
        BlockingMessageResponseHandler handler = new BlockingMessageResponseHandler();
        Exception exception = null;
        for ( InetSocketAddress socket : sockets ) {
            try {
                Conversation conv = conversationManager.startConversation( sockets[0],
                                                                           id );
                conv.sendMessage( body,
                                  handler );
                exception = null;
            } catch ( Exception e ) {
                exception = e;
                conversationManager.endConversation();
            }
            if ( exception == null ) {
                break;
            }
        }
        if ( exception != null ) {
            throw new RuntimeException( "Unable to send message",
                                        exception );
        }
        try {
            return handler.getMessage().getBody();
        } finally {
            conversationManager.endConversation();
        }
    }


    public MessageReceiverHandler getMessageReceiverHandler() {
        return new SchedulerServer( this );
    }

}