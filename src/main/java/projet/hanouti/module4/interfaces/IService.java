package projet.hanouti.module4.interfaces;

import java.util.List;

/**
 * Interface générique de service — structure imposée par le prof.
 * Chaque service du projet implémente cette interface.
 *
 * @param <T> le type de l'entité (Paiement, Panier, etc.)
 */
public interface IService<T> {
    void    addEntity(T t);
    void    deleteEntity(T t);
    void    updateEntity(int id, T t);
    List<T> getData();
}