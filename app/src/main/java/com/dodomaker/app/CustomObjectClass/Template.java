package com.dodomaker.app.CustomObjectClass;

public class Template {

    private String title;
    private String image;
    private String pathlink;
    private String songlink;
    private int trimlength;
    private int editstyle;

    public Template() {
    }

    public Template(String title, String points, String image, String pathlink, String songlink, int trimlength, int editstyle) {
        this.title = title;
        this.image = image;
        this.pathlink = pathlink;
        this.songlink = songlink;
        this.trimlength = trimlength;
        this.editstyle = editstyle;
    }

    public String gettitle() {return title;}

    public void settitle(String title) {
        this.title = title;
    }

    public String getimage() {return image;}

    public void setimage(String image) {
        this.image = image;
    }

    public String getpathlink() {return pathlink;}

    public void setpathlink(String pathlink) {
        this.pathlink = pathlink;
    }

    public String getsonglink() {return songlink;}

    public void setsonglink(String songlink) {
        this.songlink = songlink;
    }

    public int gettrimlength() {return trimlength;}

    public void settrimlength(int trimlength) {
        this.trimlength = trimlength;
    }

    public int geteditstyle() {return editstyle;}

    public void seteditstyle(int editstyle) {
        this.editstyle = editstyle;
    }
}

