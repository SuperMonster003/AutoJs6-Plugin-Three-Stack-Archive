<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="3-Stack Archive" width="128" />
  </p>

  <h1>3-Stack Archive</h1>

  <p>Un gestionnaire d'archives intégré à AutoJs6 pour parcourir, extraire, créer et modifier en toute sécurité les formats pris en charge</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive?color=534BAE&label=License"/></a>
  </p>
</div>

### Langues (Languages)

Le README est disponible dans les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/.readme/README-ar.md)

### Commencer

Un écran autonome ouvre les archives pour les consulter et extraire; la création et la modification sur place restent accessibles depuis AutoJs6. Les paramètres communs proposent la langue, le mode sombre, la couleur et quatre icônes de lanceur.

L'identifiant passe de `io.github.supermonster003.autojs6.plugin.archivemanager` à `io.github.supermonster003.autojs6.plugin.three.stack.archive`. Android installe une application distincte; les anciennes applications et leurs données peuvent être conservées, sans migration automatique des paramètres.

******

### Présentation

3-Stack Archive fonctionne dans le gestionnaire de fichiers AutoJs6 au lieu de le remplacer. Les archives prises en charge utilisent la liste, la barre de chemin, le thème, les visionneuses, la sélection, la progression et l'actualisation de l'hôte. Une page de gestion séparée reste réservée aux informations détaillées sur le format et aux réglages qui exigent un formulaire plus complet.

### Captures d'écran

Ces captures Android authentiques montrent l'intégration au menu de l'hôte, la navigation native dans les archives, la création et la gestion détaillée. Seules des données synthétiques publiques apparaissent.

<table>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/images/screenshots/explorer-actions.png?raw=true" alt="Archive actions in AutoJs6 Explorer" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/images/screenshots/native-archive-browsing.png?raw=true" alt="Native archive browsing" width="280" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/images/screenshots/create-archive-form.png?raw=true" alt="Archive creation form" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/images/screenshots/archive-management.png?raw=true" alt="Archive management page" width="280" /></td>
  </tr>
</table>

- Conditions de capture et série complète: [docs/images/screenshots/README.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/images/screenshots/README.md)

### Disponible actuellement

