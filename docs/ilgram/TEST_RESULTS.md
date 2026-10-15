# ILGRAM test natijalari

Sana: 2026-10-15. Kod: `master` (Telegram 13.0.2 asosida).

## Avtomatik testlar

| To'plam | Qayerda | Natija |
|---|---|---|
| Original Telegram build (o'zgartirishsiz) | GitHub Actions | ✅ muvaffaqiyatli (39 daq) |
| ILGRAM release APK build (imzolangan) | GitHub Actions | ✅ muvaffaqiyatli |
| Java kompilyatsiya tekshiruvi | GitHub Actions | ✅ |
| `ILGRAMRulesTest` — qoidalar (ID formatlari, 45 ta havola turi, sozlamalar) | oddiy JDK | ✅ 76/76 |
| `ILGRAMIntegrationTest` — haqiqiy `MessagesController`, NavigationGuard, Browser va h.k. | Android 14 emulator | ✅ 15/15 |
| `ILGRAMSmokeTest` — ilovani ishga tushirish, xavfli deep linklar, internet uzilishi | Android 14 emulator | ✅ 2/2 |
| Yangilashda ma'lumot saqlanishi (`adb install -r`) | Android 14 emulator | ✅ PASS |

Emulator testlari Telegram akkauntisiz ishlaydi: ular ILGRAM qatlamini haqiqiy Telegram klasslari ichida tekshiradi
(soxta guruh/foydalanuvchi obyektlari `MessagesController` ga yuklanadi va filtrlash, navigatsiya, havolalar tekshiriladi).

Emulator skrinshotlari tufayli topilgan va tuzatilgan xatolar:
* Login'dan oldin `tg://proxy` havolasi "Proxy Server" oynasini ochardi → `AndroidUtilities.handleProxyIntent` da bloklandi.
* Login'dan oldingi kirish ekranida Telegram logotipi va nomi chiqardi → o'quvchi rejimida bu ekran o'tkazib yuboriladi, ilova to'g'ridan-to'g'ri telefon raqami kiritish ekranidan boshlanadi.

## Spec §17 bo'yicha holat

Belgilar: ✅ avtomatik tekshirildi · 🟡 qisman / qoida darajasida tekshirildi · ⬜ real qurilma va Telegram akkaunti kerak

| # | Test | Holat | Qanday tekshirildi |
|---|---|---|---|
| 1 | Telegram login ishlaydi | ⬜ | Login ekrani ochilishi emulatorda ✅. Haqiqiy login uchun `ILGRAM_API_ID/HASH` kerak |
| 2 | Ruxsat etilgan guruh ko'rinadi | ✅ | `t02_t03_chatList_showsOnlyAllowedChats` |
| 3 | Begona guruh yashiriladi | ✅ | o'sha test (+ nomida "LUCK" bo'lgan begona guruh) |
| 4 | Nomi o'zgarsa ham Chat ID bo'yicha ruxsat saqlanadi | ✅ | `t04_renamedGroup_staysAllowedById` |
| 5 | Ruxsatli User ID bilan chat ochiladi | ✅ | `t05_t06_openChat_onlyAllowed` |
| 6 | Begona User ID bilan chat ochilmaydi | ✅ | o'sha test |
| 7 | Begona chat bildirishnomasi ko'rinmaydi | 🟡 | `t07_...` (siyosat darajasida). Haqiqiy push bilan real qurilmada |
| 8 | Guruhdagi username cheklovni aylanib o'tmaydi | 🟡 | username → chat/profil ochilishi NavigationGuard'dan o'tadi (`t05`, `t08`); real guruhda qo'lda |
| 9 | Avatar orqali begona chat ochilmaydi | ✅ | `t08_t09_profiles_ofForeignUsersAreBlocked` |
| 10 | Forward faqat ruxsatli chatlarga | ✅ | `t10_forwardTargets_onlyAllowed` |
| 11 | Kanal havolasi ochilmaydi | ✅ | `t11_t12_t29_links`, `ILGRAMRulesTest` |
| 12 | Bot havolasi ochilmaydi | ✅ | o'sha |
| 13 | Ichki brauzer ochilmaydi | ✅ | `t13_t14_webViews_disabled` + kod |
| 14 | Mini Apps ochilmaydi | ✅ | o'sha + smoke (startapp/startattach) |
| 15 | Tashqi havola brauzerni ishga tushirmaydi | ✅ | `t15_externalLink_neverStartsABrowser` (ActivityMonitor) |
| 16 | Add Account ko'rinmaydi | 🟡 | `t16_t17_settings_simplified`, `LoginActivity` guard; UI real qurilmada |
| 17 | Switch Account ko'rinmaydi | 🟡 | o'sha |
| 18 | Yangi guruh yaratish ko'rinmaydi | ✅ | `t18_creationAndDiscoveryScreens_areBlocked` |
| 19 | Rasm yuborish | ⬜ | Telegram mexanizmi o'zgartirilmagan; akkaunt kerak |
| 20 | Video yuborish | ⬜ | o'sha |
| 21 | PDF yuborish | ⬜ | o'sha (kiosk rejimida DocumentsUI allowlist — `MDMESH.md`) |
| 22 | Ovozli xabar | ⬜ | o'sha |
| 23 | Reply | ⬜ | o'sha |
| 24 | Guruh ichida qidiruv | ⬜ | ChatActivity qidiruvi o'zgartirilmagan |
| 25 | Fon rejimida xabar olish | ⬜ | Foreground keep-alive xizmat qo'shilgan; real qurilmada batareya sozlamasi bilan tekshirish kerak |
| 26 | Internet uzilganda barqarorlik | ✅ | `networkLoss_appStaysAlive` (airplane mode) |
| 27 | Qayta ishga tushganda konfiguratsiya saqlanadi | 🟡 | Dizayn bo'yicha: konfiguratsiya APK ichida / MDM managed config'da (qurilma xotirasiga bog'liq emas) |
| 28 | APK yangilanganda sessiya saqlanadi | ✅ | `adb install -r` dan keyin ilova fayllari saqlandi; bir xil imzo kaliti |
| 29 | Android deep link cheklovni chetlab o'tmaydi | ✅ | `launch_and_hostileDeepLinks_doNotCrash` + `LinkManager`/proxy tuzatishi |
| 30 | Standart xabar almashish buzilmaydi | ⬜ | MTProto/xabar kodi o'zgartirilmagan (`CHANGED_FILES.md`); akkaunt bilan tekshirish kerak |

## Real qurilmada qilinadigan testlar (≈30 daqiqa)
Shart: `ILGRAM_API_ID/HASH` va `ILGRAM_CONFIG` secretlari qo'shilgan, yangi APK yig'ilgan.

1. Samsung va Redmi telefonga APK o'rnating, sinov o'quvchi raqami bilan kiring (1).
2. Ro'yxatda faqat config'dagi guruh(lar) va o'qituvchi chatini ko'ring (2, 3).
3. Ruxsatli guruhda: rasm, video, PDF, ovozli xabar yuboring; reply, reaction, tahrirlash, o'chirish, guruh ichida qidiruv (19–24).
4. Guruhdagi sinfdoshi avatarini, username'ini, mention'ini bosing — hech narsa ochilmasligi va "ILGRAM: bu bo'lim..." xabari (8, 9).
5. Xabarni forward qiling — ro'yxatda faqat ruxsatli chatlar (10).
6. Begona akkauntdan o'quvchiga shaxsiy xabar yuboring — bildirishnoma kelmasligi (7).
7. Ilovani yopib, o'qituvchidan xabar yuboring — bildirishnoma kelishi (25). Qurilmani qayta yoqib takrorlang (27).
8. Yangi build'ni MDMesh orqali o'rnating — qayta login so'ralmasligi (28).
