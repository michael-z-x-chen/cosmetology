package com.cloud.es.model;



import java.util.Date;

/**
 * @author czx
 * @date 2018-03-14 17:16:11
 */
public class KeyWordModel {

    /***/
    private Long keyId;
    /**
     * 被拆分的关键词主题
     */
    private Long parentKeyId = 0l;
    /**
     * 0、用户行为；1、运营定义
     */
    private Integer collectType = 0;
    /**
     * 0、未入词库；1、已入词库
     */
    private Integer keyState = 0;
    /**
     * 关键字
     */
    private String keyWord;
    /**
     * 是否删除（0：未删除；1：删除）
     */
    private Integer isDelete = 0;
    /**
     * 创建人id
     */
    private Long createdBy = 0l;
    /**
     * 更新人id
     */
    private Long updatedBy = 0l;
    /**
     * 数据库自动记录的数据创建时间
     */
    private Date dbCreatedTime;
    /**
     * 数据库自动记录的更新时间
     */
    private Date dbUpdatedTime;

    /**
     * 设置：
     */
    public void setKeyId(Long keyId) {
        this.keyId = keyId;
    }

    /**
     * 获取：
     */
    public Long getKeyId() {
        return keyId;
    }

    /**
     * 设置：被拆分的关键词主题
     */
    public void setParentKeyId(Long parentKeyId) {
        this.parentKeyId = parentKeyId;
    }

    /**
     * 获取：被拆分的关键词主题
     */
    public Long getParentKeyId() {
        return parentKeyId;
    }

    /**
     * 设置：0、用户行为；1、运营定义
     */
    public void setCollectType(Integer collectType) {
        this.collectType = collectType;
    }

    /**
     * 获取：0、用户行为；1、运营定义
     */
    public Integer getCollectType() {
        return collectType;
    }

    /**
     * 设置：0、未入词库；1、已入词库
     */
    public void setKeyState(Integer keyState) {
        this.keyState = keyState;
    }

    /**
     * 获取：0、未入词库；1、已入词库
     */
    public Integer getKeyState() {
        return keyState;
    }

    /**
     * 设置：关键字
     */
    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }

    /**
     * 获取：关键字
     */
    public String getKeyWord() {
        return keyWord;
    }

    /**
     * 设置：是否删除（0：未删除；1：删除）
     */
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    /**
     * 获取：是否删除（0：未删除；1：删除）
     */
    public Integer getIsDelete() {
        return isDelete;
    }

    /**
     * 设置：创建人id
     */
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 获取：创建人id
     */
    public Long getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置：更新人id
     */
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 获取：更新人id
     */
    public Long getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 设置：数据库自动记录的数据创建时间
     */
    public void setDbCreatedTime(Date dbCreatedTime) {
        this.dbCreatedTime = dbCreatedTime;
    }

    /**
     * 获取：数据库自动记录的数据创建时间
     */
    public Date getDbCreatedTime() {
        return dbCreatedTime;
    }

    /**
     * 设置：数据库自动记录的更新时间
     */
    public void setDbUpdatedTime(Date dbUpdatedTime) {
        this.dbUpdatedTime = dbUpdatedTime;
    }

    /**
     * 获取：数据库自动记录的更新时间
     */
    public Date getDbUpdatedTime() {
        return dbUpdatedTime;
    }

    //JSON
/*    public String toString() {
        return JasonUtil.toJsonString(this);
    }*/
}