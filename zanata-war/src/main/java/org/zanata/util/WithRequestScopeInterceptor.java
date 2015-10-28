package org.zanata.util;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import lombok.extern.slf4j.Slf4j;

import org.apache.deltaspike.cdise.api.ContextControl;

/**
 *
 * @author Sean Flanigan
 */
@Interceptor
@WithRequestScope
@Slf4j
public class WithRequestScopeInterceptor implements Serializable {
    @Inject
    private ContextControl ctxCtrl;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext invocation) throws Exception {
        log.info("starting request scope");
        //this will implicitly bind a new RequestContext to the current thread
        ctxCtrl.startContext(RequestScoped.class);
        try {
            return invocation.proceed();
        } finally {
            // stop the RequestContext to ensure that all request-scoped beans
            // get cleaned up.
            ctxCtrl.stopContext(RequestScoped.class);
            log.info("stopped request scope");
        }
    }

}