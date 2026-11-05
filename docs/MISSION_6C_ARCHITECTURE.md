# Mission 6C — choix de séparation avant implémentation

Décision préparée avant toute modification du code M6C : conserver la planète `industrial-serpulo` et son profil M6B en accès libre, et ajouter à la fin du contenu une planète `industrial-serpulo-progressive`. Cette nouvelle planète possède une grille de secteurs distincte et réutilise le même générateur Serpulo, sans copie des machines, unités ou recettes.

Options examinées :

| Option | Conséquences | Décision |
| --- | --- | --- |
| Basculer la planète actuelle en strict | Les mêmes stocks et secteurs seraient partagés ; risque de conversion et de verrouillage des anciennes parties. | Rejetée. |
| Un gestionnaire de profils arbitraires | Nécessite une identité de profil dans les chemins de sauvegarde, paramètres de secteurs, stocks, recherche, import/export et interface. Une séparation de la seule recherche serait trompeuse. | Reportée ; architecture plus importante à auditer séparément. |
| Une seconde planète industrielle dédiée | Réutilise les espaces de noms natifs par planète pour fichiers `sector-<planète>-<id>.msav`, informations et statistiques. Recherche explicitement liée à cette planète. | Retenue : une campagne libre et une campagne stricte, réellement distinctes. |

Risques et protections prévus :

- Ajouter la planète après tous les contenus planétaires existants ; ne pas renommer les anciens contenus ou fichiers.
- Garder la clé de recherche M6B exacte pour la campagne libre. La campagne stricte utilise une autre clé contenant son propre nom.
- Aucun transfert automatique de ressources, de découvertes ou de recherches entre les deux planètes.
- Première implantation stricte confirmée par un dialogue explicite ; les implantations existantes sont reprises normalement. Aucun bouton de suppression ou de réinitialisation n’est introduit.
- Une seule campagne stricte est disponible dans cette mission. Sélectionner de nouveau sa planète reprend ses sauvegardes ; cela ne crée pas un profil supplémentaire.
- Les nœuds industriels restent détachés des listes d’auto-déblocage et des pointeurs `content.techNode` classiques. L’écran natif reçoit un contexte industriel explicite.
- Les machines sont filtrées par recherche uniquement dans la campagne stricte, avec contrôle de placement et des configurations de fabrication. Les campagnes libres et classiques conservent leurs règles.
- Les coûts de recherche des nœuds industriels peuvent être réorganisés ; aucune recette ni coût de construction partagé n’est modifié.
- Les explorations voisines et leurs coûts natifs restent disponibles. Aucun frais supplémentaire, historique payant ou verrou géographique n’est ajouté.
- Exporter les données complètes du jeu reste nécessaire pour transférer tous les secteurs et recherches ; un seul fichier de secteur n’est pas un profil complet.

La validation doit vérifier les trois espaces (classique, industriel libre, industriel strict), les paiements après redémarrage, les chemins vers graphite/silicium/titane/thorium, les coûts et prérequis affichés en portrait, et la conservation d’une vraie sauvegarde M4/M5/M6B. Les fichiers du joueur ne sont jamais utilisés comme espace de test : copies dans des profils cloud séparés uniquement.
