package com.zriton.offnav.model;

/**
 * Created by aditya on 17/7/16.
 */
public class ModelDirection {


    public String content;
    public String distance;
    public int flag;

    public ModelDirection(String content, String distance, int flag) {
        this.content = content;
        this.distance = distance;
        this.flag = flag;
    }

    public ModelDirection() {
    }
}
