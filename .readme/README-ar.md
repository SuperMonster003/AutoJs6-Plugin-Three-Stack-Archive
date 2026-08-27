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
- تصحيح ترميز أسماء ZIP مباشرة من شريط مسار المضيف; تعيد جلسة القراءة نفسها بناء الفهرس وتحافظ قدر الإمكان على المسار الداخلي الحالي والتحديد المتاح.
- تصفح مجموعات `.z01 + .zip` القياسية الكاملة ومجموعات WinRAR الحديثة `partN.rar` ومعاينتها واستخراجها عبر واصفات أجزاء مجاورة محدودة ومصرح بها من المضيف؛ ويفشل الجزء المفقود أو المتغير بخطأ صريح.
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

> يتطلب التكامل الأصلي بناء AutoJs6 6.8.0 المقترن مع Explorer Action v13 (رمز الإصدار 5276 أو أحدث). تبقى RAR والأرشيفات المجزأة للقراءة فقط؛ ويقتصر التحرير على ملفات `.zip` العادية أحادية الجزء. افتح ZIP المجزأ القياسي من ملف `.zip` الأخير، ومجموعة WinRAR الحديثة من أول `partN.rar`، مع وجود كل الأجزاء المطلوبة في المجلد نفسه. لا تتوفر تدفقات `.zip.001` أو 7Z المجزأ أو تشفير الأسماء عند الإنشاء أو حذف المصادر أو تحرير محتوى 7Z وRAR وعائلة TAR.

### الاستخدام

1. ثبت Archive Manager وفعله من مركز إضافات AutoJs6.
2. اضغط الإجراء الرئيسي لملف مدعوم أو اختر فتح الأرشيف، ثم تصفحه كمجلد عادي باستخدام شريط مسار المضيف.
3. استخدم إجراء الاستخراج في شريط المسار للمجلد الداخلي الحالي أو إجراء الترميز لتصحيح أسماء ZIP، أو اضغط مطولا لاستخراج تحديد، أو اختر استخراج إلى... من قائمة الملف للأرشيف كاملا. تظهر مطالبة كلمة المرور عند الحاجة.
4. اختر ضغط... لملف أو مجلد، أو حدد عدة عناصر في المجلد نفسه واستخدم الإجراء في الشريط السفلي.
5. اختر إدارة الأرشيف... فقط عند إضافة محتوى أو إعادة تسميته أو حذفه داخل ZIP عادي أحادي الجزء.

### الأذونات والبيانات

لا يطلب Archive Manager إذن التخزين أو الشبكة. يقدم المضيف واصفات قراءة قصيرة العمر ومعاملات مثبتة على UID الإضافة، لذلك لا تستطيع الإضافة اختيار مسارات عشوائية. ينقل Explorer Action v11 كلمة المرور في طلب متزامن محدود فقط، ويمسح الطرفان المخازن فورا ولا يحفظانها. لا يضيف Explorer Action v12 إلا فهرسا محدودا ومرتبطا بالجلسة للأجزاء المجاورة التي وافق عليها المضيف: تتلقى الإضافة معرفات معتمة بدلا من المسارات، ويعاد التحقق من UID المتصل وهوية الملف وحجمه ووقت تعديله وعمره قبل الاستخدام. يعيد Explorer Action v13 فهرسة المصدر المؤقت نفسه فقط، ويحد قائمة أسماء الترميز، ويحافظ على الحالة السابقة حتى يجهز فهرس بديل كامل. تجعل نسخ التشغيل التي لا يمكن تجنبها تنظيف كلمة المرور بأفضل جهد. تبقى المسارات الخطرة معزولة، ويتحقق من الإخراج قبل النشر، ولا يعطل تأكيد الميزانية فحوص السلامة البنيوية.

### Roadmap

تتابع الأعمال المتبقية بعناصر قابلة للتأشير: دراسة `.zip.001` و7Z المجزأ، وإعادة البناء القابلة للكتابة لما بعد ZIP، ومعاملات التراجع أو حذف المصدر، ومراجعة إمكانية الوصول، وبقية مصفوفة الأجهزة وأدوات الإنتاج.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### ملاحظات الإصدار

