# M7C.1 — diagnostic des livraisons hors écran

Statut : correction ciblée validée dans le cloud, essais Android physiques attendus, branche `feature/m7c-conservative-transports`, suite de la PR #2. Base : `9713e80cc` ; APK M7C précédent : `05fcd7ab8`. Aucun travail M7D, aucune fusion. Voir le [rapport M7C](MISSION_7C_VALIDATION.md) pour l’architecture et les limites historiques.

## Conclusion et reproduction

Le constat « rien n’arrive avant de visiter B » recouvre des situations différentes. Nous n’avons pas le journal du S25 : l’origine exacte de cette partie n’est pas établie. Les cas suivants ont été reproduits sur des mondes temporaires isolés ; aucune sauvegarde du joueur n’a été effacée ou réinitialisée.

1. **Limitation volontaire et diagnostic trompeur** : avec une réception de B connue, A envoie 100 cuivre et le joueur reste dans C. Après 7 199 ticks, le lot reste `transit` ; au 7 200e tick, `Universe.update()` exécute un tour et le lot devient `overlay`. Le tampon virtuel contient 100, son eau connue passe de 3 000 à 1 500, le fichier du monde B reste identique et le noyau B n’est pas crédité. M7C regroupait ce lot sous « En transit / attente de tampon » et gardait « Reçus confirmés : 0 », ce qui masquait la réservation effective.
2. **Défaut réel reproduit** : une arrivée est commencée dans B, 200 unités d’eau sont consommées, puis B est sauvegardé et C chargé. Dans M7C, le lot reste `arriving` après le tour : les réceptions `busy` sont toujours ignorées. Après certaines reprises/créations de secteur, il devient `transit` mais son ancienne réception reste `busy`, donc toujours inutilisable hors écran. Le test de régression a échoué avant correction : `expected overlay, was arriving`.
3. **Destination inconnue ou indisponible** : sans relevé M7C de sa réception, B ne peut pas être supposé équipé/alimenté. Un tampon plein, désactivé, non configuré pour la matière, sans eau ou sans base doit aussi attendre. Rien ne doit être fabriqué pour contourner ces conditions.
4. **Temps de jeu** : la cadence est un tour de 120 secondes de simulation, pas 120 secondes civiles. `Logic.update()` n’appelle `Universe.update()` qu’en campagne non pausée ; carte planétaire et diagnostic mettent le solo en pause. L’application fermée ne fait pas avancer les lots.

## Correction retenue

Une réception sauvegardée enregistre maintenant dans le journal son identifiant de colis, sa matière en cours d’arrivée et l’eau déjà consommée. Une simple réservation en mémoire ne suffit pas : seul un point de sauvegarde canonique atteste cette liaison.

- Une arrivée finie et appariée reste `arriving` lors de la reprise si son crédit n’est pas encore sauvegardé ; elle n’est plus systématiquement renvoyée dans une file incapable de l’utiliser.
- Au prochain tour, une réception inactive, activée, vide et disposant de l’eau restante termine virtuellement cette arrivée : `arriving → overlay`. Exemple : 1 500 requis − 200 déjà consommés = **1 300** réservés, sans double consommation des 200.
- Au chargement ultérieur de B, toutes les liaisons, matières, tampons et niveaux d’eau sont vérifiés **avant** le moindre crédit. Le lot est appliqué une fois, l’animation appariée est terminée, l’eau restante est déduite, puis une sauvegarde confirme le reçu. Une liaison incohérente provoque un blocage signalé, pas un crédit supposé.
- Le diagnostic sépare maintenant **transit**, **réservé hors écran**, **atterrissage**, **crédit local à sauvegarder**, et **reçu confirmé après sauvegarde du monde B**. Il affiche aussi la raison disponible d’une attente. Lecture détachée : aucun scan d’un monde inactif ni écriture lors de la consultation.

## États et conditions

