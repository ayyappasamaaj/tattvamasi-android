package com.ayyappasamaaj.tattvamasi.model;

public class ParentListItem {
    private String name;
    //private ArrayList<ListItem> listItems = new ArrayList<ListItem>();

    public ParentListItem() {}

    public ParentListItem(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*public ArrayList<ListItem> getListItems() {
        return listItems;
    }

    public void setListItems(ArrayList<ListItem> listItems) {
        this.listItems = listItems;
    }*/
}
