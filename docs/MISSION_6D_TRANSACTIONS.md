# M6D — décisions avant implémentation

Uniquement la planète industrielle progressive. Aucun changement de canExplore/findLauncher, grille, générateur ou simulation hors écran.

## Migration

Pas de frais rétroactifs. À la première utilisation, toute campagne M6C possédant au moins une sauvegarde/trace d'implantation est considérée comme une campagne déjà commencée (compteur économique 1). Le nombre de bases préexistantes est conservé comme information de migration, sans les facturer. Le premier déploiement M6D est donc sans surcharge ; le suivant coûte 250 cuivre/200 plomb. Une nouvelle campagne commence à 0 ; son premier atterrissage établit le compteur à 1. Le compteur mesure les implantations de la nouvelle économie, pas le nombre de bases actuellement occupées. Visiter une base existante ne compte pas ; réimplanter après abandon compte et paie le tarif courant. Aucun abandon ne réduit le compteur.

## Prix

Paramètres indépendants du contenu natif : deux premières implantations sans surcharge, puis 250 cuivre/200 plomb par palier, plafonds 2000/1600. Le coût du schéma et le chargement restent natifs. Le chargement maximal réserve d'abord schéma + surcharge. Avec le noyau de base, même la surcharge maximale reste sous ses 4000 places par matière ; les schémas plus coûteux doivent pouvoir être financés par la source, sans agrandir artificiellement le stockage.

## Journal de lancement

Le moteur ne propose pas de transaction atomique couvrant fichier .msav et paramètres. Une seule transaction persistante par planète est autorisée. Avant départ : validation source/voisinage/destination/stock, sauvegarde source, journal PREPARED avec identifiant, coûts figés et destination, aucun débit. Avant la sauvegarde initiale de destination : marqueur d'identifiant dans rules.tags. Après sauvegarde réussie : débit hors écran du stock source et mise à jour compteur + journal, puis forceSave des paramètres. Le stock hors écran et le journal sont dans le même fichier settings natif ; SectorInfo.write réapplique ce stock au prochain chargement de la source.

Au démarrage/reprise, un journal dont le marqueur existe dans la sauvegarde destination est terminé une seule fois ; sinon il est annulé sans frais. Aucun secteur n'est effacé pour récupérer. Les erreurs d'E/S sont signalées ; le journal est conservé pour reprise. Ce protocole couvre interruptions ordinaires entre étapes, pas une corruption simultanée des sauvegardes/paramètres ou une restauration manuelle désynchronisée. Exporter l'ensemble des données pour transférer ou restaurer.

## Écart constaté dans la demande

Le code et le test de M6C proposent **six** voisins autour de 170 (et non cinq). M6D conserve exactement `canExplore` et `findLauncher` ; aucun filtrage arbitraire n'est ajouté. Le joueur a explicitement confirmé de conserver les choix M6C, même s'il y en a six. Les références à cinq dans la demande ne correspondent pas à la version auditée.

Les paramètres natifs Arc ne garantissent pas un remplacement atomique/fsync de settings.bin. Le journal traite les interruptions entre étapes persistées ; une coupure au milieu de l'écriture des paramètres peut provoquer une restauration de leur sauvegarde native et perdre des mises à jour récentes. Le protocole n'annonce donc pas une garantie ACID face à une corruption ou une restauration désynchronisée. Les erreurs synchrones de sauvegarde sont propagées, avec retour arrière en mémoire et journal conservé pour nouvelle tentative.