| État | Signification / suite |
| --- | --- |
| `prepared` | Intention journalisée, débit RAM ; attendre la sauvegarde source avant de rendre le lot livrable. |
| `transit` | Débit prouvé ; attendre une réception disponible. Aucun débit moyen ne crée un nouveau lot. |
| `arriving` | Arrivée active engagée ; hors écran, achèvement possible seulement si le relevé sauvegardé prouve la liaison et l’eau restante. |
| `overlay` | Quantité réellement réservée dans un tampon virtuel fini ; elle n’est pas simultanément ajoutée au noyau. Application au chargement B. |
| `credit` | Crédit actif appliqué en RAM, en attente du point de sauvegarde ; reprise par marqueur idempotent. |

Conditions hors écran : planète progressive mise à jour, B inactif avec base, relevé connu, plateforme activée et vide, matière/capacité compatibles, eau suffisante ; pour une arrivée engagée, identifiant et état sauvegardés appariés. Le lot reste réservé si le tampon est occupé : aucun convoyeur virtuel ne le vide.

## Compatibilité et conservation

Le format global du journal reste v1, identité, identifiants de colis et marqueurs inchangés. Les nouveaux champs sont facultatifs. Un ancien `overlay` M7C sans coût explicite conserve le coût complet de 1 500. Une ancienne arrivée M7C sans appariement sauvegardé est conservée, mais nécessite **une visite et une sauvegarde** pour relever sa liaison avant achèvement hors écran. Aucune migration inventant un stock.

Aucun changement du format des fichiers de monde ou de la révision des bâtiments ; aucune recette, recherche, règle d’exploration, coût M6D ou moyenne native de production modifié. Classique et libre restent sur leurs chemins natifs. Les exports statistiques restent neutralisés uniquement dans la progressive. Une seule carte sectorielle est simulée complètement.

## Fichiers et tests

- `core/src/mindustry/game/IndustrialTransports.java` : relevé des arrivées appariées, reprise, réservation de l’eau restante et diagnostic distinct.
- `tests/src/test/java/IndustrialTransportTests.java` : dix nouvelles régressions, dont horloge native A → B depuis C, absence de visite B, reprise dans C, deux arrivées, manque d’eau, ancienne forme JSON M7C et liaison modifiée.
- Workflow Android : type/version M7C.1, versionCode 11 ; identifiant et signature conservés.
- Documents du harnais concernés et présent rapport ; M7C historique conservé.

Résultats ciblés initiaux : **53 tests réussis, 0 échec, 0 erreur, 0 ignoré** (41 transports + 12 diagnostics). Avant correction : reproduction positive du tampon normal, puis régression `arriving → overlay` en échec comme décrit ci-dessus. Suite locale complète : **438 réussis, 0 échec, 0 erreur, 0 ignoré**, après correction. Contrôle portrait cloud : client reconstruit depuis ce code, mode mobile **600 × 1000**, source A = 170, B = 171, C = 15. Après préparation/sauvegarde de B puis départ A → B, un tour économique forcé dans cette fixture affiche **« Réservés hors écran : 100 »**, **« En transit : 0 »**, **« Reçus confirmés : 0 »**. C reste chargé ; le fichier B et le journal restent identiques lors de dix actualisations du diagnostic. Défilement uniquement vertical, boutons accessibles et capture inspectée. Trace : `M7C1_UI_PORTRAIT_VIRTUAL_100_FROM_C_NO_B_LOAD_READONLY_OK`. Cette fixture n’est jamais livrée dans l’APK. Ce contrôle est un client desktop en mode mobile, pas un essai physique Android.

Les 41 tests de transport incluent les cas suivants :