- Ouvrir les archives des familles ZIP, 7Z et TAR directement dans la liste native d'AutoJs6, avec le thème, le mode sombre et les couleurs dynamiques de l'hôte.
- Afficher le dossier externe, le nom de l'archive et le dossier interne dans la barre de chemin ; toucher un niveau pour y accéder et utiliser Retour pour remonter avant de quitter l'archive.
- Extraire le dossier interne courant depuis la barre de chemin, ou activer le mode de sélection et extraire les fichiers et dossiers cochés, sans quitter la page native de l'hôte ; la progression est affichée, la tâche peut être annulée et le dossier parent s'actualise à la fin.
- Ouvrir les documents, images, fichiers audio et vidéos pris en charge avec les visionneuses existantes de l'hôte.
- Utiliser « Extraire vers... » pour extraire toute l'archive dans le dossier recommandé à côté de celle-ci, ou choisir un autre dossier avec le sélecteur système Android; les noms de dossiers équivalents déjà présents sont numérotés en toute sécurité.
- Choisir « Gérer l'archive... » pour ouvrir la page de gestion, extraire toute l'archive, le dossier interne actuel ou la sélection cochée, ou modifier un ZIP ordinaire à volume unique ou un TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 ou TAR.ZST/TZST compatible avec « Ajouter des fichiers... », « Ajouter un dossier... », « Nouveau dossier... », « Renommer... » et « Supprimer »; « Extraire vers... » reste le raccourci pour l'archive entière.
- Les modifications ZIP et TAR sont planifiées avant l'écriture, reconstruites dans une sortie en attente détenue par l'hôte, entièrement relues, puis remplacent l'original atomiquement après validation. Une annulation ou un échec laisse la source intacte, et une validation réussie actualise Explorer automatiquement.
- Pour les noms de sortie équivalents, choisissez Demander à chaque fois, Ignorer, Écraser ou Renommer automatiquement; Appliquer à tout traite les conflits compatibles restants et les dossiers de sortie existants sont toujours numérotés et préservés.
- Parcourir les dossiers, rechercher et trier le contenu de l'archive.
- Afficher la liste à partir des métadonnées sans décompresser chaque entrée au préalable.
- Parcourir les archives ordinaires via le descripteur repositionnable en lecture seule de l'hôte et des canaux à position indépendante, sans copie intégrale ; les tubes, les sources inscriptibles ou non repositionnables, Android 7 et les lecteurs exigeant un fichier local lisible par le processus (actuellement les ZIP chiffrés) utilisent le cache privé, supprimé à la fermeture.
- Choisir un budget d'extraction Compatible, Strict ou Personnalisé ; une archive qui dépasse les seuils d'entrées, de chemin, de taille de sortie ou de taux de compression reste consultable et affiche l'espace estimé et les risques avant une confirmation unique d'écriture.
- Afficher la progression par éléments et octets, l'élément actuel, le débit et le temps restant estimé ; une annulation ou un échec restaure la nouvelle racine de sortie, et tout résidu refusé par le fournisseur est indiqué par nom et URI.
- Placer les noms contenant une remontée vers le dossier parent, un chemin absolu, un préfixe de lecteur ou un caractère de contrôle dans un dossier Chemins non sûrs visible dans la barre de chemin ; les données lisibles restent prévisualisables et l'extraction complète exige d'ignorer explicitement ces entrées sans affecter les autres.
- Parcourir, prévisualiser et extraire les TAR non compressés ; les liens symboliques ou physiques, les nœuds de périphérique et les entrées creuses restent listés sans être écrits comme des fichiers ordinaires.
- Parcourir, prévisualiser et extraire les TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST avec les mêmes chemins internes, l'isolation des entrées spéciales et les contrôles d'intégrité.
- Parcourir, prévisualiser et extraire les 7Z ordinaires ou solid, y compris les chaînes courantes de compression et de filtres ainsi que les entrées chiffrées dans le contenu ou l'en-tête ; un mot de passe absent ou erroné produit un diagnostic explicite.
- Vérifier la structure ZIP/7Z/TAR réelle et unifier les capacités de prévisualisation, d'extraction et de création, en laissant désactivées les options non prises en charge.
- Prendre en charge Zip64, les préambules auto-extractibles, les anciens encodages de noms et les séparateurs Windows.
- Parcourir, prévisualiser et extraire les ensembles `.z01 + .zip` complets via des descripteurs voisins bornés et autorisés par l'hôte ; créer des ZIP fractionnés avec des tailles MiB prédéfinies ou personnalisées et signaler clairement les volumes absents ou modifiés.
- Parcourir et extraire les ZIP protégés par ZipCrypto ou AES, réessayer un mot de passe erroné sur place et créer au choix des ZIP chiffrés en AES-256 dont les noms restent visibles ; la création chiffrée exige une confirmation identique du mot de passe.
- Remplacer l'encodage des noms ZIP quand la détection automatique est incorrecte ; la navigation et l'extraction réutilisent le même choix.
- Afficher le format, l'étape, un code stable et un motif clair en cas d'échec ; les versions de débogage peuvent copier le diagnostic complet.
- Proposer « Compresser... » pour les fichiers, les dossiers et les sélections multiples de même dossier parent.
- Créer une archive par élément d’une sélection partageant le même dossier parent ; le formulaire affiche le nombre de sorties et les noms dérivés, tandis que les noms existants ou répétés sont numérotés sans remplacement. Chaque sortie est validée séparément ; une annulation ou un échec conserve et signale les sorties terminées tout en bloquant une relance ambiguë du lot entier.
- Créer des ZIP ordinaires ou fractionnés standard, ainsi que des 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST, avec un nom configurable et seulement les niveaux et options de mot de passe réellement pris en charge par le format choisi.
- Écrire d'abord dans un fichier temporaire du même dossier puis valider atomiquement ; choisir la numérotation automatique ou essayer le nom exact et demander avant de réessayer avec un numéro, sans écraser les fichiers existants. Après réservation du nom, un instantané source borné est analysé avant l'ouverture de la sortie temporaire ; le formulaire distingue analyse, compression, vérification et validation, avec le total des fichiers, les octets lus et les tailles inconnues. Avant publication, la sortie encore masquée est relue intégralement pour vérifier le format, les entrées, les tailles, les CRC et les empreintes du contenu. Un échec de création ou de vérification annule la transaction ; si l'hôte ne peut pas confirmer le nettoyage, le formulaire affiche le chemin prévu et interdit une nouvelle tentative.

### Formats actuels

