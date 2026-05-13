package com.clinicare.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.clinicare.model.Clinic;
import com.clinicare.model.ClinicService;
import com.clinicare.model.TimeSlot;
import com.clinicare.model.User;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.ClinicServiceRepository;
import com.clinicare.repository.TimeSlotRepository;
import com.clinicare.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ClinicRepository        clinicRepo;
    private final ClinicServiceRepository serviceRepo;
    private final TimeSlotRepository      slotRepo;
    private final UserRepository          userRepo;
    private final PasswordEncoder         passwordEncoder;

    @Value("${app.seed-data:true}")
    private boolean seedData;

    @Override
    public void run(String... args) {
        if (!seedData || clinicRepo.count() > 0) return;

        // Admin
        userRepo.save(User.builder().name("System Admin").email("admin@clinicare.com")
                .password(passwordEncoder.encode("admin123")).role(User.Role.ADMIN).phone("000-000-0000").build());

        //  Patient demo
        userRepo.save(User.builder().name("Demo Patient").email("patient@clinicare.com")
                .password(passwordEncoder.encode("patient123")).role(User.Role.USER).phone("080-1234-5678").build());

        // 50 Clinics across Japan
        List<Clinic> all = new ArrayList<>();

        // TOKYO (15 clinics) 
        all.add(save("Shinjuku Medical Center",       "1-1 Shinjuku, Shinjuku-ku, Tokyo",          "03-1111-2222", "General clinic offering blood tests, X-rays, and preventive care.",           35.6895, 139.6917, 4.8, 120));
        all.add(save("Kabukicho Clinic",              "2-15 Kabukicho, Shinjuku-ku, Tokyo",         "03-2222-3333", "Walk-in welcome. Fast service, no appointment needed for most tests.",         35.6939, 139.7034, 4.5,  88));
        all.add(save("Takashimaya Health Clinic",     "5-24-2 Sendagaya, Shibuya-ku, Tokyo",        "03-3333-4444", "Premium full-checkup facility with specialist doctors.",                        35.6814, 139.7006, 4.9, 210));
        all.add(save("Yoyogi Family Clinic",          "3-2 Yoyogi, Shibuya-ku, Tokyo",              "03-4444-5555", "Family-friendly clinic, pediatrics and adult care.",                            35.6832, 139.6951, 4.6,  55));
        all.add(save("Shibuya Central Clinic",        "2-1 Dogenzaka, Shibuya-ku, Tokyo",           "03-5555-6666", "Modern clinic in the heart of Shibuya with digital records.",                  35.6580, 139.7016, 4.7,  98));
        all.add(save("Akihabara Health Plaza",        "1-3 Sotokanda, Chiyoda-ku, Tokyo",           "03-6666-7777", "Convenient clinic near Akihabara station. Evening hours available.",            35.6984, 139.7731, 4.4,  67));
        all.add(save("Ikebukuro Wellness Center",     "1-10 Nishiikebukuro, Toshima-ku, Tokyo",     "03-7777-8888", "Comprehensive wellness center with lab, imaging, and specialist care.",         35.7295, 139.7109, 4.6,  88));
        all.add(save("Ginza Premium Clinic",          "5-8 Ginza, Chuo-ku, Tokyo",                  "03-8888-9999", "High-end clinic in Ginza with executive health checkup packages.",              35.6717, 139.7652, 4.9, 302));
        all.add(save("Ueno Medical Clinic",           "4-1 Ueno, Taito-ku, Tokyo",                  "03-9999-0001", "Affordable and efficient clinic serving Ueno and Akihabara areas.",             35.7141, 139.7774, 4.3,  44));
        all.add(save("Asakusa Community Clinic",      "2-3 Asakusa, Taito-ku, Tokyo",               "03-0001-1111", "Community clinic with focus on elderly care and preventive medicine.",           35.7148, 139.7967, 4.5,  71));
        all.add(save("Roppongi International Clinic", "6-1 Roppongi, Minato-ku, Tokyo",             "03-0002-2222", "Multilingual clinic serving Tokyo's international community.",                   35.6628, 139.7314, 4.8, 156));
        all.add(save("Harajuku Skin & Health",        "1-8 Jingumae, Shibuya-ku, Tokyo",            "03-0003-3333", "Dermatology and general health clinic near Harajuku.",                          35.6702, 139.7026, 4.5,  82));
        all.add(save("Shimbashi Clinic",              "2-5 Shimbashi, Minato-ku, Tokyo",            "03-0004-4444", "Business district clinic, open early for working professionals.",                35.6659, 139.7585, 4.4,  59));
        all.add(save("Koenji Family Medical",         "3-2 Koenji-kita, Suginami-ku, Tokyo",        "03-0005-5555", "Warm neighborhood clinic for the whole family.",                                 35.7053, 139.6492, 4.6,  48));
        all.add(save("Nakameguro Health Studio",      "1-4 Kamimeguro, Meguro-ku, Tokyo",           "03-0006-6666", "Boutique clinic with lifestyle medicine and preventive health.",                  35.6440, 139.6986, 4.7,  93));

        // OSAKA (10 clinics)
        all.add(save("Umeda Central Hospital",        "1-1 Umeda, Kita-ku, Osaka",                  "06-1111-2222", "Large clinic in Umeda with comprehensive diagnostic services.",                  34.7024, 135.4959, 4.7, 189));
        all.add(save("Namba Medical Clinic",          "3-1 Namba, Chuo-ku, Osaka",                  "06-2222-3333", "Convenient clinic in Namba shopping district, open weekends.",                   34.6687, 135.5014, 4.5, 134));
        all.add(save("Shinsaibashi Health Center",    "1-5 Shinsaibashisuji, Chuo-ku, Osaka",       "06-3333-4444", "Modern health center with digital imaging and lab services.",                    34.6741, 135.5014, 4.6,  97));
        all.add(save("Tennoji Medical Plaza",         "1-2 Tennoji, Tennoji-ku, Osaka",             "06-4444-5555", "Full-service medical plaza near Tennoji Zoo.",                                   34.6471, 135.5131, 4.4,  76));
        all.add(save("Osaka Honmachi Clinic",         "2-3 Honmachi, Chuo-ku, Osaka",               "06-5555-6666", "Business district clinic with quick appointment booking.",                       34.6842, 135.5006, 4.5,  63));
        all.add(save("Kyobashi Family Clinic",        "1-8 Kyobashi, Miyakojima-ku, Osaka",         "06-6666-7777", "Family clinic serving Kyobashi and surrounding neighborhoods.",                  34.6947, 135.5317, 4.3,  51));
        all.add(save("Nanba Wellness Center",         "4-2 Nambanaka, Naniwa-ku, Osaka",            "06-7777-8888", "Holistic wellness center with preventive and lifestyle medicine.",                34.6626, 135.5013, 4.8, 112));
        all.add(save("Yodoyabashi Medical Center",    "1-4 Hiranomachi, Chuo-ku, Osaka",            "06-8888-9999", "Premium medical center in Osaka's financial district.",                          34.6900, 135.4998, 4.9, 178));
        all.add(save("Tanimachi Health Clinic",       "5-1 Tanimachi, Chuo-ku, Osaka",              "06-9999-0001", "Affordable clinic with same-day appointments available.",                        34.6799, 135.5139, 4.2,  38));
        all.add(save("Shin-Osaka Traveler Clinic",    "5-1 Nishinakajima, Yodogawa-ku, Osaka",      "06-0001-1111", "Quick clinic near Shin-Osaka station for business travelers.",                   34.7331, 135.5001, 4.6,  84));

        // KYOTO (5 clinics)
        all.add(save("Kyoto Station Clinic",          "1-1 Higashishiokojicho, Shimogyo-ku, Kyoto", "075-111-2222", "Modern clinic inside Kyoto Station building.",                                   34.9858, 135.7588, 4.6,  72));
        all.add(save("Kawaramachi Medical Center",    "3-2 Kawaramachi, Nakagyo-ku, Kyoto",         "075-222-3333", "Central Kyoto clinic with English-speaking staff.",                              35.0061, 135.7693, 4.5,  58));
        all.add(save("Fushimi Family Clinic",         "1-3 Fushimimomoyama, Fushimi-ku, Kyoto",     "075-333-4444", "Community clinic near Fushimi Inari, family-focused.",                          34.9401, 135.7733, 4.4,  41));
        all.add(save("Arashiyama Health Center",      "2-5 Sagatenryuji, Ukyo-ku, Kyoto",           "075-444-5555", "Peaceful clinic in Arashiyama with holistic approach.",                          35.0095, 135.6757, 4.7,  63));
        all.add(save("Kyoto Imperial Health Clinic",  "1-2 Kamigyocho, Kamigyo-ku, Kyoto",         "075-555-6666", "Premium clinic near Kyoto Imperial Palace.",                                     35.0254, 135.7619, 4.8,  94));

        // FUKUOKA (5 clinics) 
        all.add(save("Hakata Medical Center",         "1-1 Hakata-eki Higashi, Hakata-ku, Fukuoka", "092-111-2222", "Comprehensive medical center near Hakata station.",                              33.5904, 130.4207, 4.6,  88));
        all.add(save("Tenjin Health Clinic",          "2-3 Tenjin, Chuo-ku, Fukuoka",               "092-222-3333", "Modern clinic in Tenjin shopping area, walk-ins welcome.",                       33.5898, 130.3984, 4.5,  67));
        all.add(save("Nakasu Riverside Clinic",       "1-5 Nakasu, Hakata-ku, Fukuoka",             "092-333-4444", "Riverside clinic with premium diagnostic services.",                             33.5944, 130.4061, 4.7, 103));
        all.add(save("Ohori Park Medical Clinic",     "2-1 Ohorikoen, Chuo-ku, Fukuoka",            "092-444-5555", "Relaxed clinic near Ohori Park with focus on wellness.",                         33.5857, 130.3813, 4.4,  49));
        all.add(save("Fukuoka Airport Clinic",        "1-1 Shimousui, Hakata-ku, Fukuoka",          "092-555-6666", "Quick-service clinic near Fukuoka Airport for travelers.",                       33.5839, 130.4511, 4.3,  37));

        // SAPPORO (4 clinics)
        all.add(save("Sapporo Odori Clinic",          "1-1 Odori Nishi, Chuo-ku, Sapporo",          "011-111-2222", "Central Sapporo clinic with full diagnostic services.",                          43.0621, 141.3544, 4.6,  71));
        all.add(save("Susukino Medical Center",       "5-1 Minami, Chuo-ku, Sapporo",               "011-222-3333", "24-hour emergency clinic in Susukino entertainment district.",                   43.0556, 141.3547, 4.4,  55));
        all.add(save("Sapporo Station Health Plaza",  "2-4 Kita, Chuo-ku, Sapporo",                 "011-333-4444", "Large health plaza connected to Sapporo station.",                               43.0686, 141.3508, 4.7, 112));
        all.add(save("Hokkaido Family Clinic",        "3-2 Nishi, Kita-ku, Sapporo",                "011-444-5555", "Trusted family clinic serving North Sapporo since 2005.",                        43.0774, 141.3388, 4.5,  63));

        // NAGOYA (4 clinics) 
        all.add(save("Nagoya Station Clinic",         "1-1 Meieki, Nakamura-ku, Nagoya",            "052-111-2222", "Convenient clinic attached to Nagoya station complex.",                          35.1706, 136.8817, 4.6,  89));
        all.add(save("Sakae Medical Center",          "3-5 Sakae, Naka-ku, Nagoya",                 "052-222-3333", "Full-service medical center in Sakae shopping district.",                        35.1687, 136.9074, 4.5,  74));
        all.add(save("Osu Health Clinic",             "2-18 Osu, Naka-ku, Nagoya",                  "052-333-4444", "Affordable clinic in the vibrant Osu shopping area.",                            35.1606, 136.9035, 4.3,  48));
        all.add(save("Fushimi Wellness Clinic",       "1-2 Fushimi, Naka-ku, Nagoya",               "052-444-5555", "Business district wellness clinic with executive packages.",                     35.1659, 136.8985, 4.8, 127));

        // HIROSHIMA (3 clinics) 
        all.add(save("Hiroshima Peace Clinic",        "1-1 Otemachi, Naka-ku, Hiroshima",           "082-111-2222", "Central Hiroshima clinic with comprehensive health services.",                   34.3955, 132.4596, 4.6,  66));
        all.add(save("Hondori Medical Center",        "5-2 Hondori, Naka-ku, Hiroshima",            "082-222-3333", "Walk-in clinic on Hiroshima's famous Hondori shopping street.",                  34.3941, 132.4560, 4.4,  51));
        all.add(save("Hiroshima Station Clinic",      "2-3 Matsubara-cho, Minami-ku, Hiroshima",    "082-333-4444", "Quick clinic near Hiroshima station for commuters.",                             34.3966, 132.4756, 4.5,  43));

        // SENDAI (2 clinics)
        all.add(save("Sendai Ichibancho Clinic",      "3-1 Ichibancho, Aoba-ku, Sendai",            "022-111-2222", "Premium clinic in Sendai's main shopping district.",                             38.2682, 140.8694, 4.7,  79));
        all.add(save("Kotodai Health Center",         "1-5 Kotodaicho, Aoba-ku, Sendai",            "022-222-3333", "Comprehensive health center serving central Sendai.",                            38.2600, 140.8694, 4.5,  54));

        // KOBE (3 clinics) 
        all.add(save("Kobe Sannomiya Clinic",         "1-2 Sannomiyacho, Chuo-ku, Kobe",            "078-111-2222", "Modern clinic in Kobe's central Sannomiya area.",                               34.6939, 135.1956, 4.6,  84));
        all.add(save("Motomachi Medical Center",      "3-1 Motomachidori, Chuo-ku, Kobe",           "078-222-3333", "Historic district clinic with advanced medical equipment.",                      34.6907, 135.1844, 4.5,  62));
        all.add(save("Kobe Harborland Clinic",        "2-1 Higashikawasakicho, Chuo-ku, Kobe",      "078-333-4444", "Waterfront clinic with premium diagnostic services.",                            34.6808, 135.1861, 4.8, 118));

        // OKINAWA (2 clinics) 
        all.add(save("Kokusai Street Clinic",         "2-4 Makishi, Naha-shi, Okinawa",             "098-111-2222", "Central Naha clinic on famous Kokusai shopping street.",                         26.2124, 127.6809, 4.5,  57));
        all.add(save("Naha Airport Medical Center",   "1-1 Kagamizu, Naha-shi, Okinawa",            "098-222-3333", "Quick medical services near Naha Airport for travelers.",                        26.1958, 127.6460, 4.3,  39));

        // YOKOHAMA (2 clinics) 
        all.add(save("Yokohama Minato Clinic",        "1-1 Kaigandori, Naka-ku, Yokohama",          "045-111-2222", "Premium clinic in Yokohama's scenic Minato Mirai area.",                        35.4478, 139.6425, 4.7,  96));
        all.add(save("Kannai Medical Center",         "3-2 Nihonodori, Naka-ku, Yokohama",          "045-222-3333", "Full-service medical center in Kannai business district.",                       35.4414, 139.6409, 4.5,  73));

        System.out.println("✅ Created " + all.size() + " clinics");

        // Clinic login accounts (first 10 clinics get login)
        String[] clinicEmails = {
            "clinic1@clinicare.com","clinic2@clinicare.com","clinic3@clinicare.com",
            "clinic4@clinicare.com","clinic5@clinicare.com","clinic6@clinicare.com",
            "clinic7@clinicare.com","clinic8@clinicare.com","clinic9@clinicare.com",
            "clinic10@clinicare.com"
        };
        for (int i = 0; i < Math.min(10, all.size()); i++) {
            userRepo.save(User.builder()
                    .name(all.get(i).getName())
                    .email(clinicEmails[i])
                    .password(passwordEncoder.encode("clinic123"))
                    .role(User.Role.CLINIC)
                    .managedClinic(all.get(i)).build());
        }

        // Services for every clinic 
        String[][] serviceSets = {
            {"Blood Test",          "Lab",        "2800",  "20"},
            {"Chest X-Ray",         "Imaging",    "4500",  "30"},
            {"General Checkup",     "Preventive", "3200",  "40"},
            {"Urine Test",          "Lab",        "2000",  "15"},
            {"ECG",                 "Cardiology", "5000",  "30"},
            {"Blood Pressure Check","General",    "1500",  "10"},
            {"Full Checkup Package","Preventive","18000", "120"},
            {"MRI Scan",            "Imaging",   "25000",  "60"},
            {"Dental Checkup",      "Dental",     "3500",  "45"},
            {"Pediatric Checkup",   "Pediatrics", "3800",  "30"},
        };

        for (int i = 0; i < all.size(); i++) {
            Clinic c = all.get(i);
            // Each clinic gets 3-5 services
            int start = i % serviceSets.length;
            for (int j = 0; j < 4; j++) {
                String[] svc = serviceSets[(start + j) % serviceSets.length];
                serviceRepo.save(ClinicService.builder()
                        .clinic(c).serviceName(svc[0]).category(svc[1])
                        .price(new BigDecimal(svc[2]))
                        .durationMinutes(Integer.parseInt(svc[3]))
                        .available(true).build());
            }
        }

        // Time slots for all clinics 
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = today.plusDays(2);
        LocalTime[] starts = {
            LocalTime.of(9,0), LocalTime.of(9,30), LocalTime.of(10,0),
            LocalTime.of(10,30), LocalTime.of(11,0), LocalTime.of(14,0),
            LocalTime.of(14,30), LocalTime.of(15,0), LocalTime.of(15,30)
        };

        for (Clinic clinic : all) {
            for (LocalTime st : starts) {
                for (LocalDate date : List.of(today, tomorrow, dayAfter)) {
                    slotRepo.save(TimeSlot.builder()
                            .clinic(clinic).date(date)
                            .startTime(st).endTime(st.plusMinutes(30))
                            .waitMinutes(0).isBooked(false).build());
                }
            }
        }

        System.out.println("✅ Seed data loaded successfully! 50 clinics across Japan.");
        System.out.println("   Admin:    admin@clinicare.com / admin123");
        System.out.println("   Patient:  patient@clinicare.com / patient123");
        System.out.println("   Clinics:  clinic1@clinicare.com ~ clinic10@clinicare.com / clinic123");
    }

    private Clinic save(String name, String address, String phone, String description,
                        double lat, double lng, double rating, int reviewCount) {
        return clinicRepo.save(Clinic.builder()
                .name(name).address(address).phone(phone).description(description)
                .latitude(lat).longitude(lng)
                .averageRating(rating).reviewCount(reviewCount)
                .active(true).build());
    }
}