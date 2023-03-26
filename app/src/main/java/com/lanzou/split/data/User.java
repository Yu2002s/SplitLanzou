package com.lanzou.split.data;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

public class User extends LitePalSupport {

    private long id;

    @Column(unique = true)
    private long uid;
    private String username;
    private String password;

    private Long uploadPath;

    public Long getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(Long uploadPath) {
        this.uploadPath = uploadPath;
    }

    public User() {
    }

    public User(long uid) {
        assignBaseObjId(uid);
    }

    public void update() {
        update(id);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String cookie;

    private boolean isCurrent;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}
