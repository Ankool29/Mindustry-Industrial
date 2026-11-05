# Mission 6D — coûts progressifs de colonisation

La surcharge concerne uniquement **Serpulo industrielle — progression**. Le Mindustry original et la campagne industrielle libre gardent leurs règles de lancement. Aucun contenu, recette, coût de construction partagé ni algorithme de simulation des secteurs non visités n’est modifié.

## APK et installation

**[Télécharger l’APK M6D et ses sources](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42031564912/artifacts/13416407991)** · [Compilation réussie](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42031564912) · [Code compilé `cec7286a`](https://github.com/Ankool29/Mindustry-Industrial/commit/cec7286a79fb4bd3d48b7864818d43d1305f6339).

APK téléchargé puis vérifié : signature valide avec le certificat précédent SHA-256 `61254fd2280bf34aa64bd06405cf0e2bc9b2d8fe7df4cc615c39983bcdfd8086`, `versionName=8-industrial-m6d-160.7`, alignement Android 16 Kio validé. SHA-256 APK : `075c9e81b1f330914c14477d7db53c969858402ac93ee72a081a1aed850bb16e`. Java 17, Gradle 8.14.3, AGP 8.10.1, SDK 36, Android minimal 21. L’artefact expire le 5 décembre 2026 ; le workflow permet de le reconstruire avec la même clé.

Extraire l’archive puis installer `Mindustry-Industrial-v160.7-m6d.apk` **par-dessus M6C, sans désinstaller**. Le package reste `io.anuke.mindustry.industrial.m3`, le code de version passe à **8**. L’archive comprend les sources correspondantes Mindustry/Arc et leurs licences.

## Choix des secteurs conservés

L’audit a révélé **six voisins autour de 170**, contrairement aux cinq mentionnés initialement. Le joueur a explicitement demandé de conserver les choix M6C même s’il y en a six. `IndustrialCampaign.canExplore` et `findLauncher` ne changent pas : accès au secteur initial, aux bases existantes et aux voisins d’une base. Aucun secteur lointain n’est rendu accessible. La grille et le générateur ne changent pas. La surcharge ne requiert aucune recherche ni matière avancée.

## Tarifs

Le numéro ci-dessous désigne une implantation économique réussie, et non une visite.

| Implantation | Surcharge cuivre | Surcharge plomb |
| --- | ---: | ---: |
| 1re | 0 | 0 |
| 2e | 0 | 0 |
| 3e | 250 | 200 |
| 4e | 500 | 400 |
| 5e | 750 | 600 |
| 6e | 1000 | 800 |
| 7e | 1250 | 1000 |
| 8e | 1500 | 1200 |
| 9e | 1750 | 1400 |
| 10e et suivantes | 2000 | 1600 |

Le schéma du noyau et le chargement emporté gardent leurs prix natifs. Pour un noyau de base : 1000 cuivre/800 plomb, donc à plafond **3000 cuivre/2400 plomb**, avant chargement, sous la capacité native de **4000 par matière**. Le chargement maximal réserve schéma + surcharge puis applique la capacité native du chargement. Aucun stockage artificiel n’est ajouté. Un schéma plus coûteux peut demander une source mieux équipée ou le choix du noyau de base ; un total non finançable reste refusé.

Les valeurs sont regroupées dans `IndustrialExpansion.Policy` : `freeLandings`, `copperStep`, `leadStep`, `copperCap`, `leadCap`. Chaque nouvelle économie en conserve une copie sérialisée dans son registre, pour ne pas changer silencieusement ses paramètres lorsqu’un défaut est rééquilibré. Pas d’éditeur d’équilibrage tactile dans cette mission ; une modification contrôlée du registre ou un outil de migration explicite serait nécessaire pour rééquilibrer les parties déjà commencées. Aucune modification manuelle des données du joueur n’est nécessaire pour jouer ou tester.

## Compteur et compatibilité M6C

- Une nouvelle campagne commence à zéro ; son premier atterrissage réussi établit le compteur à un.
- Une campagne M6C possédant déjà des sauvegardes ou traces d’implantation reçoit **un point de départ économique**, quel que soit le nombre de bases préexistantes. Aucun stock n’est prélevé à la migration, aucune recherche, construction ou base n’est réinitialisée. Le nombre de secteurs préexistants est enregistré pour expliquer la transition.
- Le premier nouveau déploiement après cette migration est donc l’implantation économique n° 2, sans surcharge ; le suivant est la n° 3, à 250/200. Le compteur n’est volontairement pas présenté comme le nombre total de colonies historiques.
- Visiter un secteur avec une base reste gratuit et ne compte pas. Abandonner ne réduit jamais le compteur. **Réimplanter un noyau après abandon est un nouveau déploiement**, facturé au tarif courant, puis comptabilisé une fois réussi. Cela ne permet pas de revenir aux premiers tarifs.
- **Secours natif si toutes les bases ont disparu :** une confirmation explicite permet de reprendre au secteur initial avec le noyau et le stock de départ natifs, puisqu’aucune source ne peut payer. Cette exception n’est pas une colonisation gratuite au choix : seul le secteur initial est concerné, les recherches et le compteur restent conservés, et cette implantation augmente le compteur. Tant qu’une autre base existe, le raccourci gratuit est refusé : il faut une source voisine finançable. Aucune règle de sélection des secteurs n’est modifiée.
- Le registre est conservé dans `industrial-expansion-v1-industrial-serpulo-progressive`, à côté des recherches M6C, sans changer leur espace de noms. Aucune donnée économique n’est créée pour les deux autres campagnes.

## Paiement et interruptions

Le [document technique](https://github.com/Ankool29/Mindustry-Industrial/blob/industrial-android/docs/MISSION_6D_TRANSACTIONS.md) expose les choix et limites. Le moteur écrit séparément la sauvegarde `.msav` et les paramètres : une transaction atomique entre les deux n’est pas disponible.

1. Avant confirmation, le dialogue n’établit aucun journal de départ et ne prélève rien.
2. À confirmation, le stock réel de la source est revérifié ; le secteur source est sauvegardé. Un journal `PREPARED` conserve identifiant unique, source, destination et montant complet figé. **Aucun débit à ce stade.** Une deuxième confirmation avec un journal en cours est refusée.
3. Après confirmation, le départ strict passe directement à l’écran de chargement. La source reste brièvement en pause, ce qui empêche machines, constructions et tours hors écran de consommer les ressources validées pendant le délai. L’animation de décollage reste inchangée dans les deux autres campagnes. Le monde destination reçoit ce marqueur dans les `rules.tags`, puis sa sauvegarde initiale est écrite.
4. Seulement après cette sauvegarde réussie, les ressources sont retirées du stock hors écran de la source. Stock, compteur et clôture du journal sont écrits ensemble dans les paramètres natifs. Au prochain chargement de la source, `SectorInfo.write` réapplique son stock, comme dans le moteur d’origine.
5. Au démarrage ou avant de revenir à la source, un journal interrompu est rapproché du marqueur **lu dans la sauvegarde destination** : marqueur présent = terminer une fois le débit ; absent = annuler sans frais. Un ancien marqueur de recolonisation ne correspond pas au nouvel identifiant.
6. Le premier atterrissage et la reprise sans source possèdent aussi un journal avant leur sauvegarde (`source = -1`, montant vide), pour ne pas perdre ou doubler leur incrémentation en cas d’interruption entre les deux écritures.
7. Les erreurs synchrones de sauvegarde sont signalées. Une erreur de clôture restaure le stock et le compteur en mémoire et conserve le journal, pour permettre une nouvelle tentative sans double débit. Une destination illisible ne provoque pas d’effacement de sauvegarde par ce service.

Ce protocole couvre les interruptions entre étapes persistées et les erreurs d’E/S testées ; **ce n’est pas une garantie ACID face à une coupure au milieu de l’écriture de `settings.bin`**, une corruption simultanée ou une restauration de fichiers d’époques différentes. Arc utilise ses sauvegardes natives des paramètres et n’offre pas ici de remplacement atomique avec synchronisation disque. Utiliser l’export/import **complet** des données pour restaurer ou transférer une campagne. Aucun service de paiement parallèle ni modification de la simulation économique des secteurs n’est ajouté.

## Interface mobile

Le dialogue natif de lancement est conservé. Dans la campagne stricte, il ajoute destination et numéro économique, puis une liste verticale défilante. Chaque matière indique séparément **noyau/schéma**, **chargement**, **expansion**, **total** et **disponible au départ**. Un manque est rouge et désactive le départ. Le bouton de chargement maximal réserve les coûts fixes. Les deux autres campagnes gardent la présentation native.

## Validation

**385 tests automatisés, 0 échec, 0 erreur, 0 ignoré** sur le code final en local. La suite GitHub Actions finale donne les mêmes résultats : **385/385**, XML téléchargés et recomptés. [Rapports JUnit](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42031564912/artifacts/13416113701) · [Preuves de compilation](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42031564912/artifacts/13416577778).

Les **18 nouveaux tests** couvrent :

1. Premier atterrissage d’une campagne neuve et persistance du compteur à un.
2. Prix des 2e, 3e, 4e, 5e et 10e implantations, plafonds, grands compteurs et paramètres modifiés.
3. Compatibilité du noyau de base avec le plafond, sans modification de sa capacité.
4. Ensemble des six voisins initiaux M6C conservé et refus d’un secteur lointain.
5. Migration de plusieurs bases M6C sans prélèvement ni frais rétroactifs.
6. Annulation d’un départ non terminé, sans débit ni incrémentation.
7. Stock insuffisant : aucun journal ni prélèvement.
8. Débit unique après sauvegarde, rappels répétés sans effet et persistance après rechargement des paramètres.
9. Interruption après sauvegarde destination : reprise et débit unique au redémarrage.
10. Interruption avant sauvegarde destination : annulation sans frais au redémarrage.
11. Refus d’une double confirmation tant qu’un départ est en cours.
12. Visite gratuite, abandon sans baisse du compteur et recolonisation facturée au palier courant.
13. Campagnes libre et classique : aucun registre économique, surcharge ni changement de stock.
14. Échec synchrone injecté de l’écriture des paramètres : retour arrière en mémoire, journal conservé puis reprise sans double paiement.
15. Ancien marqueur destination insuffisant pour facturer une nouvelle implantation échouée.
16. Reprise après abandon de toutes les bases : compteur conservé et augmenté une seule fois, puis persistant.
17. Refus du raccourci gratuit au secteur initial tant qu’une autre base subsiste, sans modifier les choix de carte.
18. Premier atterrissage interrompu après la sauvegarde : incrémentation reprise une seule fois au démarrage, sans source à débiter.

Le banc de test génère des mondes et fichiers temporaires dédiés ; il ne touche pas les sauvegardes du joueur. Les 367 tests antérieurs restent dans la suite complète, dont la progression, les paiements de recherche, les sauvegardes et les systèmes de secteurs.

Le client graphique du code final a été lancé **dans deux processus distincts**, en disposition mobile **600 × 1000**. Une copie indépendante des données de validation M6C a conservé la recherche de la presse à graphite. La migration a établi un compteur à un ; annulation et stock insuffisant n’ont créé aucun paiement. La 2e implantation a coûté seulement le noyau natif : source passée de 4000/4000 à 3000 cuivre/3200 plomb. Le retour à cette source n’a pas modifié le compteur.

La 3e implantation, avec 100 cuivre/50 plomb de chargement et une surcharge de 250/200, a laissé **2650 cuivre et 2950 plomb** à la source. La destination a reçu exactement le chargement natif ; le journal a été clôturé. Après fermeture complète, le deuxième processus a retrouvé ces stocks et le compteur à trois, puis visité gratuitement la destination. Le devis suivant a affiché 500/400 ; l’annuler n’a rien changé. Les trois noyaux natifs de Serpulo (base, fondation, noyau avancé) ont aussi été contrôlés avec leurs vrais tableaux de construction et le plafond de surcharge : chaque matière tient dans leur capacité native. Le chargement maximal, le menu natif de la campagne libre et les vagues/règles de Serpulo classique ont aussi été vérifiés. Une garde de validation a surveillé l’absence de ticks de simulation de la source pendant le journal préparé ; les départs ont été testés même avec la préférence native d’animation activée.

Une autre copie de test a invoqué l’abandon natif de ses trois bases industrielles, sans supprimer de fichier : le compteur est resté à trois. Annuler le dialogue de secours n’a rien changé. Confirmer la reprise a conservé la recherche de la presse, donné le stock initial natif de 100 cuivre et porté le compteur à quatre, une seule fois. Un processus suivant a confirmé cette persistance et la surcharge suivante de 750 cuivre. Le [script de reprise](docs/mission6d/recovery-check.js) est réservé à cette copie jetable ; **ne pas l’installer sur les données du joueur**.

Un dernier processus a lancé un départ payant directement depuis la carte au menu principal, sans charger préalablement la source : menu conservé jusqu’au chargement, arrivée sans défaite et paiement unique vérifiés ([script](docs/mission6d/menu-check.js)).

[Résultats des cinq processus](docs/mission6d/ui-results.txt) · [Script de validation](docs/mission6d/ui-check.js). **Script réservé aux copies de test**, absent de l’APK : il approvisionne ses noyaux à 4000 cuivre/4000 plomb et choisit un chargement de test pour vérifier les montants exacts. Il ne doit pas être installé sur les sauvegardes du joueur. Aucun fichier du joueur n’a été supprimé ou réinitialisé. Les noms de contenus anglais dans les captures proviennent du chargement initial de cette configuration de test ; les nouvelles commandes françaises sont chargées ensuite.

| Vérification mobile | Capture |
| --- | --- |
| Deuxième implantation sans surcharge | [Devis en portrait](docs/mission6d/second-colony-free.png) |
| Ressources insuffisantes, départ désactivé | [Refus](docs/mission6d/insufficient-resources.png) |
| Noyau + chargement + surcharge 250/200 | [Troisième implantation](docs/mission6d/third-colony-breakdown.png) |
| Chargement maximal finançable | [Maximum](docs/mission6d/maximum-cargo.png) |
| Arrivée et retour sans facturation | [Arrivée](docs/mission6d/third-colony-landed.png) · [Retour](docs/mission6d/return-free.png) |
| Compteur persistant, devis suivant 500/400 | [Quatrième implantation](docs/mission6d/fourth-colony-portrait.png) |
| Secours explicite après perte de toutes les bases | [Confirmation](docs/mission6d/recovery-confirmation.png) · [Arrivée](docs/mission6d/recovery-landed.png) |
| Présentation native conservée dans la campagne libre | [Lancement libre](docs/mission6d/legacy-native-launch.png) |

## Procédure de test sans effacement

1. Mettre à jour par-dessus M6C. Une exportation complète préalable permet de conserver une copie de la progression ; **ne pas désinstaller, réinitialiser ou supprimer des secteurs pour ce test**.
2. Ouvrir **Serpulo industrielle — progression**, visiter une base existante, puis la carte planétaire. Choisir un secteur accessible sans base ; les choix de M6C restent les mêmes.
3. Préparer les ressources normales du noyau. Ouvrir le départ : vérifier le secteur, le schéma choisi, le chargement, la surcharge et les totaux. Dans une ancienne campagne M6C, ce premier nouveau départ est sans surcharge.
4. Toucher **Retour** : rien n’a été prélevé. Rouvrir et confirmer quand les totaux sont finançables ; attendre l’arrivée.
5. Revenir dans la source : aucun prix de visite ni nouvelle incrémentation. Pour un autre secteur vide, le devis suivant montre 250 cuivre/200 plomb de surcharge après la première implantation M6D réussie d’une ancienne campagne.
6. Fermer normalement le jeu et le rouvrir : vérifier les stocks et le même palier pour le prochain départ. Ne pas provoquer volontairement une coupure au milieu de l’écriture des sauvegardes sur les données de jeu.
7. Vérifier **Serpulo industrielle** et **Serpulo** : leurs recherches et lancements restent ceux d’avant. Aucun abandon n’est nécessaire pour valider la nouvelle économie ; les cas d’abandon et de panne sont couverts sur des copies de test.

Limites : la validation graphique dans le cloud ne remplace pas l’installation de cet APK et l’essai tactile physique sur le S25 Ultra. Les modifications de sauvegardes par des mods et le multijoueur entre versions différentes ne sont pas couverts. Les artefacts GitHub sont conservés 30 jours ; le workflow permet de reconstruire l’APK avec la clé précédente, sans désinstallation.
