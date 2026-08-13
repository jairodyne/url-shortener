package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlEntity;

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
public class UrlRepository {

    private final EntityManagerFactory emf;

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
            return Optional.ofNullable(findByQuery("code", code, em));
        } finally {
            em.close();
        }
    }

    public Optional<UrlEntity> findByAlias(String alias) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(findByQuery("alias", alias, em));
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

    private UrlEntity findByQuery(String field, String value, EntityManager em) {
        try {
            return em.createQuery(
                            "select u from UrlEntity u where u." + field + " = :value", UrlEntity.class)
                    .setParameter("value", value)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
