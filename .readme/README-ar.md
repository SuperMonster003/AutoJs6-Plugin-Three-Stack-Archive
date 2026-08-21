<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>ملحق لمدير ملفات AutoJs6 لفتح أرشيفات ZIP واستخراجها وإنشائها</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### اللغات (Languages)

يتوفر README باللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

### حول المشروع

يدمج Archive Manager تصفح ZIP واستخراجه وإنشائه في مدير ملفات AutoJs6. يستطيع الإصدار الحالي ضغط عنصر واحد أو مجموعة من الأب نفسه، ويكتب النتيجة من خلال جلسة ملفات يتحكم فيها المضيف. تبقى الصيغ الإضافية وصفحة الأرشيف الأصلية في المضيف والتحرير الداخلي ضمن Roadmap.

### المتاح حاليا

- فتح أرشيفات عائلة ZIP من قائمة ملفات AutoJs6.
- استخدام اختصار «استخراج إلى...» لاستخراج الأرشيف كاملا من دون فتح شاشة التصفح أولا.
- تصفح المجلدات والبحث في المسارات وتحديد الملفات أو المجلدات.
- إنشاء القائمة من بيانات الدليل من دون فك كل عنصر مسبقا.
- دعم Zip64 ومقدمات الاستخراج الذاتي وترميزات الأسماء القديمة وفواصل مسارات Windows.
- إبقاء الأرشيف قابلا للتصفح إذا تعذر استخراج عنصر منفرد.
- استخراج المحدد إلى مجلد عبر منتقي Android مع التقدم والإلغاء.
- إتاحة «ضغط...» للملفات والمجلدات والتحديدات المتعددة التي تشترك في المجلد الأب.
- إنشاء ZIP باسم ومستوى ضغط قابلين للضبط؛ استخدام اسم الهدف لعنصر واحد واسم المجلد الأب لعدة عناصر افتراضيا.
- الكتابة أولا إلى ملف مؤقت في المجلد نفسه ثم الاعتماد بشكل ذري؛ ترقيم تعارضات الأسماء دون استبدال الملفات الموجودة.

### الصيغ الحالية

يتعرف الإصدار الحالي على امتدادات عائلة ZIP التالية:

```text
zip, jar, aar, war
```

يستطيع الإصدار الحالي إنشاء الصيغ التالية:

```text
zip
```

> يتطلب تكامل Explorer Action v4 رمز إصدار AutoJs6 رقم 5276 أو أحدث. لم تصدر بعد ميزات 7z وأنواع tar وكلمات المرور والأجزاء وتشفير الأسماء والأرشيفات المنفصلة وحذف المصادر والإضافة أو الحذف داخل الأرشيف. حالة مربعات Roadmap هي المرجع.

### الاستخدام

1. ثبت الملحق وفعله في مركز ملحقات AutoJs6.
2. افتح قائمة ملف ZIP أو JAR أو AAR أو WAR.
3. اختر «فتح الأرشيف»، ثم تصفح أو ابحث وحدد المحتوى المطلوب.
4. اختر «استخراج المحدد» وحدد مجلد الإخراج؛ وللأرشيف كاملا اختر «استخراج إلى...» مباشرة من قائمة الملف.
5. لإنشاء ZIP اختر «ضغط...» من قائمة ملف أو مجلد عادي، أو حدد عدة عناصر في مجلد واحد واستخدم «ضغط...» في الشريط السفلي.

### الأذونات والبيانات

لا يطلب الملحق إذن التخزين أو الشبكة. يستخدم التصفح والاستخراج URI المؤقت من المضيف فقط. يستخدم إنشاء ZIP جلسة مضيف قصيرة مرتبطة بمعرف UID للملحق، ويقرأ الأهداف على صفحات، ولا ينشئ إخراجا معاملاتيا إلا في المجلد الأب الحالي. أزيل حد إدخال 4 GiB الثابت وحدود التصفح، مع بقاء عزل المسار وفحوص السلامة والتنظيف عند الفشل.

### Roadmap

توجد مهام ومعايير الصيغ الإضافية وكلمات المرور والأجزاء والتحرير وصفحة أرشيف أصلية في المضيف وشريط المسار الداخلي ومصفوفة الأجهزة الكاملة في Roadmap. البند غير المحدد ليس ميزة حالية.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### ملاحظات الإصدار

#### Unreleased

_غير منشور_

- `إضافة` توحيد اسم المنتج إلى Archive Manager وإجراء الملف إلى فتح الأرشيف
- `إضافة` اختصار «استخراج إلى...» لاختيار الوجهة واستخراج الأرشيف كاملا
- `إضافة` يضيف Explorer Action v4 إجراء «ضغط...» إلى قوائم الملفات والمجلدات وإلى شريط الإجراءات الخمسة للتحديدات ذات المجلد الأب نفسه
- `إضافة` إنشاء ZIP باسم افتراضي ومستويات ضغط وتقدم وإلغاء وترقيم تلقائي لتعارض الأسماء
- `إصلاح` تعتمد قائمة ZIP على بيانات الدليل وتدعم مقدمات الاستخراج الذاتي والترميزات القديمة وفواصل Windows وطرقا إضافية قابلة للقراءة
- `إصلاح` لم تعد الأحجام المجهولة وURI الصالحة وأذونات الكتابة الإضافية من المضيف ترفض الأرشيف الصحيح
- `إصلاح` إصلاح تصفح ZIP واستخراجه في Android 7.x بعد تجنب واجهات لا تتوفر إلا في الأنظمة الأحدث
- `تحسين` إزالة حد 4 GiB الثابت وحدود الحجم أو نسبة الضغط أثناء التصفح مع إبقاء العزل والتحقق من السلامة
- `تحسين` إضافة Roadmap قابل للتتبع وإعادة كتابة README وCHANGELOG
- `تحسين` الشاشة المستقلة تتبع الآن وضع النظام والألوان الديناميكية Material
- `تحسين` يمر إخراج ZIP عبر جلسة مضيف مرتبطة بمعرف UID إلى ملف مؤقت في المجلد نفسه ثم يعتمد ذرياً دون إذن تخزين أو استبدال ملفات موجودة

#### v1.0.1

_2026/08/08_

- `إصلاح` ربط خدمة فارغ عند تفعيل الملحق
- `تحسين` تبسيط الاسم والوصف وتعليمات الاستخدام

#### v1.0.0

_2026/08/02_

- `إضافة` الإصدار الأول لتصفح ZIP وJAR وAAR وWAR واستخراج المحتوى المحدد
- `إضافة` البحث والتحديد والتقدم والإلغاء وتنظيف البيانات المؤقتة وواجهة متعددة اللغات

##### السجل الكامل

* [CHANGELOG-ar.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

### البناء

```powershell
.\gradlew.bat :app:assembleDebug
```

بناء Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

استخدم Gradle Wrapper من جذر المستودع، واعتبر `version.properties` مرجع متطلبات SDK و JDK.

### الروابط

- توثيق AutoJs6: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
