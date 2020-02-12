package com.varun.testapp.ui.singletons;

import android.net.Uri;

public class Visitors {
    private String person_name;

    private String visitor_type;

    private String phone_number;

    private  int visitor_count;

    private  String uid;

    private  String visitor_img_url;

    public Visitors(String person_name, String visitor_type, String phone_number, String uid,int visitor_count,String visitor_img_url) {
        this.person_name = person_name;
        this.visitor_type = visitor_type;
        this.phone_number = phone_number;
        this.uid = uid;
        this.visitor_count=visitor_count;
        this.visitor_img_url=visitor_img_url;
    }

    public Visitors() {}

    public String getVisitor_img_url() {
        return visitor_img_url;
    }

    public void setVisitor_img_url(String visitor_img_url) {
        this.visitor_img_url = visitor_img_url;
    }

    public int getVisitor_count() {
        return visitor_count;
    }

    public void setVisitor_count(int visitor_count) {
        this.visitor_count = visitor_count;
    }

    public String getPerson_name() {
        return person_name;
    }

    public void setPerson_name(String person_name) {
        this.person_name = person_name;
    }

    public String getVisitor_type() {
        return visitor_type;
    }

    public void setVisitor_type(String visitor_type) {
        this.visitor_type = visitor_type;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override

    public String toString() {

        return "visitorsData{" +

                "name='" + person_name + '\'' +

                ", uid='" + uid + '\'' +

                ", phone='" + phone_number + '\'' +

                ", visitor_type='" + visitor_type+ '\'' +
                ", visitor_count='" + visitor_count+ '\'' +
                ", visitor_img_url='" + visitor_img_url+ '\'' +

                '}';

    }
}