| Scénario | Résultat |
| --- | --- |
| Horloge native, A → B, C actif | `transit` à 7 199 ticks, `overlay` à 7 200 ; B non chargé/réécrit. |
| Diagnostic sans retour B | 100 réservés, 0 transit, 0 reçus confirmés ; lectures sans mutation. |
| Arrivée sauvegardée après 200 d’eau consommés | Achèvement virtuel ; coût restant 1 300 ; après chargement, 100 cuivre et 1 500 eau restants. |
| Redémarrage dans C, répétition des tours et du journal | Même ID, un seul lot ; après chargement/sauvegarde, pas de deuxième crédit. |
| Deux arrivées avec ordre de réception différent | Deux tampons distincts, 200 objets au total ; 1 500 d’eau restants dans chacun. |
| Eau restante insuffisante, tampon occupé ou absent | Conservation du lot, attente ; aucune livraison supposée. |
| Arrivée sauvegardée dont la liaison a été altérée | Exception de transport avant crédit, fichier conservé. |
| Forme JSON M7C reconstituée sans les nouveaux champs | Ancien `overlay` appliqué avec coût complet ; ancienne arrivée conservée puis débloquée par une visite/sauvegarde. |
| Fixture M7B et campagnes classique/libre | Tests existants réussis ; aucune migration destructive ni changement de réception native dans les autres campagnes. |

Les interruptions sont reproduites par rechargement de journaux et fichiers temporaires, pas par coupure électrique sur S25. Le test M7B de production reste **5 graphite réels → 35 estimés hors écran → 35 au retour** : il ne constitue pas une livraison M7C.1. L’ancienne duplication statistique des réceptions reste bloquée dans la progressive.

**CI finale : 438 réussis, 0 échec, 0 erreur, 0 ignoré, 16 suites JUnit**, au commit `32ba1e1210c873a47d45009bfe7c24591f7dcc1f`. Les comptes ont été recalculés depuis tous les XML de l’[artefact JUnit](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42765979342/artifacts/13739920886) ; 41 tests de transport, 12 diagnostics, 385 autres. Aucun cumul avec les exécutions locales. Les [journaux de compilation et de tests](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42765979342/artifacts/13739891112) et le [workflow final réussi](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42765979342) sont les preuves de livraison.

Mesures de la CI finale (Java 17, Ubuntu 22.04, **pas S25**) : après chauffe, moyenne de 1 000 appels du service inactif ; le JIT explique les variations non monotones. Il ne s’agit pas de FPS ni d’une mesure de toute la simulation.

| Colonies synthétiques | Temps moyen / appel | Allocations / appel | Journal |
| --- | ---: | ---: | ---: |
| 5 | 2,21 µs | 120 octets | 990 octets |
| 20 | 2,33 µs | 459 octets | 3 421 octets |
| 50 | 0,86 µs | 1 176 octets | 8 641 octets |
| 100 | 1,02 µs | 2 376 octets | 17 341 octets |

File pleine : 512 admissions durables, **0,79 ms/admission** en moyenne, journal **68 080 octets** ; le lot supplémentaire reste dans la rampe. Ces bancs mesurent principalement des réceptions au repos et le journal de départ, pas un réseau massif d’arrivées simultanées. Les allocations ne sont pas la mémoire résidente ; l’identité ajoute 36 octets. Les écritures restent synchrones et les latences physiques Android restent à tester. Aucun nouvel historique permanent ni traitement à chaque image.

## Limites conservées

- Le correctif ne dépose pas des objets dans un noyau inactif, ne vide pas une réception et n’alimente pas une usine inactive en continu. Une réservation `overlay` peut donc être visible hors écran alors que les objets physiques n’apparaissent qu’au chargement B : c’est le modèle M7C, pas un échec de livraison.
- Une arrivée préexistante non appariée, un relevé absent ou périmé reste en attente ou peut nécessiter une visite. Aucun second monde n’est chargé pour reconstituer ces informations.
- Résolution par tours économiques, pas simulation détaillée à 2,5 secondes de l’animation native hors écran.
- Transactions entre plusieurs fichiers toujours non atomiques ; pas de garantie absolue en cas de coupure électrique ou restauration partielle d’un ancien journal valide. Export complet et cohérent requis ; ne jamais effacer le journal/identité pour débloquer une partie.
- Production native estimée toujours non conservative : le cas graphite M7B 5 réels → 35 estimés relève de M7D et reste exposé par les tests. Ce correctif n’en fait pas des cargaisons réelles.
- Validation physique du correctif sur S25 attendue ; performances du stockage et confort tactile ne se déduisent pas des seuls essais serveur.

