<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>مدير أرشيفات مدمج في AutoJs6 لتصفح الصيغ المدعومة واستخراجها وإنشائها وتحريرها بأمان</p>

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

يعمل Archive Manager داخل مدير ملفات AutoJs6 بدلا من استبداله. تستخدم الأرشيفات المدعومة قائمة المضيف وشريط المسار والسمة والمعاينات والتحديد والتقدم وتحديث المجلدات. تبقى صفحة الإدارة المنفصلة للإعدادات والعمليات التي تحتاج إلى نموذج أوسع فقط.

### المتاح حاليا

- تصفح ZIP/JAR/AAR/WAR و7Z وRAR4/RAR5 وعائلة TAR في قائمة AutoJs6 الأصلية مع المسار الداخلي والبحث والفرز والتنقل بزر الرجوع.
- معاينة المستندات والصور والصوت والفيديو القابلة للقراءة بواسطة معاينات المضيف الحالية دون استخراج الأرشيف كاملا أولا.
- استخراج الأرشيف كاملا أو المجلد الداخلي الحالي أو عناصر محددة مع التقدم والإلغاء وأسماء تعارض آمنة والتحقق والتراجع قبل النشر.
- فتح واستخراج ZIP و7Z وRAR المشفرة مع طلب كلمة مرور أصلي من المضيف، ويمكن تصحيح كلمة المرور الخاطئة دون فقدان المسار الحالي.
- إنشاء ZIP و7Z وTAR وTAR.GZ وTAR.XZ وTAR.BZ2 وTAR.ZST من عنصر واحد أو تحديد له نفس المجلد الأب، كما يدعم ZIP تشفير AES-256 والتقسيم القياسي وأرشيفا مستقلا لكل عنصر.
- تحرير ZIP عادي أحادي الجزء بإعادة بناء متحققة: إضافة ملفات أو شجرة مجلدات وإنشاء مجلد فارغ وإعادة التسمية والحذف، ثم استبدال المصدر ذريّا بعد القراءة الكاملة فقط.
- عزل الأسماء الخطرة للقراءة فقط، وتطبيق حدود البنية والموارد قبل الكتابة، والقراءة مباشرة من واصف المضيف القابل للبحث متى أمكن.

### الصيغ الحالية

يتعرف الإصدار الحالي على الامتدادات التالية القابلة للتصفح والاستخراج:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

يستطيع الإصدار الحالي إنشاء الصيغ التالية:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> يتطلب التكامل الأصلي AutoJs6 6.8.0 برمز إصدار 5276 أو أحدث مع Explorer Action v11. صيغة RAR للقراءة فقط عن قصد. يقتصر التحرير على ملفات `.zip` العادية أحادية الجزء. لا يمكن بعد قراءة مجموعات ZIP/RAR المقسمة كمجموعة كاملة لأن المضيف يمنح واصف الملف المحدد فقط؛ قد يعرض الجزء الأول من RAR البيانات الوصفية لكن الاستخراج يبقى معطلا. لا يتوفر حاليا تشفير الأسماء عند الإنشاء أو حذف المصادر بعد الضغط أو تحرير محتوى 7Z وRAR وعائلة TAR.

### الاستخدام

1. ثبت Archive Manager وفعله من مركز إضافات AutoJs6.
2. اضغط الإجراء الرئيسي لملف مدعوم أو اختر فتح الأرشيف، ثم تصفحه كمجلد عادي باستخدام شريط مسار المضيف.
3. استخدم إجراء شريط المسار لاستخراج المجلد الداخلي الحالي، أو اضغط مطولا لاستخراج تحديد، أو اختر استخراج إلى... من قائمة الملف للأرشيف كاملا. تظهر مطالبة كلمة المرور عند الحاجة.
4. اختر ضغط... لملف أو مجلد، أو حدد عدة عناصر في المجلد نفسه واستخدم الإجراء في الشريط السفلي.
5. اختر إدارة الأرشيف... فقط عند إضافة محتوى أو إعادة تسميته أو حذفه داخل ZIP عادي أحادي الجزء.

### الأذونات والبيانات

لا يطلب Archive Manager إذن التخزين أو الشبكة. يقدم المضيف واصف قراءة قصير العمر ومعاملات إخراج مثبتة على UID الإضافة، لذلك لا تستطيع الإضافة اختيار مسارات عشوائية. ينقل Explorer Action v11 كلمة المرور في طلب إعادة محاولة متزامن ومحدود فقط؛ يزيل المضيف والإضافة المخازن المحتفظ بها ويمسحانها فورا، ولا يحفظانها في الحالة أو التفضيلات أو السجلات أو التشخيصات. قد تنشئ Android ومكتبات Java نسخا وقتية قصيرة لا يمكن تجنبها، لذلك فهذا تنظيف للذاكرة بأفضل جهد وليس ضمانا مطلقا. تبقى المسارات الخطرة معزولة، ويتحقق من الإخراج قبل النشر، ولا يعطل تأكيد ميزانية الموارد فحوص السلامة البنيوية.

