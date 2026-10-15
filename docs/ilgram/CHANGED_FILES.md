# O'zgartirilgan fayllar ro'yxati

Bazaviy commit: `f32ffee` (DrKLO/Telegram 13.0.2 (7175)). Barcha ILGRAM o'zgarishlari kodda `ILGRAM` izohi bilan belgilangan (`grep -rn ILGRAM`).

| Fayl | + | − | Maqsad |
|---|---|---|---|
| `.github/workflows/build.yml` | 82 | 0 | CI / test / hujjat |
| `.github/workflows/compile-check.yml` | 31 | 0 | CI / test / hujjat |
| `.github/workflows/device-tests.yml` | 81 | 0 | CI / test / hujjat |
| `TMessagesProj/build.gradle` | 16 | 0 | api_id/api_hash BuildConfig, ARM ABI, ccache |
| `TMessagesProj/google-services.json` | 87 | 0 | ILGRAM paket nomlari (build uchun) |
| `TMessagesProj/src/main/AndroidManifest.xml` | 6 | 2 | Managed configuration meta-data; NotificationsService (exported=false, remoteMessaging) |
| `TMessagesProj/src/main/assets/ilgram.properties` | 33 | 0 | YANGI. Standart konfiguratsiya |
| `TMessagesProj/src/main/java/org/telegram/ilgram/ILGRAMPolicy.java` | 412 | 0 | YANGI. Markaziy UI siyosati (Android): config yuklash, MDM managed config, NavigationGuard, havolalar, bildirishnomalar |
| `TMessagesProj/src/main/java/org/telegram/ilgram/ILGRAMRules.java` | 516 | 0 | YANGI. Sof Java qoidalar: konfiguratsiya, ID normalizatsiyasi, dialog/havola/sozlama qarorlari |
| `TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java` | 12 | 0 | ILGRAMPolicy.init(); keep-alive xizmatni ishga tushirish |
| `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java` | 8 | 6 | api_id/api_hash secretdan; Telegram update tekshiruvi, SafetyNet, passkeys, billing o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/messenger/ContactsController.java` | 3 | 0 | Telefon kitobi o'qilmaydi |
| `TMessagesProj/src/main/java/org/telegram/messenger/MediaDataController.java` | 23 | 2 | Top peers va launcher shortcutlar faqat ruxsatli chatlar |
| `TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java` | 32 | 4 | Chat filtering: sortDialogs(), getAllDialogs(); arxiv/promo/Saved Messages; forward ro'yxati |
| `TMessagesProj/src/main/java/org/telegram/messenger/NotificationsController.java` | 59 | 3 | Notification filtering (yangi, yuklangan, o'qilgan xabarlar) |
| `TMessagesProj/src/main/java/org/telegram/messenger/NotificationsService.java` | 55 | 1 | Foreground keep-alive xizmat (FCM o'rniga) |
| `TMessagesProj/src/main/java/org/telegram/messenger/PushListenerController.java` | 3 | 0 | O'quvchi rejimida FCM ishlatilmaydi |
| `TMessagesProj/src/main/java/org/telegram/messenger/browser/Browser.java` | 27 | 0 | Link policy: tashqi brauzer, ichki brauzer, tashqi ilovalar |
| `TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java` | 3 | 0 | Fon ulanishi doim yoqilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarLayout.java` | 6 | 0 | NavigationGuard: presentFragment/addFragmentToStack |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java` | 3 | 0 | NavigationGuard: showAsSheet |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionIntroActivity.java` | 5 | 0 | getActionType() (QR-login aniqlash uchun) |
| `TMessagesProj/src/main/java/org/telegram/ui/Adapters/DialogsAdapter.java` | 7 | 3 | Kontakt tavsiyalari va reply/forward tavsiyalari filtrlangan |
| `TMessagesProj/src/main/java/org/telegram/ui/Adapters/DialogsSearchAdapter.java` | 1 | 1 | getAllDialogsInternal() (ichki ro'yxat) |
| `TMessagesProj/src/main/java/org/telegram/ui/CameraScanActivity.java` | 4 | 0 | QR skaner (desktop login) o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/DataSettingsActivity.java` | 9 | 3 | Proxy bo'limi yashirin |
| `TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java` | 22 | 5 | Global qidiruv, folder tablari, FAB, stories, kontakt so'rovi o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java` | 5 | 2 | Login oldidan proxy havolalari qabul qilinmaydi; Telegram intro ekrani o'tkazib yuboriladi |
| `TMessagesProj/src/main/java/org/telegram/ui/LauncherIconController.java` | 13 | 0 | Faqat ILGRAM ikonkasi |
| `TMessagesProj/src/main/java/org/telegram/ui/LinkManager.java` | 4 | 0 | Android deep link / intent policy |
| `TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java` | 42 | 1 | Faqat Chatlar + Sozlamalar tablari; akkaunt almashtirish yo'q |
| `TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java` | 116 | 0 | Soddalashtirilgan sozlamalar, Yordam/Ilova haqida, debug menyu o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/Stories/StoriesController.java` | 8 | 2 | Stories o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/bots/BotWebViewAttachedSheet.java` | 4 | 0 | Mini Apps o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/bots/BotWebViewSheet.java` | 4 | 0 | Mini Apps o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/web/BotWebViewContainer.java` | 14 | 0 | Web view hech qanday URL yuklamaydi |
| `TMessagesProj/src/main/res/drawable/ilgram_icon_background.xml` | 5 | 0 | YANGI. Ikonka foni |
| `TMessagesProj/src/main/res/drawable/ilgram_icon_foreground.xml` | 11 | 0 | YANGI. Ikonka "IL" |
| `TMessagesProj/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | - | - | ILGRAM adaptiv ikonka |
| `TMessagesProj/src/main/res/values*/strings.xml` | - | - | AppName = ILGRAM |
| `TMessagesProj/src/main/res/values/ilgram_strings.xml` | 12 | 0 | YANGI. Managed configuration matnlari |
| `TMessagesProj/src/main/res/xml/ilgram_app_restrictions.xml` | 33 | 0 | YANGI. MDM managed configuration sxemasi |
| `TMessagesProj_App/build.gradle` | 1 | 1 | Faqat ARM ABI |
| `TMessagesProj_App/google-services.json` | 87 | 0 | ILGRAM paket nomlari (build uchun) |
| `TMessagesProj_AppTests/build.gradle` | 1 | 1 | ABI override (emulator testlari) |
| `TMessagesProj_AppTests/src/androidTest/java/org/telegram/ilgram/ILGRAMIntegrationTest.java` | 398 | 0 | CI / test / hujjat |
| `TMessagesProj_AppTests/src/androidTest/java/org/telegram/ilgram/ILGRAMSmokeTest.java` | 108 | 0 | CI / test / hujjat |
| `gradle.properties` | 1 | 1 | applicationId uz.istiqbol.ilgram |
| `ilgram/ci/prepare.sh` | 36 | 0 | CI / test / hujjat |
| `ilgram/ci/run-device-tests.sh` | 25 | 0 | CI / test / hujjat |
| `ilgram/tests/ILGRAMRulesTest.java` | 137 | 0 | CI / test / hujjat |
| `ilgram/tests/run-tests.sh` | 7 | 0 | CI / test / hujjat |
| `TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java` | 5 | 0 | Proxy havolalari (login oldidan ham) bloklangan |
| `TMessagesProj/src/main/java/org/telegram/messenger/HashtagSearchController.java` | 6 | 0 | Barcha chatlar/public post bo'yicha hashtag qidiruvi o'chirilgan |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/ShareAlert.java` | 19 | 0 | Share oynasi qidiruvi faqat ruxsatli chatlar, server qidiruvi yo'q |

Bundan tashqari: `mipmap-*/ic_launcher*.png` va `drawable-*/notification.png` — ILGRAM ikonkalari (Telegram logotipi o'rniga).
