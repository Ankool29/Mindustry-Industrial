# Compiler et installer depuis un smartphone Android

Cette branche dérive de Mindustry **v160.7**, commit amont `17012d61d9e4c3cbd9820d87e81c24495486ac53`. Elle prépare uniquement la compilation Android dans GitHub Actions : gameplay original et systèmes de combat conservés. Elle n’est pas une version officielle de Mindustry.

## Lancer une compilation sans ordinateur

1. Ouvrir le dépôt GitHub dans Chrome ou Samsung Internet et se connecter à GitHub.
2. Choisir **Actions**, puis **Android cloud APK**. Si nécessaire, utiliser le menu du navigateur **Version pour ordinateur**.
3. Choisir **Run workflow**, sélectionner la branche `industrial-android`, puis confirmer **Run workflow**.
4. Attendre la coche verte du workflow. Un changement poussé sur `industrial-android` lance aussi la compilation automatiquement.
5. Ouvrir l’exécution terminée et descendre à **Artifacts**.
6. Télécharger **Mindustry-Industrial-Android**. GitHub demande normalement d’être connecté pour télécharger un artefact. L’autre artefact, **Android-build-evidence**, contient les logs et les preuves techniques.

Aucune compilation ni application de développement n’est nécessaire sur le téléphone. Les artefacts sont conservés 30 jours ; après expiration, relancer le workflow. Les limites et éventuels quotas de GitHub Actions restent ceux du compte GitHub.

## Installer sur le Galaxy S25 Ultra

1. Dans **Mes fichiers → Téléchargements**, ouvrir le ZIP téléchargé puis choisir **Extraire**.
2. Ouvrir `Mindustry-Industrial-v160.7-test.apk`.
3. Si Android le demande, autoriser temporairement **Installation d’applis inconnues** pour l’application qui ouvre le fichier, par exemple **Mes fichiers**, puis revenir à l’APK et toucher **Installer**.
4. Sur certaines versions One UI, **Paramètres → Sécurité et confidentialité → Bloqueur automatique** peut empêcher cette installation. Si c’est le cas, le désactiver uniquement le temps d’installer cet APK de confiance, puis le réactiver. Les libellés peuvent varier selon la version de One UI.
5. Lancer **Mindustry Industrial (test)** et vérifier le menu, puis une partie classique avec construction, convoyeurs et combat. Révoquer ensuite l’autorisation d’installation d’applications inconnues si elle avait été activée.

L’identifiant d’application de test est distinct de celui de Mindustry officiel : les deux applications peuvent coexister. Ne pas désinstaller l’original. Les sauvegardes de l’original ne sont pas migrées automatiquement ; utiliser ses fonctions d’export/import si nécessaire.

## Signature et mises à jour de test

L’APK est signé avec la clé Android de **debug**, ce qui permet son installation. Ce n’est pas une clé de publication. Le workflow conserve cette clé dans le cache GitHub pour faciliter les mises à jour de test. Le cache peut expirer ou être supprimé ; la signature peut alors changer. Son empreinte est enregistrée dans `apk-signature.txt`.

Si Android refuse une mise à jour pour signature incompatible, exporter les sauvegardes depuis l’application de test avant de désinstaller **uniquement cette application de test**, puis installer le nouvel APK. Une clé stable stockée dans GitHub Secrets sera nécessaire avant toute distribution durable. Ne pas désactiver Play Protect globalement ni forcer l’installation si le fichier ou sa provenance sont douteux.

## Vérification et diagnostic

- Une exécution verte doit avoir compilé `android:assembleDebug`, vérifié la signature, la présence des bibliothèques natives ARM64 et celle des sprites, puis publié l’artefact.
- Le fichier `SHA256SUMS.txt` donne le SHA-256 de l’APK. `BUILD-INFO.txt` indique le commit exact et le lien de l’exécution.
- Une compilation réussie ne prouve pas encore le lancement sur un S25 Ultra. L’essai physique ci-dessus doit être confirmé séparément.
- En cas d’échec, ouvrir l’étape rouge ou télécharger `Android-build-evidence`, puis transmettre le lien de l’exécution à Codex. Aucun mot de passe ni token GitHub ne doit être envoyé dans le chat.

## Versions et reproductibilité

- JDK : **Temurin 17**.
- Android : plateforme **36**, Build Tools **36.0.0**, SDK minimal déclaré **21**.
- Arc : commit `7445105cd2683daf5b9ac7033ebb07265d3e814d`, correspondant au `archash` amont.
- Gradle : **8.14.3**, distribution vérifiée par SHA-256. Android Gradle Plugin : **8.10.1**, compatible avec la plateforme Android 36. Les valeurs réellement utilisées sont archivées par le workflow. L’amont v160.7 déclare Gradle 9.3.1 et AGP 8.2.2 ; un essai réel dans le cloud a atteint R8 puis échoué sur un fichier intermédiaire `base.jar` manquant. Les deux versions d’outillage sont ajustées ; le code du jeu reste inchangé.
- Le build utilise les sources Arc à la révision verrouillée plutôt que sa branche `master`. Les autres dépendances restent celles de Mindustry. L’accès à Google Maven, Maven Central et JitPack reste nécessaire.
- `CI=false` évite la configuration de la clé de publication privée de l’amont. Le job construit une APK debug, pas une release officielle.

Les workflows de publication et maintenance propres au dépôt officiel sont remplacés sur cette branche par le seul workflow Android. Aucun secret ni droit d’écriture sur les dépôts amont n’est requis.

La branche par défaut `master` contient un lanceur manuel qui appelle le workflow de `industrial-android`. Le bouton **Run workflow** fonctionne ainsi sans réglage administratif du dépôt ; un lancement manuel compile toujours la branche de travail v160.7. Les constructions déclenchées par un push compilent le commit exact de ce push.

## Licence et sources

Le fork reste sous **GNU GPL v3**, avec conservation du fichier `LICENSE` et de l’historique amont. Le ZIP d’artefact contient le texte de licence et une archive des sources du commit du fork correspondant à l’APK. Il contient aussi les sources Arc de la révision utilisée et sa licence Apache 2.0. Le workflow et cette procédure font partie des sources ; `BUILD-INFO.txt` référence aussi la révision Arc utilisée. Conserver et respecter les licences propres aux dépendances et assets. Avant diffusion durable au-delà des essais, compléter l’inventaire des notices et la fourniture des sources des dépendances embarquées selon leurs licences.

Les modifications de cette mission concernent uniquement la CI, la compilation et l’identité de l’application de test. Aucun mode industriel n’est encore développé.
