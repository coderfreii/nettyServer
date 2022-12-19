package org.tl.nettyServer.media.conf;

import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.tl.nettyServer.media.client.ClientRegistry;
import org.tl.nettyServer.media.client.Context;
import org.tl.nettyServer.media.client.CoreHandler;
import org.tl.nettyServer.media.client.Server;
import org.tl.nettyServer.media.net.rtmp.status.StatusObjectService;
import org.tl.nettyServer.media.scope.*;
import org.tl.nettyServer.media.service.*;
import org.tl.nettyServer.media.service.invoker.ServiceInvoker;
import org.tl.nettyServer.media.service.resolver.ContextServiceResolver;
import org.tl.nettyServer.media.service.resolver.HandlerServiceResolver;
import org.tl.nettyServer.media.service.resolver.IServiceResolver;
import org.tl.nettyServer.media.service.resolver.ScopeServiceResolver;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Component
public class ConfigServer implements ApplicationContextAware {
    public static ConfigServer configServer;

    private ApplicationContext applicationContext;

    private Server server;
    private Context context;
    private CoreHandler coreHandler;
    private GlobalScope globalScope;


    private Server server() {
        if (this.server == null) {
            this.server = Server.getInstance();
        }
        return this.server;
    }

    public Context context() {
        if (this.context == null) {
            this.context = new Context();
            this.context.setScopeResolver(scopeResolver());
            this.context.setClientRegistry(clientRegistry());
            this.context.setServiceInvoker(serviceInvoker());
            this.context.setMappingStrategy(mappingStrategy());
            context.setApplicationContext(this.applicationContext);
            context.setCoreBeanFactory(this.applicationContext);
        }
        return this.context;
    }


    private CoreHandler coreHandler() {
        if (this.coreHandler == null) {
            this.coreHandler = new CoreHandler();
        }
        return this.coreHandler;
    }


    @SneakyThrows
    public GlobalScope globalScope() {
        if (this.globalScope == null) {
            GlobalScope gsope = new GlobalScope();
            //设置server  最后调用initMethod  将自己注册进server
            gsope.setServer(server());
            gsope.setContext(context());
            gsope.setHandler(coreHandler());
            gsope.setPersistenceClass("org.tl.nettyServer.media.persistence.FilePersistence");
            Set<IScopeSecurityHandler> securityHandlers = new HashSet<>();
            ScopeSecurityHandler handler = new ScopeSecurityHandler();
            handler.setConnectionAllowed(false);
            securityHandlers.add(handler);
            gsope.setSecurityHandlers(securityHandlers);
            this.globalScope = gsope;
            this.globalScope.register();
        }
        return this.globalScope;
    }

    private ScopeResolver scopeResolver;

    public ScopeResolver scopeResolver() {
        if (this.scopeResolver == null) {
            ScopeResolver resolver = new ScopeResolver();
            resolver.setGlobalScope(globalScope());
            this.scopeResolver = resolver;
        }
        return this.scopeResolver;
    }


    //--------------
    private ClientRegistry clientRegistry;

    public ClientRegistry clientRegistry() {
        if (this.clientRegistry == null) {
            this.clientRegistry = new ClientRegistry();
        }
        return this.clientRegistry;
    }

    private ServiceInvoker serviceInvoker;

    public ServiceInvoker serviceInvoker() {
        if (this.serviceInvoker == null) {
            ServiceInvoker inv = new ServiceInvoker();
            Set<IServiceResolver> serviceResolvers = new HashSet<IServiceResolver>();
            serviceResolvers.add(new ScopeServiceResolver());
            serviceResolvers.add(new HandlerServiceResolver());
            serviceResolvers.add(new ContextServiceResolver());
            inv.setServiceResolvers(serviceResolvers);
            this.serviceInvoker = inv;
        }
        return this.serviceInvoker;
    }


    public MappingStrategy mappingStrategy() {
        return new MappingStrategy();
    }

    public Context contextOflaDemo() throws Exception {
        Context context = new Context();
        context.setScopeResolver(scopeResolver());
        context.setClientRegistry(clientRegistry());
        context.setServiceInvoker(serviceInvoker());
        context.setMappingStrategy(mappingStrategy());
        context.setApplicationContext(this.applicationContext);
        context.setCoreBeanFactory(this.applicationContext);
        return context;
    }


    public Application handlerOflaDemo() throws Exception {
        Application context = new Application();
        return context;
    }


    public WebScope scopeOflaDemo() throws Exception {
        WebScope scope = new WebScope();
        scope.setName("oflaDemo");
        scope.setServer(server());
        scope.setParent(globalScope());
        scope.setContext(contextOflaDemo());
        scope.setHandler(handlerOflaDemo());
        scope.setContextPath("/oflaDemo");
        scope.setVirtualHosts("*, 0.0.0.0, localhost:5080, 127.0.0.1:5080");
//		context.addChildScope(new BroadcastScope(context,"live_123.flv"));
        scope.register();
        return scope;
    }


    @SneakyThrows
    public Server config() {
        if (this.server != null) return this.server;
        globalScope();
        scopeOflaDemo();
        return this.server;
    }

    private StatusObjectService statusObjectService;

    public StatusObjectService statusObjectService() {
        if (this.statusObjectService == null) {
            this.statusObjectService = new StatusObjectService();
            this.statusObjectService.loadStatusObjects();
            this.statusObjectService.cacheStatusObjects();
        }
        return this.statusObjectService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        ConfigServer.configServer = this;
        // set/reset property
        iniRoot();
        ConfigServer.configServer.config();
    }

    public static void iniRoot() {
        //读取VM arguments
        String root = System.getProperty("red5.root");

        //读取系统环境变量
        root = root == null ? System.getenv("RED5_HOME") : root;

        //读取VM arguments user.dir
        if (root == null || ".".equals(root)) {
            root = System.getProperty("user.dir");
        }

        //适配路径分隔符
        if (File.separatorChar != '/') {
            root = root.replaceAll("\\\\", "/");
        }

        // normalize the path substr last '/' if it presents
        if (root.charAt(root.length() - 1) == '/') {
            root = root.substring(0, root.length() - 1);
        }

        //make sure property set to VM
        System.setProperty("red5.root", root);
    }
}