#### v2.6.0

_2026/08/27_

- `ملاحظة` يتطلب هذا الإصدار بناء AutoJs6 6.8.0 المقترن مع Explorer Action v13 (رمز الإصدار 5276 أو أحدث)
- `إضافة` تتيح صفحة الأرشيف الأصلية الآن اختيار ترميز أسماء ZIP من شريط المسار دون فتح صفحة الإدارة; ويحتفظ التغيير بالمجلد الداخلي الحالي والعناصر المحددة التي ما زالت موجودة
- `إضافة` يعيد Explorer Action v13 فهرسة المصدر المؤقت داخل جلسة القراءة نفسها ويستخدم معرفات عناصر ثابتة لاستعادة أعمق مسار متاح والعناصر التي ما زالت موجودة
- `تحسين` لا ينشر الفهرس البديل إلا بعد اكتماله; ويحافظ الاختيار غير الصالح أو فشل الفحص أو المعاينة أو الاستخراج النشط على الفهرس السابق, بينما تبقى كلمة المرور في ذاكرة قابلة للمسح فقط

#### v2.5.0

_2026/08/27_

- `ملاحظة` يتطلب هذا الإصدار بناء AutoJs6 6.8.0 المقترن مع Explorer Action v12 (رمز الإصدار 5276 أو أحدث)
- `إضافة` يمكن الآن تصفح مجموعات `.z01 + .zip` القياسية الكاملة ومجموعات WinRAR الحديثة `partN.rar` ومعاينتها واستخراجها في الصفحة الأصلية؛ وتبقى الأرشيفات المجزأة للقراءة فقط
- `إضافة` يقدم Explorer Action v12 فهرسا محدودا فقط للأجزاء المجاورة التي وافق عليها المضيف، ويفتح كل جزء بمعرف معتم كواصف للقراءة فقط من دون كشف مجلد أو مسار في نظام الملفات
- `إصلاح` تقبل بيانات دليل الجزء الأخير من ZIP المجزأ بصورة صحيحة على Android 7 والأحدث، وتقرأ البيانات عبر كل الأجزاء المصرح بها من دون اعتبار الجزء الأخير تالفا
- `إصلاح` لم تعد قيمة CRC لمقطع RAR تقارن بالبيانات المعاد تجميعها؛ وتنتج الأجزاء المفقودة أو المتغيرة بعد الفتح أخطاء مستقرة محددة النوع
- `تحسين` تخضع أعداد الأجزاء وأسماؤها ومعرفاتها وطلبات فتحها وUID المتصل وهوية الملفات وعمر الجلسة لحدود وإعادة تحقق؛ ويحذف الترحيل المنقطع كل بقايا الذاكرة المؤقتة الخاصة

#### v2.4.0

_2026/08/26_

- `ملاحظة` يتطلب هذا الإصدار AutoJs6 6.8.0 مع Explorer Action v11 ورمز إصدار 5276 أو أحدث
- `إضافة` يمكن الآن تصفح RAR4/RAR5 ومعاينتها واستخراجها، بما في ذلك المحتوى أو الرؤوس المشفرة؛ وتبقى RAR للقراءة فقط عن قصد
- `إضافة` يمكن لصفحة الأرشيف الأصلية في AutoJs6 طلب كلمة المرور عند الفتح الأول أو أثناء الاستخراج ثم إعادة المحاولة دون فقدان المسار أو التحديد
- `إصلاح` يحتفظ الجزء الأول من RAR المقسم ببياناته الوصفية القابلة للقراءة، لكنه لا يعرض الاستخراج عند غياب الأجزاء المجاورة
- `إصلاح` تمسح كلمة المرور الخاطئة الإدخال السابق وتعيد المحاولة على لقطة أرشيف لم تتغير دون مغادرة الصفحة الأصلية
- `تحسين` تقرأ RAR مباشرة من واصف المضيف القابل للبحث عند توفره، ولا تضيف ABI أصلية، وتعيد استخدام فحوص السلامة المشتركة
- `اعتماد` أضيف Junrar 8.1.0 وSLF4J 2.0.17 لدعم RAR للقراءة فقط وفق شروط التراخيص المرفقة

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
