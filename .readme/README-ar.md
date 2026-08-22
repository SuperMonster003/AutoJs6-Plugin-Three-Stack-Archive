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

يدمج Archive Manager تصفح ZIP واستخراجه وإنشائه في مدير ملفات AutoJs6. يتصفح الإصدار الحالي الأرشيفات في قائمة المضيف الأصلية مع المسارين الخارجي والداخلي، ويعاين العناصر المدعومة، ويضغط عنصرا واحدا أو مجموعة من الأب نفسه. تبقى الصيغ الإضافية والاستخراج حسب العنصر والتحرير الداخلي ضمن Roadmap.

### المتاح حاليا

- فتح أرشيفات عائلة ZIP مباشرة في قائمة ملفات AutoJs6 الأصلية مع سمة المضيف والوضع الداكن والألوان الديناميكية.
- عرض المجلد الخارجي واسم الأرشيف والمجلد الداخلي في شريط المسار؛ الانتقال بالنقر على مستوى واستخدام الرجوع للصعود قبل مغادرة الأرشيف.
- معاينة عناصر المستندات والصور والصوت والفيديو المدعومة باستخدام عارضات المضيف الحالية.
- استخدام اختصار «استخراج إلى...» لاستخراج الأرشيف كاملا من دون فتح شاشة التصفح أولا.
- تصفح مجلدات الأرشيف والبحث فيها وفرزها.
- إنشاء القائمة من بيانات الدليل من دون فك كل عنصر مسبقا.
- التحقق من بنية ZIP الفعلية وتوحيد إمكانات المعاينة والاستخراج والإنشاء، مع إبقاء الخيارات غير المدعومة معطلة.
- دعم Zip64 ومقدمات الاستخراج الذاتي وترميزات الأسماء القديمة وفواصل مسارات Windows.
- تصفح واستخراج ملفات ZIP المحمية بـ ZipCrypto أو AES، وإعادة محاولة كلمة المرور الخاطئة في المكان نفسه، وإنشاء ZIP اختياري بتشفير AES-256 مع بقاء أسماء الملفات ظاهرة؛ ويتطلب الإنشاء المشفر تأكيدًا مطابقًا لكلمة المرور.
- تجاوز ترميز أسماء ZIP يدويا عندما يكون الاكتشاف التلقائي غير صحيح، مع إعادة استخدام الاختيار نفسه في التصفح والاستخراج.
- عرض التنسيق ومرحلة المعالجة والرمز الثابت والسبب الواضح عند الفشل، وإتاحة نسخ التشخيص الكامل في إصدارات التصحيح.
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

> يتطلب التصفح الأصلي ومعاينة العناصر في Explorer Action v6 والضغط في v4 رمز إصدار AutoJs6 رقم 5276 أو أحدث. لم تصدر بعد ميزات الاستخراج حسب العنصر داخل صفحة المضيف الأصلية و7z وأنواع tar والأجزاء وتشفير الأسماء والأرشيفات المنفصلة وحذف المصادر والإضافة أو الحذف داخل الأرشيف. حالة مربعات Roadmap هي المرجع.

### الاستخدام

1. ثبت الملحق وفعله في مركز ملحقات AutoJs6.
2. افتح قائمة ملف ZIP أو JAR أو AAR أو WAR.
3. اختر «فتح الأرشيف»، ثم ادخل المجلدات أو ابحث أو انتقل عبر شريط المسار في قائمة المضيف.
4. لاستخراج الأرشيف كاملا اختر «استخراج إلى...» من قائمة الملف ثم حدد مجلد الإخراج بمنتقي نظام Android.
5. لإنشاء ZIP اختر «ضغط...» من قائمة ملف أو مجلد عادي، أو حدد عدة عناصر في مجلد واحد واستخدم «ضغط...» في الشريط السفلي.

### الأذونات والبيانات

لا يطلب الملحق إذن التخزين أو الشبكة. يستخدم التصفح الأصلي جلسة أرشيف قصيرة للقراءة فقط مرتبطة بمعرف UID للمضيف، ويحذف الإدخال المؤقت عند إغلاق الصفحة أو قطع الاتصال؛ ويستخدم الاستخراج URI المؤقت من المضيف فقط. يستخدم إنشاء ZIP جلسة ملفات مضيف مرتبطة بمعرف UID للملحق، ويقرأ الأهداف على صفحات، ولا ينشئ إخراجا معاملاتيا إلا في المجلد الأب الحالي. تبقى كلمات المرور في مخازن ذاكرة قابلة للمسح فقط، ولا تكتب أبدا في Bundle أو التفضيلات أو السجلات أو التشخيصات، وتمسح عند الاستبدال أو انتهاء المهمة أو إتلاف الصفحة. أزيل حد إدخال 4 GiB الثابت وحدود التصفح، مع بقاء عزل المسار وفحوص السلامة والتنظيف عند الفشل.

