package com.ayyappasamaaj.tattvamasi.model;

public class Bhajan {
    public Bhajan(){

    }

    public Bhajan(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;
}