## APK et protocole S25

- [Télécharger l’artefact APK M7C.1](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42765979342/artifacts/13739024415) ; extraire **`Mindustry-Industrial-v160.7-m7c1.apk`** du ZIP. Connexion GitHub éventuellement requise, rétention annoncée jusqu’au **10 décembre 2026**.
- Code compilé : **`32ba1e1210c873a47d45009bfe7c24591f7dcc1f`**, concordant dans le workflow, `BUILD-INFO.txt`, `versions.txt` et l’en-tête Git de l’archive des sources correspondantes. Le commit documentaire final n’altère pas ce code.
- SHA-256 de l’APK téléchargé et recalculé : **`e824134049dcdbfc7369a329a8f8f2283b6b87e7c4aa1e5b3f01b4b84efe911a`**, conforme à `SHA256SUMS.txt`. Digest du ZIP d’artefact vérifié séparément : `2ab2703eba537cbc601c3ddb0e28d1af79ae2d3d8ddfd5f7da2176c27e42ef02`.
- `aapt` confirme **`8-industrial-m7c1-160.7`**, **versionCode 11**, **`io.anuke.mindustry.industrial.m3`**, minSdk déclaré 21, targetSdk 36. ARM64 présent ; contrôle `zipalign -c -P 16 4` réussi.
- `apksigner verify --verbose --print-certs` sur le binaire reçu : signature valide v1/v2, certificat SHA-256 **`61254fd2280bf34aa64bd06405cf0e2bc9b2d8fe7df4cc615c39983bcdfd8086`**, identique à M7C (versionCode 10) et M7B (9). Aucun remplacement de clé, aucune désinstallation demandée. L’installation effective de cette version sur S25 n’a pas été réalisée par Codex.

1. Exporter toutes les données avant essai ; installer par-dessus M7C, sans désinstallation/effacement.
2. Pour une destination ancienne, visiter B, configurer une réception cuivre avec eau et sortie, puis quitter en sauvegardant. Charger A, expédier un lot fini, puis rester dans C.
3. Laisser C jouer **au moins un tour économique non pausé**, ensuite ouvrir le diagnostic. Chercher **« Réservés hors écran : 100 »** ; « Reçus confirmés » peut encore être 0. Fermer les écrans pour faire avancer le temps.
4. Tester une arrivée commencée dans B puis quitter avant sa fin : après un tour depuis C, elle doit devenir réservée si son relevé est apparié et l’eau restante suffisante. Après mise à jour d’une ancienne partie M7C, une visite/sauvegarde initiale peut être nécessaire.
5. Visiter B : un seul lot physique, consommation totale d’eau correcte, puis confirmation après sauvegarde. Tester saturation et eau insuffisante : le lot doit rester conservé avec motif explicite.
6. Signaler l’état du journal/diagnostic si une attente persiste. Ne pas restaurer séparément un secteur ou journal ni supprimer de fichiers. Vérifier séparément classique et libre.

## Verdict

**Prête pour les essais Android de M7C.1, pas de fusion automatique.** Le blocage d’une arrivée finie sauvegardée est reproduit puis corrigé, avec conservation des quantités et de l’eau dans les cas testés. Le diagnostic ne doit plus faire passer une réservation virtuelle pour une absence totale de progression. En revanche, un noyau ou une usine inactive n’est toujours pas alimenté physiquement avant la visite : ce comportement volontaire ne fait pas partie du correctif.

La [PR #2](https://github.com/Ankool29/Mindustry-Industrial/pull/2) reste non fusionnée. Arrêt après publication de M7C.1 ; essais S25 et validation utilisateur attendus. M7D non commencée.
