package com.classbridge.security;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    private final EntityManagerFactory emf;

    public TenantAwareJpaTransactionManager(EntityManagerFactory emf){
        super(emf);
        this.emf=emf;
    }


    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition){

        super.doBegin(transaction,definition);

        Long orgId = TenantContext.getOrgId();

        if(orgId==null){
            return;
        }

        EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);

        if(holder == null){
            throw new IllegalStateException("No EntityManager bound, hence cannot apply tenant filter");
        }

        Session session = holder.getEntityManager().unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("orgId",orgId);
    }
}