La version actuelle reconnaît les extensions consultables et extractibles suivantes:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La version actuelle peut créer les formats suivants:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> L'intégration native exige la build AutoJs6 6.8.0 associée avec Explorer Action v21 (code de version 5276 ou supérieur). RAR et les archives fractionnées restent volontairement en lecture seule. La modification est disponible pour les ZIP ordinaires à volume unique, les 7Z non chiffrés, non solid, à volume unique et dans le budget du décodeur, ainsi que les archives TAR ne contenant que des fichiers et dossiers ordinaires sûrs. Les 7Z chiffrés, solid, fractionnés, dangereux, non pris en charge ou hors budget restent en lecture seule. Ouvrez un ZIP fractionné standard par son `.zip` final, un ensemble WinRAR moderne par son premier `partN.rar`, et un ZIP ou 7Z numéroté par son volume `.001`, avec tous les volumes requis dans le même dossier. Le chiffrement des noms à la création et la modification interne de JAR/AAR/WAR ou RAR restent indisponibles.

### Utilisation

1. Installez le plugin et activez-le dans le centre de plugins AutoJs6.
2. Ouvrez le menu d'une archive ZIP, JAR, AAR, WAR, 7Z ou de la famille TAR.
3. Choisissez « Ouvrir l'archive », puis entrez dans les dossiers, recherchez ou naviguez avec la barre de chemin de la liste hôte.
4. Pour extraire toute l'archive, choisissez « Extraire vers... » dans son menu. Utilisez le dossier actuel recommandé ou choisissez-en un autre avec le sélecteur système Android, puis confirmez le chemin de sortie exact.
5. Pour extraire le dossier interne courant, touchez le bouton d'extraction à droite de la barre de chemin. Pour extraire des entrées précises, maintenez une entrée, cochez les fichiers ou dossiers puis touchez « Extraire » dans la barre inférieure. Utilisez « Gérer l'archive... » ou « Extraire vers... » si un mot de passe, une correction d'encodage, une confirmation de chemin dangereux, une règle de conflit ou une autre destination est nécessaire.
6. Pour modifier un ZIP ordinaire à volume unique ou un TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 ou TAR.ZST/TZST compatible, choisissez « Gérer l'archive... » puis utilisez « Ajouter des fichiers... », « Ajouter un dossier... » pour importer toute une arborescence, « Nouveau dossier... » pour créer un dossier vide, « Renommer... » ou « Supprimer ». Attendez la reconstruction, la validation et le message de réussite avant de quitter la page.
7. Avant l'extraction dans la page de gestion, choisissez comment traiter les noms de sortie équivalents. Demander à chaque fois permet d'appliquer une décision d'ignorer, d'écraser ou de renommer automatiquement à tous les conflits compatibles restants.
8. Pour créer une archive, choisissez « Compresser... » dans le menu d'un fichier ou dossier, ou sélectionnez plusieurs éléments du même dossier et utilisez « Compresser... » dans la barre inférieure. Pour créer une archive par élément, activez « Compresser chaque élément séparément », vérifiez l’aperçu des sorties, puis lancez la création ; ce mode résout toujours les conflits par une numérotation automatique sûre. Pour ZIP, choisissez Aucun fractionnement, une valeur MiB courante ou un entier personnalisé de 1 à 4096 MiB ; si la sortie dépasse cette taille, elle comprend les volumes `.z01`, `.z02`, ... puis un `.zip` final, tandis qu'une sortie plus petite reste un seul `.zip`.

### Autorisations et données

3-Stack Archive ne demande aucune autorisation de stockage ou de réseau. L'hôte fournit des descripteurs en lecture seule de courte durée et des transactions liées à l'UID du plugin; celui-ci ne peut donc pas choisir un chemin arbitraire. Explorer Action v11 transporte le mot de passe uniquement dans une requête synchrone bornée et ne le persiste jamais. Explorer Action v12 ajoute seulement un catalogue borné, lié à la session, de volumes frères approuvés, avec identifiants opaques et revalidation de l'identité. Explorer Action v13 réindexe uniquement la même source préparée et conserve l'ancien état jusqu'à ce qu'un index complet soit prêt. Explorer Action v14 ne reconnaît que des suffixes composés bornés tels que `.zip.001` et `.7z.001`, jamais n'importe quel fichier `.001`, et réutilise le catalogue v12 sans donner accès au dossier ni aux chemins. Explorer Action v17 ajoute uniquement une action secondaire en lecture seule et sans filtre lorsque l'action principale ne correspond pas; elle réutilise une session existante après le choix de l'utilisateur et n'accorde aucun accès supplémentaire au chemin, au dossier ou en écriture. Les chemins dangereux restent isolés, la sortie est vérifiée avant publication et la confirmation d'un budget ne désactive jamais la sécurité structurelle.

