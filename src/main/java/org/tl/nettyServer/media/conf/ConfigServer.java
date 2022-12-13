package org.tl.nettyServer.media.conf;

import org.tl.nettyServer.media.client.Context;
import org.tl.nettyServer.media.client.CoreHandler;
import org.tl.nettyServer.media.client.Server;
import org.tl.nettyServer.media.scope.GlobalScope;
import org.tl.nettyServer.media.scope.IScopeSecurityHandler;
import org.tl.nettyServer.media.scope.ScopeSecurityHandler;

import java.util.HashSet;
import java.util.Set;

public class ConfigServer {
    private Server server;
    private Context context;
    private CoreHandler coreHandler;


    private Server server() {
        if (this.server == null) {
            this.server = Server.getInstance();
        }
        return this.server;
    }

    public Context context() {
        if (this.context == null) {
            this.context = new Context();
        }
        return this.context;
    }


    private CoreHandler coreHandler() {
        if (this.coreHandler == null) {
            this.coreHandler = new CoreHandler();
        }
        return this.coreHandler;
    }


    public void globalScope() throws Exception {
        GlobalScope gsope = new GlobalScope();
        //设置server  最后调用initMethod  将自己注册进server
        gsope.setServer(server());
        gsope.setContext(context());
        gsope.setHandler(coreHandler());
        gsope.setPersistenceClass("org.red5.server.persistence.FilePersistence");
        Set<IScopeSecurityHandler> securityHandlers = new HashSet<>();
        ScopeSecurityHandler handler = new ScopeSecurityHandler();
        handler.setConnectionAllowed(false);
        securityHandlers.add(handler);
        gsope.setSecurityHandlers(securityHandlers);
    }

    public void config() throws Exception {
        globalScope();
    }
}
