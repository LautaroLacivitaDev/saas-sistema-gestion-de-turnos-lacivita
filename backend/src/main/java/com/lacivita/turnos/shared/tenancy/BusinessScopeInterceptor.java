package com.lacivita.turnos.shared.tenancy;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;

/** Fija el {@link TenantContext} de los métodos {@link BusinessScoped} con el valor de su {@link BusinessId}. */
class BusinessScopeInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        UUID businessId = businessIdOf(invocation);
        return TenantContext.callInBusiness(businessId, invocation::proceed);
    }

    private static UUID businessIdOf(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        if (invocation.getThis() != null) {
            method = AopUtils.getMostSpecificMethod(method, invocation.getThis().getClass());
        }
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(BusinessId.class)) {
                if (!(invocation.getArguments()[i] instanceof UUID businessId)) {
                    throw new IllegalArgumentException(
                            "El parámetro @BusinessId de " + method + " es nulo o no es UUID");
                }
                return businessId;
            }
        }
        throw new IllegalStateException(method + " es @BusinessScoped pero no tiene un parámetro @BusinessId");
    }
}