### Roadmap

تتابع الأعمال المتبقية بعناصر قابلة للتأشير: إدخال الأجزاء المجاورة لمجموعات ZIP/RAR المقسمة، وتصحيح ترميز الأسماء داخل المضيف، وإعادة البناء القابلة للكتابة لما بعد ZIP، ومعاملات التراجع أو حذف المصدر، ومراجعة إمكانية الوصول، وبقية مصفوفة الأجهزة وأدوات الإنتاج.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### ملاحظات الإصدار

#### v2.4.0

_2026/08/26_

- `ملاحظة` يتطلب هذا الإصدار AutoJs6 6.8.0 مع Explorer Action v11 ورمز إصدار 5276 أو أحدث
- `إضافة` يمكن الآن تصفح RAR4/RAR5 ومعاينتها واستخراجها، بما في ذلك المحتوى أو الرؤوس المشفرة؛ وتبقى RAR للقراءة فقط عن قصد
- `إضافة` يمكن لصفحة الأرشيف الأصلية في AutoJs6 طلب كلمة المرور عند الفتح الأول أو أثناء الاستخراج ثم إعادة المحاولة دون فقدان المسار أو التحديد
- `إصلاح` يحتفظ الجزء الأول من RAR المقسم ببياناته الوصفية القابلة للقراءة، لكنه لا يعرض الاستخراج عند غياب الأجزاء المجاورة
- `إصلاح` تمسح كلمة المرور الخاطئة الإدخال السابق وتعيد المحاولة على لقطة أرشيف لم تتغير دون مغادرة الصفحة الأصلية
- `تحسين` تقرأ RAR مباشرة من واصف المضيف القابل للبحث عند توفره، ولا تضيف ABI أصلية، وتعيد استخدام فحوص السلامة المشتركة
- `اعتماد` أضيف Junrar 8.1.0 وSLF4J 2.0.17 لدعم RAR للقراءة فقط وفق شروط التراخيص المرفقة

#### v2.3.0

_2026/08/26_

- `ملاحظة` يتطلب هذا الاصدار بناء AutoJs6 6.8.0 المرافق مع Explorer Action v10 (رمز الاصدار 5276 او احدث)
- `إضافة` يمكن لصفحة الارشيف الاصلية الان استخراج المجلد الداخلي الحالي من شريط المسار او العناصر المحددة من شريط التحديد دون فتح صفحة ادارة منفصلة
- `إضافة` يكتب الاستخراج الاصلي عبر شجرة اخراج يملكها المضيف، مع التقدم والالغاء والترقيم الامن للتعارضات وتحديث Explorer تلقائيا
- `إصلاح` لم يعد الخروج من الارشيف على Android 7 يسبب تعطلا اثناء تنظيف المضيف لذاكرة المعاينة المؤقتة
- `إصلاح` تتمركز تسميات شريط تحديد الملفات ذي الاجراءات الخمسة تحت ايقوناتها في الشاشات الضيقة
- `تحسين` يظهر وضع تحديد الارشيف الان الخروج والاستخراج فقط، ويخفي عمليات نظام الملفات التي لا تنطبق داخل الارشيف

#### v2.2.0

_2026/08/26_

- `ملاحظة` يتطلب هذا الاصدار بناء AutoJs6 6.8.0 المقترن الذي يتضمن Explorer Action v9 (رمز الاصدار 5276 او احدث)
- `إضافة` يعرض «استخراج إلى...» الآن المجلد الحالي كوجهة موصى بها وينشئ مجلد إخراج باسم الأرشيف عبر معاملة دليل يملكها المضيف; اذا وجد اسم مكافئ يرقم بأمان ولا يغير المحتوى الموجود
- `إضافة` يمكن لادارة ZIP العادي ذي الجزء الواحد استيراد مجلد كامل عبر منتقي نظام Android, بما في ذلك الملفات المتداخلة والمجلدات الفارغة
- `إصلاح` يبقى مربع اختيار وجهة الاستخراج قابلا للاستخدام بالكامل على الشاشات القصيرة والضيقة ويعرض المسار الافتراضي الدقيق
- `إصلاح` تلغي عمليات الاستخراج في المجلد نفسه مخرجاتها غير المنشورة عند الالغاء او الفشل او انقطاع العملية او نفاد المساحة; وتستعيد الجلسة التالية معاملات المضيف المتوقفة من دون تغيير الارشيف المصدر
- `تحسين` ينشر Explorer Action v9 اشجار المجلدات المتحقق منها بصورة ذرية, ويعرض AutoJs6 مجلد الاخراج الجديد فور الاعتماد

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
