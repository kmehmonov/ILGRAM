# ILGRAM

**ILGRAM** — Istiqbol LUCK va Istiqbol LIGHT maktablari o'quvchilari uchun Telegram Android klientining
cheklangan interfeysli versiyasi.

ILGRAM yangi messenjer emas: u Telegram serverlari bilan ishlaydigan haqiqiy Telegram klienti
([DrKLO/Telegram](https://github.com/DrKLO/Telegram) asosida). MTProto, autentifikatsiya, xabar almashish,
media, sinxronizatsiya va shifrlash o'zgartirilmagan. Faqat o'quvchi ko'radigan interfeys va navigatsiya cheklangan:

* chatlar ro'yxatida faqat maktab tasdiqlagan **guruhlar (Chat ID)** va **shaxsiy chatlar (User ID)**;
* kanallar, botlar, arxiv, Saved Messages, global qidiruv, stories, kontaktlar, papkalar yashirin;
* begona chatga olib boradigan barcha yo'llar (username, avatar, mention, forward manbasi, havolalar, deep link,
  bildirishnoma) yagona `ILGRAMPolicy` orqali yopilgan;
* ichki brauzer, Mini Apps, tashqi havolalar, proxy, QR-login, Add/Switch account o'chirilgan;
* Forward va bildirishnomalar faqat ruxsatli chatlar uchun.

> ⚠️ Cheklovlar faqat shu ilova interfeysida amal qiladi va Telegram serveridagi huquqlarni o'zgartirmaydi.
> ILGRAM norasmiy ilova, Telegram FZ-LLC bilan bog'liq emas.

## Hujjatlar
| | |
|---|---|
| [Arxitektura va Source Audit](docs/ilgram/ARCHITECTURE.md) | ILGRAMPolicy, NavigationGuard, qayerga ulangan |
| [Build yo'riqnomasi](docs/ilgram/BUILD.md) | GitHub Actions, secretlar, api_id, ruxsatlar ro'yxati |
| [MDMesh orqali o'rnatish](docs/ilgram/MDMESH.md) | O'rnatish, yangilash, kiosk, batareya |
| [Test natijalari](docs/ilgram/TEST_RESULTS.md) | Avtomatik va qo'lda testlar |
| [Cheklovlar va xatarlar](docs/ilgram/LIMITATIONS_AND_RISKS.md) | Nima yopilmaydi va nega |
| [Telegram API muvofiqligi](docs/ilgram/TELEGRAM_API_COMPLIANCE.md) | api_id, nom, logotip, GPL |
| [Maxfiylik siyosati](docs/ilgram/PRIVACY.md) | O'quvchilar uchun |
| [O'zgartirilgan fayllar](docs/ilgram/CHANGED_FILES.md) | Telegram kodidagi har bir o'zgarish |

## Litsenziya
GNU GPL v2 yoki keyingi versiya (Telegram for Android bilan bir xil). Manba kodi shu repozitoriyda ochiq.

---

## Telegram for Android (asl README)

## Telegram messenger for Android

[Telegram](https://telegram.org) is a messaging app with a focus on speed and security. It’s superfast, simple and free.
This repo contains the official source code for [Telegram App for Android](https://play.google.com/store/apps/details?id=org.telegram.messenger).

## Creating your Telegram Application

We welcome all developers to use our API and source code to create applications on our platform.
There are several things we require from **all developers** for the moment.

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) for your application.
2. Please **do not** use the name Telegram for your app — or make sure your users understand that it is unofficial.
3. Kindly **do not** use our standard logo (white paper plane in a blue circle) as your app's logo.
3. Please study our [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and take good care of your users' data and privacy.
4. Please remember to publish **your** code too in order to comply with the licences.

### API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

### Compilation Guide

**Note**: In order to support [reproducible builds](https://core.telegram.org/reproducible-builds), this repo contains dummy release.keystore,  google-services.json and filled variables inside BuildVars.java. Before publishing your own APKs please make sure to replace all these files with your own.

You will require Android Studio 2025.1.4, Android NDK 27.2.12479018 and Android SDK 36.

1. Clone the Telegram source code with its submodules:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/DrKLO/Telegram.git Telegram
   ```
   In case you forgot the `--recursive` flag, change to the `Telegram` directory and run:
   ```bash
   git submodule init && git submodule update --init --recursive --depth=1
   ```
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in gradle.properties to access your  release.keystore
4.  Go to https://console.firebase.google.com/, create two android apps with application IDs org.telegram.messenger and org.telegram.messenger.beta, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.
5. Open the project in the Studio (note that it should be opened, NOT imported).
6. Fill out values in TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java – there’s a link for each of the variables showing where and which data to obtain.
7. You are ready to compile Telegram.

### Localization

We moved all translations to https://translations.telegram.org/en/android/. Please use it.
