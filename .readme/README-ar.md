<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>تصفح أرشيفات عائلة ZIP للقراءة فقط واستخراج SAF انتقائي لمستكشف AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات (Languages)

******

يدعم ملف README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

******

### مقدمة

******

يضيف ملحق AutoJs6 Archive Browser تصفح الأرشيف للقراءة فقط إلى مستكشف AutoJs6. يفتح الحاويات المبنية على ZIP في عارض هرمي مخصص, ويستخرج فقط العناصر التي يحددها المستخدم إلى مجلد إخراج يختاره عبر Android Storage Access Framework.

******

### الميزات

******

- يسجل إجراء قائمة إضافية لملف واحد للقراءة فقط عبر بروتوكول `org.autojs.plugin.EXPLORER_ACTION` المشترك.
- يتصفح مجلدات الأرشيف ويعرض الحجم بعد فك الضغط, والحجم المضغوط, وCRC, وبيانات وقت التعديل.
- يبحث في مسارات العناصر بعد توحيدها ويدعم تحديد ملفات أو مجلدات منفردة أو كل العناصر الظاهرة.
- يستخرج العناصر المحددة إلى شجرة SAF يختارها المستخدم مع عرض التقدم ودعم الإلغاء.
- يقبل حاويات ZIP وJAR وAAR وWAR التي تستخدم طرق ضغط ZIP المدعومة.
- يجهز الإدخال للقراءة فقط في ذاكرة التخزين المؤقت الخاصة ويحذف البيانات المؤقتة عند إغلاق العارض.

******

### التنسيقات المدعومة

******

يتعرف الإصدار 1 على امتدادات الملفات التالية من عائلة ZIP:

```text
zip, jar, aar, war
```

******

### واجهة الملحق

******

يكتشف AutoJs6 الملحق وينفذه باستخدام الهويات التالية:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

يقتصر الإصدار 1 على إجراء قائمة إضافية لملف واحد للقراءة فقط في مستكشف AutoJs6 الرئيسي.

******

### الأمان

******

لا يطلب الملحق أذونات التخزين أو الشبكة. يمنح المضيف وصولا مؤقتا للقراءة فقط إلى content URI الخاص بالإدخال, بينما يقتصر وصول الإخراج على دليل SAF الذي يختاره المستخدم بوضوح. ترفض المسارات المطلقة, واجتياز المجلد الأصل, وبادئات محركات الأقراص, والشرطات المائلة العكسية, وUnicode غير الآمن, والمسارات المكررة, وتعارضات الملفات والمجلدات, وطرق الضغط غير المدعومة, وعدم تطابق الحجم, وعدم تطابق CRC.

******

### حدود الأمان

******

- الحد الأقصى لحجم الإدخال المجهز: `4 GiB`.
- الحد الأقصى لعقد مسارات الأرشيف, بما في ذلك المجلدات الضمنية: `20,000`.
- الحد الأقصى لحجم دليل ZIP المركزي: `64 MiB`.
- الحد الأقصى لطول المسار الموحد: `1,024` حرفا.
- الحد الأقصى لعمق المسار: `64` مقطعا.
- الحد الأقصى للحجم بعد فك الضغط لعنصر واحد: `512 MiB`.
- الحد الأقصى لإجمالي الحجم بعد فك الضغط: `2 GiB`.
- الحد الأقصى لنسبة الضغط: `1000:1`.

******

### سجل الإصدارات

******

# v1.0.0

###### 2026/08/02

* `ميزة` ملحق Archive Browser بمعرف `archive-browser`, ومحرك `explorer-action`, ومتغير `default`
* `ميزة` إجراء مستكشف للقراءة فقط لملف ZIP أو JAR أو AAR أو WAR واحد مع تصفح هرمي, وبحث في المسارات, وتحديد العناصر
* `ميزة` استخراج انتقائي إلى شجرة SAF يختارها المستخدم مع التقدم والإلغاء, ووصول مؤقت للقراءة فقط إلى الإدخال, ومن دون إذن التخزين أو الشبكة
* `ميزة` حدود أمان هي إدخال 4 GiB, و20,000 عقدة مسار بما فيها المجلدات الضمنية, ودليل ZIP مركزي بحجم 64 MiB, و512 MiB لعنصر واحد بعد فك الضغط, و2 GiB لإجمالي البيانات بعد فك الضغط, ونسبة ضغط 1000:1
* `ميزة` التحقق من المسارات غير الآمنة, والعناصر المكررة أو المتعارضة, وطرق الضغط غير المدعومة, وتغير المصدر, وعدم تطابق الحجم, وعدم تطابق CRC
* `ميزة` موارد محلية لبيانات الملحق والواجهة وتعليمات الاستخدام وREADME وCHANGELOG بالإسبانية والفرنسية والروسية والعربية واليابانية والكورية والإنجليزية والصينية المبسطة والصينية التقليدية لهونغ كونغ والصينية التقليدية لتايوان

##### لمزيد من سجل الإصدارات

* [CHANGELOG-ar.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

```powershell
.\gradlew.bat :app:assembleDebug
```

بناء Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

تأتي معلمات البناء من `version.properties`. الحد الأدنى الحالي لإصدار SDK هو 24 وإصدار SDK المستهدف هو 36.

******

### بنية الموارد

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/values-*/plurals.xml
app/src/main/res/raw-*/plugin_instruction.md
```

يوفر `strings.xml` ترجمة بيانات الملحق ونصوص المتصفح الثابتة, بينما يوفر `plurals.xml` ترجمة نصوص المتصفح المرتبطة بالكميات. يوفر `plugin_instruction.md` تعليمات الاستخدام الظاهرة للمضيف. يولد `.python/generate_markdown.py` ملفات README وسجل التغييرات من مصادر JSON.

******

### الروابط

******

- وثائق AutoJs6: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
