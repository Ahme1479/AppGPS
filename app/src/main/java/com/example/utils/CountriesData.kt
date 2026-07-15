package com.example.utils

data class CountryInfo(
    val nameEn: String,
    val nameAr: String,
    val code: String,
    val latitude: Double,
    val longitude: Double
)

fun parseDoubleSafe(value: String): Double? {
    var cleaned = value.trim()
    val arabicDigits = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    for (i in 0..9) {
        cleaned = cleaned.replace(arabicDigits[i], i.toString())
    }
    val persianDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    for (i in 0..9) {
        cleaned = cleaned.replace(persianDigits[i], i.toString())
    }
    cleaned = cleaned.replace("٫", ".").replace(",", ".")
    return cleaned.toDoubleOrNull()
}

object CountriesData {
    val list = listOf(
        CountryInfo("United States", "الولايات المتحدة", "US", 37.0902, -95.7129),
        CountryInfo("United Kingdom", "المملكة المتحدة", "GB", 55.3781, -3.4360),
        CountryInfo("Germany", "ألمانيا", "DE", 51.1657, 10.4515),
        CountryInfo("France", "فرنسا", "FR", 46.2276, 2.2137),
        CountryInfo("Japan", "اليابان", "JP", 36.2048, 138.2529),
        CountryInfo("Egypt", "مصر", "EG", 26.8206, 30.8025),
        CountryInfo("Saudi Arabia", "المملكة العربية السعودية", "SA", 23.8859, 45.0792),
        CountryInfo("United Arab Emirates", "الإمارات العربية المتحدة", "AE", 23.4241, 53.8478),
        CountryInfo("Turkey", "تركيا", "TR", 38.9637, 35.2433),
        CountryInfo("Netherlands", "هولندا", "NL", 52.1326, 5.2913),
        CountryInfo("Canada", "كندا", "CA", 56.1304, -106.3468),
        CountryInfo("Australia", "أستراليا", "AU", -25.2744, 133.7751),
        CountryInfo("Singapore", "سنغافورة", "SG", 1.3521, 103.8198),
        CountryInfo("South Korea", "كوريا الجنوبية", "KR", 35.9078, 127.7669),
        CountryInfo("Switzerland", "سويسرا", "CH", 46.8182, 8.2275),
        CountryInfo("Sweden", "السويد", "SE", 60.1282, 18.6435),
        CountryInfo("India", "الهند", "IN", 20.5937, 78.9629),
        CountryInfo("Brazil", "البرازيل", "BR", -14.2350, -51.9253),
        CountryInfo("Spain", "إسبانيا", "ES", 40.4637, -3.7492),
        CountryInfo("Italy", "إيطاليا", "IT", 41.8719, 12.5674),
        CountryInfo("Russia", "روسيا", "RU", 61.5240, 105.3188),
        CountryInfo("Jordan", "الأردن", "JO", 30.5852, 36.2384),
        CountryInfo("Morocco", "المغرب", "MA", 31.7917, -7.0926),
        CountryInfo("Algeria", "الجزائر", "DZ", 28.0339, 1.6596),
        CountryInfo("Qatar", "قطر", "QA", 25.3548, 51.1839),
        CountryInfo("Kuwait", "الكويت", "KW", 29.3117, 47.4818),
        CountryInfo("Bahrain", "البحرين", "BH", 25.9304, 50.6378),
        CountryInfo("Oman", "عمان", "OM", 21.5126, 55.9233),
        CountryInfo("Iraq", "العراق", "IQ", 33.2232, 43.6793),
        CountryInfo("Lebanon", "لبنان", "LB", 33.8547, 35.8623),
        CountryInfo("Tunisia", "تونس", "TN", 33.8869, 9.5375),
        CountryInfo("Palestine", "فلسطين", "PS", 31.9522, 35.2332),
        CountryInfo("Malaysia", "ماليزيا", "MY", 4.2105, 101.9758),
        CountryInfo("Indonesia", "إندونيسيا", "ID", -0.7893, 113.9213),
        CountryInfo("South Africa", "جنوب أفريقيا", "ZA", -30.5595, 22.9375),
        CountryInfo("Argentina", "الأرجنتين", "AR", -38.4161, -63.6167),
        CountryInfo("Mexico", "المكسيك", "MX", 23.6345, -102.5528),
        CountryInfo("China", "الصين", "CN", 35.8617, 104.1954),
        CountryInfo("Norway", "النرويج", "NO", 60.4720, 8.4689),
        CountryInfo("Austria", "النمسا", "AT", 47.5162, 14.5501)
    )
}