### Roadmap

توجد مهام ومعايير الصيغ الإضافية والأجزاء والاستخراج حسب العنصر وتحرير الأرشيف ومصفوفة الأجهزة الكاملة في Roadmap. البند غير المحدد ليس ميزة حالية.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### ملاحظات الإصدار

#### Unreleased

_غير منشور_

- `إضافة` توحيد اسم المنتج إلى Archive Manager وإجراء الملف إلى فتح الأرشيف
- `إضافة` يتيح Explorer Action v5 تصفح الأرشيفات في قائمة AutoJs6 الأصلية مع شريط المسار والسمة والتنقل للخلف
- `إضافة` يتيح Explorer Action v6 فتح عناصر المستندات والصور والصوت والفيديو المدعومة باستخدام عارضات المضيف
- `إضافة` اختصار «استخراج إلى...» لاختيار الوجهة واستخراج الأرشيف كاملا
- `إضافة` يضيف Explorer Action v4 إجراء «ضغط...» إلى قوائم الملفات والمجلدات وإلى شريط الإجراءات الخمسة للتحديدات ذات المجلد الأب نفسه
- `إضافة` إنشاء ZIP باسم افتراضي ومستويات ضغط وتقدم وإلغاء وترقيم تلقائي لتعارض الأسماء
- `إضافة` تصفح واستخراج ZIP المشفر بـ ZipCrypto/AES، وإعادة إدخال كلمة المرور الخاطئة في المكان نفسه، وإنشاء ZIP اختياري بتشفير AES-256 مع بقاء أسماء الملفات ظاهرة وتأكيد مطابق لكلمة المرور
- `إصلاح` تعتمد قائمة ZIP على بيانات الدليل وتدعم مقدمات الاستخراج الذاتي والترميزات القديمة وفواصل Windows وطرقا إضافية قابلة للقراءة
- `إصلاح` يمكن تجاوز ترميز أسماء ZIP يدويا عند خطأ الاكتشاف التلقائي ويعيد الاستخراج استخدام الاختيار نفسه
- `إصلاح` لم تعد الأحجام المجهولة وURI الصالحة وأذونات الكتابة الإضافية من المضيف ترفض الأرشيف الصحيح
- `إصلاح` إصلاح تصفح ZIP واستخراجه في Android 7.x بعد تجنب واجهات لا تتوفر إلا في الأنظمة الأحدث
- `إصلاح` تصنف كلمات المرور الخاطئة الآن بثبات على أنها PASSWORD/WRONG_PASSWORD، ولم تعد عناصر AES v2 ذات CRC المخزن بقيمة صفر تبلغ خطأ على أنها تالفة
- `تحسين` إزالة حد 4 GiB الثابت وحدود الحجم أو نسبة الضغط أثناء التصفح مع إبقاء العزل والتحقق من السلامة
- `تحسين` إضافة Roadmap قابل للتتبع وإعادة كتابة README وCHANGELOG
- `تحسين` الشاشة المستقلة تتبع الآن وضع النظام والألوان الديناميكية Material
- `تحسين` يمر إخراج ZIP عبر جلسة مضيف مرتبطة بمعرف UID إلى ملف مؤقت في المجلد نفسه ثم يعتمد ذرياً دون إذن تخزين أو استبدال ملفات موجودة
- `تحسين` تُفحص إمكانات التنسيق والعناصر بشكل موحد في المعاينة والاستخراج والإنشاء لتبقى الخيارات غير المتاحة معطلة
- `تحسين` توضح حالات الفشل التنسيق ومرحلة المعالجة والرمز الثابت والسبب، وتتيح إصدارات التصحيح نسخ التشخيص الكامل
- `اعتماد` إضافة Zip4j 2.11.5 المرخصة بموجب Apache License 2.0 لتدفقات ZIP المشفرة وإنشاء AES-256 ومسار التوافق مع Android 7.x

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
- إشعارات البرامج الخارجية: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
