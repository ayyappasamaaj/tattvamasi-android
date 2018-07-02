package com.ayyappasamaaj.tattvamasi.model;

public class Bhajan {
    public Bhajan(){ }

    public Bhajan(String name){
        this.name = name;
    }

    public Bhajan(String name, String imageName){
        this.name = name;
        this.imageName = imageName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    private String imageName;


}
