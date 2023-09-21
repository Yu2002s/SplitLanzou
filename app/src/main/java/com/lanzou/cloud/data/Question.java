package com.lanzou.cloud.data;

public class Question {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private int type;

    private String content;

    public Question(int type, String content) {
        this.type = type;
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
