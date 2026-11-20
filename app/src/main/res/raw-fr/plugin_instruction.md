3-Stack Archive ajoute la gestion des archives au gestionnaire de fichiers d'AutoJs6: parcourez les archives ZIP, 7Z, RAR et de la famille TAR comme des dossiers, extrayez-les en tout ou en partie et compressez des fichiers et des dossiers en archives. L'écran d'accueil du plugin peut aussi ouvrir des archives pour les parcourir et les extraire de façon autonome.

### Prise en main

1. Activez 3-Stack Archive dans "Centre des plugins".
2. Dans le gestionnaire de fichiers d'AutoJs6, ouvrez le menu d'une archive et choisissez "Voir l'archive"; son contenu s'affiche dans la même liste de fichiers.
3. Ouvrez des dossiers, lancez une recherche ou touchez une entrée pour l'ouvrir dans une visionneuse d'AutoJs6; la touche Retour remonte d'abord d'un niveau dans l'archive.
4. Pour récupérer des fichiers, choisissez "Extraire vers..." dans le menu de l'archive pour tout extraire, ou appuyez longuement sur des entrées pour n'extraire que la sélection.

### Fonctionnalités principales

- **Parcourir et prévisualiser**: Parcourez les archives ZIP, JAR, 7Z, RAR et de la famille TAR dans la liste de fichiers d'AutoJs6, avec recherche et barre de chemin; documents, images, audio et vidéo s'ouvrent dans une visionneuse sans extraction préalable.
- **Ouvrir comme archive**: Si le nom d'un fichier n'est pas reconnu, choisissez "Ouvrir comme archive..." dans le menu du fichier; le format est détecté d'après le contenu et indiqué dans la barre de chemin.
- **Extraction**: Extrayez toute l'archive, le dossier courant (bouton à droite de la barre de chemin) ou les entrées sélectionnées, avec progression et annulation; les fichiers de même nom peuvent être ignorés, remplacés ou numérotés automatiquement.
- **Mots de passe et encodage des noms**: Les archives ZIP, 7Z et RAR chiffrées demandent un mot de passe à l'ouverture ou à l'extraction, et un mot de passe erroné peut être ressaisi aussitôt; les noms de fichiers ZIP illisibles se corrigent en changeant d'encodage dans la barre de chemin.
- **Archives en plusieurs volumes**: Les ensembles complets `.z01` + `.zip`, `partN.rar`, `.zip.001` et `.7z.001` peuvent être parcourus, prévisualisés et extraits, mais pas modifiés.
- **Compression**: Choisissez "Compresser..." pour un fichier, un dossier ou plusieurs éléments sélectionnés dans un même dossier afin de créer des archives ZIP, 7Z, TAR, TAR.GZ, etc.; ZIP et 7Z acceptent un mot de passe et ZIP peut être découpé par taille.
- **Une archive par élément**: Avec plusieurs éléments sélectionnés, activez "Compresser chaque élément séparément" pour créer une archive par élément; les conflits de noms sont numérotés automatiquement.
- **Modifier une archive**: Les archives ZIP ordinaires à volume unique, 7Z non chiffrées et non solid, et de la famille TAR acceptent l'ajout de fichiers et de dossiers, la création de dossiers vides, le renommage et la suppression d'entrées; après une modification, "Restaurer la version précédente" l'annule une fois.
- **Sources vers la corbeille**: Avec "Déplacer les éléments sources vers la corbeille après compression" activé (désactivé par défaut), les sources ne vont dans la corbeille d'AutoJs6 qu'après vérification de toutes les sorties; "Historique de récupération des sources", dans le menu de la page de compression, les restaure.
- **Écran d'accueil**: Ouvrez le plugin depuis le lanceur et touchez "Ouvrir une archive" pour parcourir et extraire des archives; "Paramètres" règle la langue, le mode sombre, la couleur du thème et l'icône du lanceur.

### Utilisation dans les scripts

Les outils de plugin `archive_list`, `archive_read` et `archive_extract` listent le contenu d'une archive, lisent un fichier texte qu'elle contient et l'extraient depuis un script:

```js
let list = plugins.tools.call('archive_list', { archive: './demo.zip' });
list.result.entries.forEach(e => console.log(e.path, e.size));

let readme = plugins.tools.call('archive_read', { archive: './demo.zip', path: 'README.md' });
console.log(readme.result.text);

let extracted = plugins.tools.call('archive_extract', { archive: './demo.zip' }, {
    outputs: { files: './out/demo' },
});
console.log(extracted.result.filesExtracted, extracted.result.kept);
```

- Les chemins relatifs partent du répertoire de travail d'AutoJs6; le dossier de destination d'une extraction peut déjà exister, ses fichiers existants restent intacts et sont comptés dans `kept`.
- `archive_read` renvoie du texte UTF-8, 64 Kio au plus par appel; un fichier binaire ne renvoie pas de texte et `binary` vaut `true`.
- Transmettez le mot de passe d'une archive chiffrée dans `password`; les outils de plugin ne prennent pas en charge les archives en plusieurs volumes.

### Prérequis

- Android 7.0 ou version ultérieure.
- AutoJs6 6.8.0 ou version ultérieure, avec ce plugin activé dans "Centre des plugins"; l'écran d'accueil du plugin fonctionne aussi sans AutoJs6.
- Le plugin ne demande aucune autorisation de stockage ni de réseau et ne traite que les fichiers que vous choisissez dans AutoJs6 ou dans le sélecteur de fichiers du système.

### Questions fréquentes

**Cas où les actions du plugin manquent dans le menu**

Vérifiez que le plugin est activé dans "Centre des plugins". Certaines versions d'AutoJs6 regroupent les actions des plugins dans un sous-menu au nom du plugin, "3-Stack Archive"; pour une archive dont le nom n'est pas reconnu, utilisez "Ouvrir comme archive...".

**Raisons pour lesquelles une archive est en lecture seule**

Les archives RAR, en plusieurs volumes, JAR/AAR/WAR ainsi que les 7Z chiffrées ou solid peuvent seulement être parcourues et extraites. "Gérer l'archive..." indique le format de l'archive courante et la raison de la lecture seule.

**Noms de fichiers dans les archives chiffrées**

Le chiffrement ZIP et 7Z protège le contenu des fichiers, mais leurs noms restent visibles dans l'archive; évitez donc d'y mettre des informations sensibles.

**Cas où une archive en plusieurs volumes ne s'ouvre pas**

Ouvrez un ZIP découpé standard depuis son `.zip` final, un ensemble WinRAR depuis son premier `partN.rar` et un ensemble numéroté depuis son fichier `.001`; tous les volumes doivent se trouver dans le même dossier.

Tous les paramètres et résultats des outils de plugin sont décrits dans [Documentation AutoJs6 > Plugins](https://docs.autojs6.com/#/plugins); plus d'informations sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive).
