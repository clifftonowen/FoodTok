package com.example.foodtok.services;

public class InteractionServiceProvider {
    private static IInteractionService interactionService;
    private InteractionServiceProvider(){

    }

    public static IInteractionService getInteractionService(){
        if(interactionService == null){
            interactionService = new MockInteractionService();
        }
        return interactionService;
    }

    public static void setInteractionService(IInteractionService service){
        interactionService = service;
    }

}
