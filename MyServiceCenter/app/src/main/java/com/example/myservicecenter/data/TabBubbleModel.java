package com.example.myservicecenter.data;

import java.util.List;

/* JADX INFO: loaded from: I:\AndroidApplicationFolder\phone\classes17.dex */
public class TabBubbleModel {
    private List<String> bubbleList;
    private boolean clickToDismiss;
    private String markId;

    public void setBubbleList(List<String> list) {
        this.bubbleList = list;
    }

    public List<String> getBubbleList() {
        return this.bubbleList;
    }

    public void setClickToDismiss(boolean z) {
        this.clickToDismiss = z;
    }

    public boolean isClickToDismiss() {
        return this.clickToDismiss;
    }

    public void setMarkId(String str) {
        this.markId = str;
    }

    public String getMarkId() {
        return this.markId;
    }
}
