package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlEntity;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import java.util.Optional;

/**
 * Persiste e consulta {@link UrlEntity}. Cada operação abre a própria
 * {@link EntityManager} e controla a transação (persistence unit RESOURCE_LOCAL),
 * o que mantém a classe testável sem container.
 */
@ApplicationScoped
public class UrlRepository {

    private final EntityManagerFactory emf;

    protected UrlRepository() {
        this(null);
    }

    @Inject
    public UrlRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public UrlEntity save(UrlEntity entity) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entity);
            tx.commit();
            return entity;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<UrlEntity> findByCode(String code) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(
                    em.createQuery("select u from UrlEntity u where u.code = :code", UrlEntity.class)
                            .setParameter("code", code)
                            .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    public boolean existsByCode(String code) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "select count(u) from UrlEntity u where u.code = :code", Long.class)
                    .setParameter("code", code)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public void incrementClicks(String code) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("update UrlEntity u set u.clickCount = u.clickCount + 1 where u.code = :code")
                    .setParameter("code", code)
                    .executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
