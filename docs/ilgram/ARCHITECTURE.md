# ILGRAM arxitekturasi

ILGRAM = Telegram Android Client (DrKLO/Telegram, 13.0.2) + Restricted User Interface.

ILGRAM yangi messenjer emas. MTProto, serverga ulanish, autentifikatsiya, xabar yuborish/qabul qilish,
media yuklash, sinxronizatsiya, shifrlash va Forward/Reply mexanizmlari **o'zgartirilmagan**.
O'zgartirishlar faqat taqdimot (UI), navigatsiya va sozlamalar qatlamida.

> UI cheklovi Telegram serveridagi haqiqiy ruxsat emas. Yashirilgan chat serverda mavjud bo'lib qoladi,
> o'quvchi akkaunti boshqa klientda (masalan, kompyuterda) to'liq ishlaydi. Qurilmadagi boshqa
> messenjer/brauzerlarni MDMesh bloklaydi.

## 1. Komponentlar

```
TMessagesProj/src/main/java/org/telegram/ilgram/
  ILGRAMRules.java    — sof Java: konfiguratsiya modeli, ID normalizatsiyasi, barcha allow/deny qarorlari
  ILGRAMPolicy.java   — Android qatlami: konfiguratsiyani yuklaydi (assets + MDM managed config),
                        Telegram obyektlari (TLRPC, BaseFragment, Uri) bilan ishlaydi
TMessagesProj/src/main/assets/ilgram.properties  — standart konfiguratsiya (CI'da secret bilan almashtiriladi)
TMessagesProj/src/main/res/xml/ilgram_app_restrictions.xml — MDM uchun managed configuration sxemasi
```

Promptdagi konseptual interfeyslar va ularning haqiqiy nomlari:

| Prompt | Haqiqiy kod |
|---|---|
| `isAllowedGroup(chatId)` | `ILGRAMPolicy.isAllowedGroup(account, chatId)` |
| `isAllowedUser(userId)` | `ILGRAMPolicy.isAllowedUser(account, userId)` |
| `canOpenDialog(dialogId)` | `ILGRAMPolicy.canOpenDialog(account, dialogId)` (maxfiy chatlarni ham hisobga oladi) |
| `canNavigate(destination)` | `ILGRAMPolicy.canPresentFragment(BaseFragment)` |
| `canForwardTo(dialogId)` | `ILGRAMPolicy.canForwardTo(account, dialogId)` |
| `shouldShowNotification(dialogId)` | `ILGRAMPolicy.shouldShowNotification(account, dialogId)` |
| `isSettingVisible(settingKey)` | `ILGRAMPolicy.isSettingVisible(key)` |
| havola tekshiruvi | `ILGRAMPolicy.classifyUrl(url)` / `canOpenUrl(ctx, uri)` |
| Brauzer / Mini Apps | `ILGRAMPolicy.isWebViewAllowed()` |

Hech bir ekran o'z qoidasini yozmaydi — hammasi shu ikki klassga murojaat qiladi.

## 2. Ruxsat qoidalari

* **Guruhlar** faqat Chat ID bo'yicha. Qabul qilinadigan formatlar: Bot API `-1001234567890` (supergroup/kanal),
  `-123456789` (oddiy guruh), yoki `channel:1234567890` / `chat:123456789`. Nom hech qachon tekshirilmaydi:
  guruh nomi o'zgarsa ham ruxsat saqlanadi, nomida "luck" bo'lgan begona guruh ruxsat olmaydi.
* Ruxsat etilgan oddiy guruh supergroupga aylantirilsa (`migrated_to`), yangi supergroup avtomatik ruxsat oladi.
* **Shaxsiy chatlar** faqat User ID bo'yicha (username/telefon emas).
* **Telegram xizmat xabarlari (777000)** — kirish kodlari va xavfsizlik ogohlantirishlari uchun ko'rinadi
  (`allow_service_notifications=true`), lekin Forward manzili sifatida chiqmaydi.
