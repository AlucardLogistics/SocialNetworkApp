package com.logistics.alucard.socialnetwork.Models;

public class Followers {

    private String user_id;

    public Followers(String user_id) {
        this.user_id = user_id;
    }

    public Followers() {
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
