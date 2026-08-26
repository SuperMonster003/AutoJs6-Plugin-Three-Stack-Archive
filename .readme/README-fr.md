<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Plugin du gestionnaire de fichiers AutoJs6 pour parcourir, extraire et créer les archives prises en charge, avec modification transactionnelle des ZIP ordinaires</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### Langues (Languages)

Le README est disponible dans les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### Présentation

Archive Manager intègre la navigation, l'extraction, la création et la gestion des archives au gestionnaire de fichiers AutoJs6. La version actuelle utilise la liste native et la barre de chemin de l'hôte pour parcourir les formats ZIP, 7Z et de la famille TAR, prévisualise les entrées prises en charge, extrait une portée choisie, crée les formats pris en charge depuis un élément ou une sélection de même dossier parent et modifie transactionnellement les ZIP ordinaires en un seul volume depuis la page de gestion.

### Disponible actuellement

- Ouvrir les archives des familles ZIP, 7Z et TAR directement dans la liste native d'AutoJs6, avec le thème, le mode sombre et les couleurs dynamiques de l'hôte.
- Afficher le dossier externe, le nom de l'archive et le dossier interne dans la barre de chemin ; toucher un niveau pour y accéder et utiliser Retour pour remonter avant de quitter l'archive.
- Extraire le dossier interne courant depuis la barre de chemin, ou activer le mode de sélection et extraire les fichiers et dossiers cochés, sans quitter la page native de l'hôte ; la progression est affichée, la tâche peut être annulée et le dossier parent s'actualise à la fin.
- Ouvrir les documents, images, fichiers audio et vidéos pris en charge avec les visionneuses existantes de l'hôte.
- Utiliser « Extraire vers... » pour extraire toute l'archive dans le dossier recommandé à côté de celle-ci, ou choisir un autre dossier avec le sélecteur système Android; les noms de dossiers équivalents déjà présents sont numérotés en toute sécurité.
- Choisir « Gérer l'archive... » pour ouvrir la page de gestion, extraire toute l'archive, le dossier interne actuel ou la sélection cochée, ou modifier un ZIP ordinaire en un seul volume avec « Ajouter des fichiers... », « Ajouter un dossier... », « Nouveau dossier... », « Renommer... » et « Supprimer »; « Extraire vers... » reste le raccourci pour l'archive entière.
- Les modifications ZIP sont planifiées avant l'écriture, reconstruites dans une sortie en attente détenue par l'hôte, entièrement relues, puis remplacent l'original atomiquement après validation. Une annulation ou un échec laisse la source intacte, et une validation réussie actualise Explorer automatiquement.
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
- Reconnaître la structure ZIP fractionnée standard `.z01 + .zip` et indiquer les noms des volumes requis au lieu de déclarer le volume final endommagé ; créer des ZIP fractionnés avec des tailles MiB prédéfinies ou personnalisées, tandis que la lecture de volumes existants attend encore une API hôte donnant accès aux fichiers voisins.
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
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La version actuelle peut créer les formats suivants:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> L'intégration complète utilise Explorer Action v10 pour extraire le dossier courant ou les entrées sélectionnées dans la page native, v9 pour les sorties de dossiers vérifiées et leur récupération, v8 pour le remplacement vérifié de la cible, v7 pour vérifier la sortie avant validation, v6 pour la navigation native et l'aperçu, et les sessions de fichiers v4 pour la compression; elle exige AutoJs6 avec le code de version 5276 ou plus récent. La modification concerne actuellement les fichiers `.zip` ordinaires en un seul volume. La lecture des volumes fractionnés existants, les demandes natives de mot de passe et d'encodage des noms, le chiffrement des noms à la création, la suppression des sources et la modification de JAR/AAR/WAR, 7Z ou de la famille TAR ne sont pas encore publiés. Le Roadmap fait foi.

### Utilisation