Explorer Action v15 regroupe uniquement les nouveaux fichiers vérifiés d'une même session dans un lot récupérable de 128 membres au maximum. Explorer Action v16 permet à l'hôte de revérifier les sources et sorties puis de déplacer les sources vers la corbeille seulement si le plugin fournit la sélection originale complète et ordonnée ainsi que toutes les transactions de sortie validées. L'hôte synchronise une copie de récupération et persiste son entrée avant de retirer les données source; le plugin n'obtient aucun chemin arbitraire ni suppression directe. Une réponse Binder perdue est résolue en consultant le même état terminal idempotent, sans recommencer le déplacement.

Explorer Action v18 conserve l'archive précédente uniquement dans le stockage privé et durable de l'hôte et renvoie un identifiant opaque, jamais un chemin de sauvegarde. Une seule restauration est autorisée tant que le dossier parent et la cible correspondent exactement au remplacement validé. Les changements externes invalident l'historique; les preuves d'une récupération interrompue sont conservées et bloquent un autre remplacement de cette cible jusqu'à leur résolution par l'hôte. L'historique normal est limité par l'âge, le nombre, le total d'octets et la réserve d'espace libre; les sessions v8-v17 ne créent aucune sauvegarde de remplacement. Explorer Action v19 ne transmet que les ID opaques et le nom de feuille sûr nécessaires pour supprimer ou renommer les entrées autorisées. Explorer Action v20 ajoute une autorisation figée limitée aux racines d'entrée explicitement choisies: le plugin reçoit des noeuds opaques bornés, des métadonnées et des descripteurs ponctuels en lecture seule, jamais les chemins source, URI, voisins non choisis ni un accès général au stockage. L'hôte revérifie tout l'instantané avant validation, et toute entrée modifiée ou défaillante annule le remplacement complet de l'archive. Explorer Action v21 conserve des lots de récupération des sources, bornés et détenus par l'hôte, après la recréation de l'Activity ou de la session; restaurer les sources n'écrase aucun nom occupé et ne supprime jamais l'archive créée.

### Roadmap

La consultation, l'extraction, la création, la modification, la restauration de la version précédente et la récupération durable des sources sont terminées sans modifier la disposition ordinaire du gestionnaire de fichiers. Les éléments non cochés du Roadmap sont de futures améliorations facultatives de protocole, de moteur ou de cas limite, et non des fonctions actuelles.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/ROADMAP.md)

### Notes de version

#### v3.0.1

_2026/10/04_

- `Amélioration` Les icônes du centre de plugins utilisent les tailles, positions, images claires et sombres et fonds circulaires réglés dans Icon Studio, avec les sources et paramètres permettant de les reproduire

#### v3.0.0

_2026/10/04_

- `Note` L'identifiant passe de io.github.supermonster003.autojs6.plugin.archivemanager à io.github.supermonster003.autojs6.plugin.three.stack.archive. Android installe une application distincte; les anciennes applications et leurs données peuvent être conservées, sans migration automatique des paramètres
- `Ajout` Un écran autonome ouvre les archives pour les consulter et extraire; la création et la modification sur place restent accessibles depuis AutoJs6
- `Ajout` Les paramètres communs proposent la langue, le mode sombre, la couleur et quatre icônes de lanceur

#### v2.22.3

_2026/09/19_

- `Correction` Avertissements de lecture SDK XML v4 avec AGP 9.1 et contrôles d'alignement natif des APK déclenchés par erreur lors de l'assemblage des tests unitaires JVM, avec les plugins de compilation partagés 1.8.3
- `Amélioration` Après compileSdk, targetSdk passe à 37 (Android 17) ; le comportement du plugin ne dépend pas de la nouvelle cible

##### Historique complet

* [CHANGELOG-fr.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

### Compilation

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilation Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Utilisez le Gradle Wrapper à la racine ; `version.properties` fait foi pour les exigences SDK et JDK.

### Liens

- Guide d'installation: [docs/INSTALLATION.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/INSTALLATION.md)
- Matrice des capacités par format: [docs/FORMAT_CAPABILITIES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/FORMAT_CAPABILITIES.md)
- Politique de sécurité: [SECURITY.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/SECURITY.md)
- Documentation AutoJs6: https://docs.autojs6.com
- Mentions relatives aux logiciels tiers: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/docs/16kb.md)


### Sources et remerciements

[THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/THIRD_PARTY_NOTICES.md) · [RIGHTS_AND_TAKEDOWN.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive/blob/master/RIGHTS_AND_TAKEDOWN.md)