* **Saved Messages**, kanallar, botlar, arxiv, jamoalar (communities) — yashirin (ID ro'yxatda bo'lmasa).
* **Maxfiy chatlar** — standart holatda o'chirilgan (`allow_secret_chats=false`).
* Konfiguratsiyani o'qib bo'lmasa — **locked-down** holat: o'quvchi rejimi yoqilgan, hech narsa ruxsat etilmagan.

## 3. Source Audit natijasi — cheklovlar qayerga ulangan

| Qatlam | Telegram klassi / metod | ILGRAM o'zgarishi |
|---|---|---|
| Chat list presentation | `MessagesController.sortDialogs()` | Ruxsatsiz dialoglar barcha ko'rinish ro'yxatlaridan (`dialogsByFolder`, `dialogsForward`, `dialogsUsersOnly`, `dialogsGroupsOnly`, filtr ro'yxatlari, o'qilmaganlar hisobi) chiqarib tashlanadi. `allDialogs`/`dialogs_dict` o'zgarmaydi — sinxronizatsiya buzilmaydi. Arxivdagi ruxsatli chat asosiy ro'yxatda qoladi. Promo-kanal va Saved Messages qo'shilmaydi. |
| | `MessagesController.getAllDialogs()` | UI uchun filtrlangan nusxa qaytaradi (share sheet, quick share, pickerlar, mentionlar). Ichki ehtiyoj uchun `getAllDialogsInternal()`. |
| | `DialogsAdapter` | "Kontaktlar" tavsiyasi, reply/forward manbasi tavsiyasi faqat ruxsatli bo'lsa. |
| | `MediaDataController` (top peers, launcher shortcuts) | Faqat ruxsatli chatlar. |
| UI visibility | `MainTabsActivity` | Faqat ikki sahifa: Chatlar + Sozlamalar. Kontaktlar, Qo'ng'iroqlar, Profil/akkaunt almashtirish tablari va uzoq bosish menyulari yo'q. O'qilmaganlar soni faqat ko'rinadigan chatlardan. |
| | `DialogsActivity` | Global qidiruv, papka (folder) tablari, "yangi xabar/guruh/kanal" tugmasi, stories, kontakt ruxsati so'rovi o'chirilgan. |
| | `StoriesController` | Stories yuklanmaydi va ko'rsatilmaydi. |
| Navigation routing (NavigationGuard) | `ActionBarLayout.presentFragment()`, `addFragmentToStack()`, `BaseFragment.showAsSheet()` | Har bir ekran ochilishidan oldin `ILGRAMPolicy.canPresentFragment()`. `ChatActivity`, `ProfileActivity`, `TopicsFragment`, `MediaActivity` — dialog ID bo'yicha; `ContactsActivity`, `GroupCreate*`, `ChannelCreateActivity`, `CallLogActivity`, `Filters*`, `Proxy*`, `Premium/Stars`, `WebviewActivity`, `CameraScanActivity`, `PaymentFormActivity`, `ManageLinks/LinkEdit`, ikkinchi `LoginActivity`, QR-login — bloklangan. Bu username, avatar, a'zolar ro'yxati, reply muallifi, forward manbasi, mention, profildagi "Message", notification, bubble, share va deep linklarning **hammasini** bitta joyda yopadi. |
| Link handling | `Browser.openUrl()`, `openAsInternalIntent()`, `openInTelegramBrowser()`, `openInExternalBrowser()`, `openInExternalApp()` | Oddiy URL bosilganda hech narsa ochilmaydi (matn ko'rinadi). Telegram havolalari faqat ilova ichida ochiladi va manzil chat yana NavigationGuard'dan o'tadi. Invite, proxy, addlist, login, bot start/startapp, stories va boshqa maxsus havolalar bloklangan. |
| | `LinkManager.handle()` (Android intent / deep link) | Xuddi shu qoida tashqi intentlar uchun. |
| | `LaunchActivity` | Login oldidan proxy havolalari qabul qilinmaydi. |
| In-App Browser / Mini Apps | `BotWebViewContainer.MyWebView.loadUrl/postUrl`, `BotWebViewSheet.show()`, `BotWebViewAttachedSheet.show()`, `CameraScanActivity.showAsSheet()` | Web view hech qanday sahifa yuklamaydi, Mini App oynalari ochilmaydi. |
| Settings UI | `SettingsActivity.fillItems()` | Ko'rinadi: Hisob, Chat sozlamalari (til/tema/shrift), Maxfiylik va xavfsizlik (sessiyalar, 2FA, hisobni o'chirish), Bildirishnomalar, Ma'lumot va xotira (kesh), Qurilmalar, Quvvatni tejash, Til, ILGRAM → Yordam / Ilova haqida. Yashirin: akkauntlar ro'yxati, Folders, Premium, Stars, Business, Gifts, Wallet, Ask a Question, FAQ, debug menyu. |
| | `DataSettingsActivity` | Proxy bo'limi yashirin. |
| Search UI | `DialogsActivity.showSearch()` / `isSupportSearch()` | Global qidiruv yo'q. Guruh ichidagi qidiruv (ChatActivity) ishlaydi. |
| Forward destination selection | `sortDialogs()` → `dialogsForward` | Faqat ruxsatli guruhlar va User ID'lar; Saved Messages va 777000 yo'q. Telegram'ning forward mexanizmi o'zgarmagan. |
| Notification presentation | `NotificationsController.processNewMessages()`, `processDialogsUpdateRead()`, `processLoadedUnreadMessages()` | Ruxsatsiz chatlar bildirishnoma ham, badge hisobi ham bermaydi. |
| Account management UI | Main tabs, `SettingsActivity`, `LoginActivity` guard | Add/Switch account yo'q. Chiqish (Log out) — Sozlamalar → Hisob orqali saqlangan. |
| Kontaktlar | `ContactsController.hasContactsPermission()`, `DialogsActivity` | Telefon kitobi o'qilmaydi va serverga yuklanmaydi. |
| Push | `PushListenerController`, `ApplicationLoader.startPushService()`, `NotificationsService`, `ConnectionsManager.isPushConnectionEnabled()` | Uchinchi tomon api_id uchun Telegram FCM push yubormaydi. Shuning uchun ILGRAM foreground keep-alive xizmati bilan ulanishni ochiq ushlaydi (Android 14+ da `remoteMessaging` turi). |
| Branding | `strings.xml`, ikonkalar, `LauncherIconController`, `gradle.properties`, `BuildVars` | Nomi ILGRAM, o'z ikonkasi, `applicationId = uz.istiqbol.ilgram`, Telegram yangilanish tekshiruvi o'chirilgan, api_id/api_hash CI secretlaridan. |

## 4. Konfiguratsiya oqimi

```
assets/ilgram.properties  (CI: ILGRAM_CONFIG secret yoziladi)
        +
Android managed configuration (MDM, RestrictionsManager)  — bo'lsa, ustun turadi
        ↓
ILGRAMRules.parse()  →  ILGRAMPolicy.config
        ↓ (ACTION_APPLICATION_RESTRICTIONS_CHANGED)
sortDialogs() + dialogsNeedReload → interfeys darhol yangilanadi
```

Backend yaratilmagan. Keyinchalik markaziy server kerak bo'lsa, u faqat shu Properties kalitlarini
yetkazib beruvchi alohida modul bo'ladi.

## 5. Testlanuvchanlik

* `ILGRAMRules` — sof Java, `ilgram/tests/run-tests.sh` (oddiy JDK) bilan tekshiriladi.
* `ILGRAMIntegrationTest` / `ILGRAMSmokeTest` — haqiqiy Telegram klasslari bilan emulatorda (CI).