1. Installez le plugin et activez-le dans le centre de plugins AutoJs6.
2. Ouvrez le menu d'une archive ZIP, JAR, AAR, WAR, 7Z ou de la famille TAR.
3. Choisissez « Ouvrir l'archive », puis entrez dans les dossiers, recherchez ou naviguez avec la barre de chemin de la liste hôte.
4. Pour extraire toute l'archive, choisissez « Extraire vers... » dans son menu. Utilisez le dossier actuel recommandé ou choisissez-en un autre avec le sélecteur système Android, puis confirmez le chemin de sortie exact.
5. Pour extraire le dossier interne courant, touchez le bouton d'extraction à droite de la barre de chemin. Pour extraire des entrées précises, maintenez une entrée, cochez les fichiers ou dossiers puis touchez « Extraire » dans la barre inférieure. Utilisez « Gérer l'archive... » ou « Extraire vers... » si un mot de passe, une correction d'encodage, une confirmation de chemin dangereux, une règle de conflit ou une autre destination est nécessaire.
6. Pour modifier un ZIP ordinaire en un seul volume, choisissez « Gérer l'archive... » puis utilisez « Ajouter des fichiers... », « Ajouter un dossier... » pour importer toute une arborescence, « Nouveau dossier... » pour créer un dossier vide, « Renommer... » ou « Supprimer ». Attendez la reconstruction, la validation et le message de réussite avant de quitter la page.
7. Avant l'extraction dans la page de gestion, choisissez comment traiter les noms de sortie équivalents. Demander à chaque fois permet d'appliquer une décision d'ignorer, d'écraser ou de renommer automatiquement à tous les conflits compatibles restants.
8. Pour créer une archive, choisissez « Compresser... » dans le menu d'un fichier ou dossier, ou sélectionnez plusieurs éléments du même dossier et utilisez « Compresser... » dans la barre inférieure. Pour créer une archive par élément, activez « Compresser chaque élément séparément », vérifiez l’aperçu des sorties, puis lancez la création ; ce mode résout toujours les conflits par une numérotation automatique sûre. Pour ZIP, choisissez Aucun fractionnement, une valeur MiB courante ou un entier personnalisé de 1 à 4096 MiB ; si la sortie dépasse cette taille, elle comprend les volumes `.z01`, `.z02`, ... puis un `.zip` final, tandis qu'une sortie plus petite reste un seul `.zip`.

### Autorisations et données

Le plugin ne demande aucune autorisation de stockage ni de réseau. La navigation native conserve d'abord le descripteur repositionnable en lecture seule de l'hôte et fournit aux archives ordinaires des canaux à position indépendante. Les tubes, les sources inscriptibles ou non repositionnables, Android 7 et les lecteurs exigeant un fichier local lisible par le processus (actuellement les ZIP chiffrés) utilisent le cache privé. Le descripteur ou le cache est nettoyé à la fermeture, à la déconnexion, en cas d'échec ou après expiration. L'extraction dans le même dossier écrit uniquement par une transaction de dossier appartenant à l'hôte et liée à l'UID du plugin; un autre dossier utilise seulement l'autorisation d'arborescence choisie dans le sélecteur système Android. La création d'archive passe par une session de fichiers liée à l'UID du plugin, lit les cibles par pages et ne peut créer une sortie transactionnelle que dans le dossier parent actuel. Les mots de passe restent uniquement dans des tampons mémoire effaçables, ne sont jamais écrits dans les Bundles, préférences, journaux ou diagnostics, puis sont effacés après remplacement, fin de tâche ou destruction de la page. La limite fixe de 4 Gio et les seuils de consultation ont été retirés ; l'isolation des chemins, le contrôle de taille source, les transactions de sortie et le nettoyage restent actifs.

Les budgets de ressources déterminent seulement quand avertir ou demander une confirmation ; ils ne relâchent jamais la sécurité structurelle. Après confirmation, les limites réelles d'octets et de ratio ne sont étendues que jusqu'aux valeurs déclarées par les entrées sélectionnées pour cette extraction. Toute croissance non déclarée, modification de la source ou incohérence de taille ou de CRC interrompt toujours l'opération et nettoie la sortie.

Lors de la création d'un ZIP fractionné, le plugin assemble et vérifie tous les volumes dans son cache privé, copie chacun vers une sortie masquée de l'hôte et compare chaque volume en attente octet par octet. Il supprime ensuite seulement la copie privée et publie les volumes numérotés avant le `.zip` final, sans jamais écraser un nom existant. Comme l'hôte actuel ne sait pas valider un groupe atomiquement, toute validation partielle est signalée explicitement au lieu d'être présentée comme une archive complète.

