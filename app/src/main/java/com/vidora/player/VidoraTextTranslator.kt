package com.vidora.player

import android.content.Context

object VidoraTextTranslator {

    fun translate(
        context: Context,
        text: String
    ): String {

        if (
            VidoraLanguageManager.isPersian(
                context
            )
        ) {
            return text
        }

        return translations[text] ?: text
    }

    fun t(
        context: Context,
        text: String
    ): String {

        return translate(
            context,
            text
        )
    }

    private val translations =
        mapOf(

            "تنظیمات Vidora Player" to
                "Vidora Player Settings",

            "تنظیمات" to
                "Settings",

            "ظاهر" to
                "Appearance",

            "حالت تاریک" to
                "Dark mode",

            "نمایش ویدئوهای مخفی" to
                "Show hidden videos",

            "پخش" to
                "Playback",

            "ادامه پخش از آخرین موقعیت" to
                "Resume from last position",

            "پخش خودکار ویدئوی بعدی" to
                "Autoplay next video",

            "پخش در پس‌زمینه" to
                "Background playback",

            "روشن ماندن صفحه هنگام پخش" to
                "Keep screen on during playback",

            "کنترل‌های حرکتی" to
                "Gesture controls",

            "سرعت پیش‌فرض" to
                "Default speed",

            "سرعت" to
                "Speed",

            "نسبت تصویر" to
                "Aspect ratio",

            "تطبیق" to
                "Fit",

            "کامل" to
                "Fill",

            "زیرنویس" to
                "Subtitle",

            "اندازه زیرنویس" to
                "Subtitle size",

            "نادیده گرفتن اندازه داخلی زیرنویس" to
                "Ignore embedded subtitle size",

            "پس‌زمینه زیرنویس" to
                "Subtitle background",

            "زیرنویس ضخیم" to
                "Bold subtitles",

            "تأخیر زیرنویس برای ویدئوی فعلی" to
                "Subtitle delay for current video",

            "برای تغییر تأخیر، از کنترل زیرنویس داخل پخش‌کننده استفاده کنید." to
                "To change the delay, use the subtitle controls inside the player.",

            "زبان برنامه" to
                "App language",

            "زبان" to
                "Language",

            "خودکار — زبان گوشی" to
                "Automatic — Device language",

            "فارسی" to
                "Persian",

            "English" to
                "English",

            "لغو" to
                "Cancel",

            "باشه" to
                "OK",

            "امکانات" to
                "Features",

            "🌐 پخش ویدئوی آنلاین" to
                "🌐 Online video",

            "🔒 پوشه خصوصی" to
                "🔒 Private vault",

            "📂 انتخاب پوشه رسانه" to
                "📂 Select media folder",

            "📋 مدیریت پلی‌لیست‌ها" to
                "📋 Manage playlists",

            "⚙️ تنظیمات دسترسی سیستم" to
                "⚙️ System accessibility settings",

            "🔄 بازگردانی تنظیمات" to
                "🔄 Reset settings",

            "پلی‌لیست‌ها" to
                "Playlists",

            "هنوز پلی‌لیستی ساخته نشده است." to
                "No playlists have been created yet.",

            "بازگردانی تنظیمات" to
                "Reset settings",

            "بازگشت" to
                "Back",

            "تمام صفحه" to
                "Fullscreen",

            "ویدئوی قبلی" to
                "Previous video",

            "ویدئوی بعدی" to
                "Next video",

            "توقف" to
                "Pause",

            "توقف یا پخش" to
                "Play / Pause",

            "صدا" to
                "Audio",

            "اشتراک" to
                "Share",

            "اشتراک‌گذاری" to
                "Share",

            "حذف" to
                "Delete",

            "بیشتر" to
                "More",

            "قفل" to
                "Lock",

            "قفل صفحه" to
                "Lock screen",

            "علاقه‌مندی‌ها" to
                "Favorites",

            "افزودن به علاقه‌مندی‌ها" to
                "Add to favorites",

            "حذف از علاقه‌مندی‌ها" to
                "Remove from favorites",

            "افزودن به پلی‌لیست" to
                "Add to playlist",

            "پلی‌لیست" to
                "Playlist",

            "پلی‌لیست جدید" to
                "New playlist",

            "مدیریت پلی‌لیست‌ها" to
                "Manage playlists",

            "تغییر نام" to
                "Rename",

            "تغییر نام ویدئو" to
                "Rename video",

            "ذخیره" to
                "Save",

            "بستن" to
                "Close",

            "ساختن" to
                "Create",

            "مرتب‌سازی" to
                "Sort",

            "جدیدترین" to
                "Newest",

            "نام" to
                "Name",

            "حجم" to
                "Size",

            "همه پوشه‌ها" to
                "All folders",

            "پوشه" to
                "Folder",

            "ویدئو ندارد" to
                "No videos",

            "هیچ ویدئویی پیدا نشد." to
                "No videos found.",

            "نتیجه‌ای برای جستجو پیدا نشد." to
                "No search results.",

            "این پوشه ویدئویی ندارد." to
                "This folder has no videos.",

            "حذف ویدئو" to
                "Delete video",

            "آیا از حذف این ویدئو مطمئن هستید؟" to
                "Are you sure you want to delete this video?",

            "مخفی کردن" to
                "Hide",

            "نمایش فهرستی" to
                "List view",

            "نمایش شبکه‌ای" to
                "Grid view",

            "تنظیمات پخش" to
                "Playback settings",

            "همگام‌سازی صدا" to
                "Audio sync",

            "همگام‌سازی زیرنویس" to
                "Subtitle sync",

            "کیفیت ویدئو" to
                "Video quality",

            "صف پخش" to
                "Playback queue",

            "افزودن به صف" to
                "Add to queue",

            "نمایش صف" to
                "Show queue",

            "پاک کردن صف" to
                "Clear queue",

            "خواب" to
                "Sleep timer",

            "زمان‌سنج خواب" to
                "Sleep timer",

            "پخش ویدئو" to
                "Play video",

            "پوشه" to
                "Folder",

            "سایر" to
                "Other",

            "مجوز دسترسی به ویدئوها داده نشد." to
                "Video access permission was not granted.",

            "تغییر نام انجام نشد." to
                "Rename failed.",

            "حذف ویدئو انجام نشد." to
                "Video deletion failed.",

            "مدیریت پلی‌لیست در دسترس نیست." to
                "Playlist management is unavailable.",

            "افزودن به پلی‌لیست" to
                "Add to playlist",

            "پخش" to
                "Play",

            "خودکار" to
                "Automatic",

            "خودکار • فارسی" to
                "Automatic • Persian",

            "Automatic • English" to
                "Automatic • English"
        )
}
