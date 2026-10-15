# Telegram API shartlariga muvofiqlik hisoboti

Manbalar: https://core.telegram.org/api/terms, https://core.telegram.org/api/obtaining_api_id, DrKLO/Telegram README.

| Talab | ILGRAM holati |
|---|---|
| O'z **api_id** ni olish | ✅ Kodda Telegram'ning rasmiy api_id'si yo'q (`BuildVars.APP_ID = BuildConfig.ILGRAM_API_ID`). Qiymat GitHub secretidan build vaqtida beriladi. ❗ Maktab my.telegram.org'da o'z api_id'sini olishi kerak (`BUILD.md`). |
| "Telegram" nomini ishlatmaslik yoki norasmiyligini aniq ko'rsatish | ✅ Ilova nomi **ILGRAM**; "Ilova haqida" oynasida norasmiy ekani va Telegram asosida qurilgani yozilgan. |
| Telegram logotipini (ko'k doirada oq qog'oz samolyot) ishlatmaslik | ✅ O'z ikonkasi ("IL"), bildirishnoma ikonkasi ham almashtirilgan; Telegram'ning muqobil ikonkalari o'quvchi rejimida yoqilmaydi. |
| Xavfsizlik bo'yicha ko'rsatmalar (security guidelines), foydalanuvchi ma'lumotlarini himoya qilish | ✅ MTProto, shifrlash, sessiya va saqlash mexanizmlari o'zgartirilmagan. Xabarlar/kalitlar hech qayerga yuborilmaydi. |
| Kodni ochiq e'lon qilish (GPL v2) | ✅ `github.com/kmehmonov/ILGRAM` — ochiq. "Ilova haqida"da havola bor. |
| Reklama va foydalanuvchini aldash yo'q, xabarlarni soxtalashtirmaslik | ✅ Serverdan kelgan xabarlar o'chirilmaydi va o'zgartirilmaydi; faqat ko'rsatilmaydi. |
| Telegram'ning `SAFETYNET_KEY`, Google auth, passkeys kabi rasmiy ilovaga tegishli kalitlaridan foydalanmaslik | ✅ SafetyNet kaliti bo'shatildi, passkeys o'chirildi, billing o'chirildi. |
| Telegram'dan yangilanish yuklab olmaslik (rasmiy APK'ni taklif qilmaslik) | ✅ `CHECK_UPDATES=false`, yangilanishlar faqat MDMesh orqali. |
| Push xizmatlari | ℹ️ Telegram FCM'ni faqat rasmiy ilovalar uchun yuboradi; ILGRAM foreground ulanishdan foydalanadi (server tomonida hech narsa o'zgarmaydi). |
| Foydalanuvchi ma'lumotlari va maxfiylik siyosati | ✅ `PRIVACY.md`. |

## Ochiq savollar
* `google-services.json` — repozitoriydagi DrKLO namunaviy fayliga ILGRAM paket nomlari qo'shilgan (build ishlashi uchun). ILGRAM o'quvchi rejimida
  Firebase push'dan foydalanmaydi. O'z Firebase loyihangiz kerak emas.
* Telegram'ning Google Maps API kaliti manifestda qolgan (joylashuv xaritasi). Agar ishlatilmasa, keyingi bosqichda o'chirish yoki o'z kalitingiz bilan almashtirish tavsiya etiladi.