Les noms non sûrs ne sont exposés que comme texte d'affichage en lecture seule derrière des identifiants opaques et ne deviennent jamais des chemins de sortie.

### Roadmap

Les tâches et critères pour les formats modifiables au-delà du ZIP ordinaire, la lecture de volumes fractionnés, les demandes natives de mot de passe et d'encodage des noms, le chiffrement des noms à la création, la suppression des sources, l'annulation et le reste de la récupération après échec sont regroupés dans le Roadmap. Une case non cochée n'est pas une fonction actuelle.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### v2.3.0

_2026/08/26_

- `Note` Cette version exige la compilation AutoJs6 6.8.0 associée avec Explorer Action v10 (code de version 5276 ou ultérieur)
- `Ajout` La page native des archives peut maintenant extraire le dossier interne courant depuis la barre de chemin ou les entrées cochées depuis la barre de sélection, sans ouvrir une page de gestion distincte
- `Ajout` L'extraction native écrit dans une arborescence de sortie appartenant à l'hôte, avec progression, annulation, numérotation sûre des conflits et actualisation automatique d'Explorer
- `Correction` Quitter une archive sous Android 7 ne provoque plus de plantage pendant le nettoyage du cache d'aperçu par l'hôte
- `Correction` Les libellés de la barre de sélection à cinq actions sont centrés sous leurs icônes sur les écrans étroits
- `Amélioration` Le mode de sélection d'archive n'affiche plus que Quitter et Extraire, et masque les actions du système de fichiers sans objet dans une archive

#### v2.2.0

_2026/08/26_

- `Note` Cette version exige la version associée d'AutoJs6 6.8.0 avec Explorer Action v9 (code de version 5276 ou plus récent)
- `Ajout` Extraire vers... recommande désormais le dossier actuel et crée un dossier de sortie de même nom par une transaction appartenant à l'hôte; un nom équivalent existant est numéroté en toute sécurité sans modifier son contenu
- `Ajout` La gestion des ZIP ordinaires en un seul volume peut importer un dossier entier avec le sélecteur système Android, y compris les fichiers imbriqués et les dossiers vides
- `Correction` Le sélecteur de destination d'extraction reste entièrement utilisable sur les écrans étroits ou peu hauts et affiche le chemin par défaut exact
- `Correction` Une extraction dans le même dossier annulée, échouée, interrompue ou à court d'espace annule la sortie non publiée; la session suivante récupère les transactions interrompues de l'hôte sans modifier l'archive source
- `Amélioration` Explorer Action v9 publie atomiquement les arborescences vérifiées et actualise le nouveau dossier de sortie dans AutoJs6 immédiatement après validation

#### v2.1.0

_2026/08/25_

- `Note` La modification concerne actuellement les fichiers `.zip` ordinaires en un seul volume. JAR/AAR/WAR, ZIP fractionné, 7Z et famille TAR restent en lecture seule; la reconstruction normalise les commentaires, les métadonnées extra non essentielles et les attributs de permission Unix
- `Ajout` Gérer l'archive... ouvre le ZIP sélectionné dans la page de gestion avec Ajouter des fichiers..., Nouveau dossier..., Renommer... et Supprimer, y compris le renommage et la suppression de sous-arborescences
- `Ajout` Explorer Action v8 reconstruit dans une sortie en attente détenue par l'hôte, relit entièrement le résultat, ne remplace atomiquement l'original qu'après validation et actualise automatiquement la ligne Explorer
- `Amélioration` Chaque modification est prévalidée dans un plan immuable qui contrôle les chemins dangereux, les noms dupliqués ou équivalents, les conflits fichier/dossier, les entrées conservées non prises en charge et les changements de source avant validation de la sortie de remplacement
- `Amélioration` La reconstruction ZIP conserve le contenu Stored/Deflate, les horodatages utilisables et le chiffrement ZipCrypto/AES pris en charge; une annulation ou tout échec de validation abandonne la sortie en attente et laisse l'archive originale intacte

##### Historique complet

* [CHANGELOG-fr.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

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

- Documentation AutoJs6: https://docs.autojs6.com
- Mentions relatives aux logiciels tiers: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
