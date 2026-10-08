package com.lacivita.turnos.shared.tenancy;

import javax.sql.DataSource;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

/** Conecta el aislamiento entre negocios con Spring, Hibernate y el pool de conexiones. */
@Configuration(proxyBeanMethods = false)
class TenancyConfiguration {

    /**
     * Intercepta los métodos {@link BusinessScoped}. Corre antes que todo lo demás (permisos y
     * transacción), así ambos ven el negocio correcto.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor businessScopeAdvisor() {
        var advisor = new DefaultPointcutAdvisor(
                new AnnotationMatchingPointcut(null, BusinessScoped.class, true), new BusinessScopeInterceptor());
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return advisor;
    }

    /** Envuelve el pool de conexiones de la aplicación. Flyway usa su propia conexión y no pasa por acá. */
    @Bean
    static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    @Bean
    HibernatePropertiesCustomizer tenantResolverCustomizer() {
        return properties ->
                properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new BusinessTenantResolver());
    }
}
