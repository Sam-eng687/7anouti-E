package projet.hanouti.GestionCommandes.interfaces;

import projet.hanouti.GestionCommandes.entities.Commande;

import java.util.List;

import java.util.List;

import java.util.List;

public interface IntService<T> {

    /**
     * Ajoute une entité en base de données.
     * @param entity l'entité à persister
     * @return l'entité ajoutée (avec son ID généré)
     */
    T add(T entity);

    /**
     * Supprime une entité par son ID.
     * @param id identifiant de l'entité
     */
    void delete(int id);

    /**
     * Met à jour une entité existante.
     * @param entity l'entité avec les nouvelles valeurs
     * @return l'entité mise à jour
     */
    T update(T entity);

    /**
     * Récupère une entité par son ID.
     * @param id identifiant de l'entité
     * @return l'entité trouvée, ou null si inexistante
     */
    T getById(int id);

    /**
     * Récupère toutes les entités de ce type.
     * @return liste de toutes les entités
     */
    List<T> getAll();
}