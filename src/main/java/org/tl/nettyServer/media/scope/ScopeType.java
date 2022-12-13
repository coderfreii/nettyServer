package org.tl.nettyServer.media.scope;

/**
 * Represents all the supported scope types.
 * 表示所有支持的作用域类型。
 * @author Paul Gregoire (mondain@gmail.com)
 */
public enum ScopeType {
    /*未定义*/
    UNDEFINED,
    /*全局的*/
    GLOBAL,
    /*应用*/
    APPLICATION,
    /*房间*/
    ROOM,
    /*广播*/
    BROADCAST,
    /*共享对象*/
    SHARED_OBJECT;

}
