package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    val ICON_LIST = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "fastfood" to Icons.Default.Fastfood,
        "local_cafe" to Icons.Default.LocalCafe,
        "local_bar" to Icons.Default.LocalBar,
        "local_grocery_store" to Icons.Default.LocalGroceryStore,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "checkroom" to Icons.Default.Checkroom,
        "directions_car" to Icons.Default.DirectionsCar,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "directions_bus" to Icons.Default.DirectionsBus,
        "directions_subway" to Icons.Default.DirectionsSubway,
        "flight" to Icons.Default.Flight,
        "two_wheeler" to Icons.Default.TwoWheeler,
        "movie" to Icons.Default.Movie,
        "sports_esports" to Icons.Default.SportsEsports,
        "music_note" to Icons.Default.MusicNote,
        "fitness_center" to Icons.Default.FitnessCenter,
        "sports_soccer" to Icons.Default.SportsSoccer,
        "favorite" to Icons.Default.Favorite,
        "medical_services" to Icons.Default.MedicalServices,
        "local_pharmacy" to Icons.Default.LocalPharmacy,
        "spa" to Icons.Default.Spa,
        "home" to Icons.Default.Home,
        "apartment" to Icons.Default.Apartment,
        "flash_on" to Icons.Default.FlashOn,
        "water_drop" to Icons.Default.WaterDrop,
        "wifi" to Icons.Default.Wifi,
        "phone_iphone" to Icons.Default.PhoneIphone,
        "tv" to Icons.Default.Tv,
        "school" to Icons.Default.School,
        "menu_book" to Icons.Default.MenuBook,
        "pets" to Icons.Default.Pets,
        "child_care" to Icons.Default.ChildCare,
        "payments" to Icons.Default.Payments,
        "account_balance" to Icons.Default.AccountBalance,
        "credit_card" to Icons.Default.CreditCard,
        "savings" to Icons.Default.Savings,
        "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
        "monetization_on" to Icons.Default.MonetizationOn,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "subscriptions" to Icons.Default.Subscriptions,
        "security" to Icons.Default.Security,
        "work" to Icons.Default.Work,
        "build" to Icons.Default.Build,
        "brush" to Icons.Default.Brush,
        "receipt" to Icons.AutoMirrored.Filled.ReceiptLong,
        "sell" to Icons.Default.Sell,
        "star" to Icons.Default.Star,
        "hotel" to Icons.Default.Hotel,
        "celebration" to Icons.Default.Celebration,
        "redeem" to Icons.Default.Redeem
    )

    fun getIcon(name: String): ImageVector {
        return ICON_LIST.find { it.first.equals(name, ignoreCase = true) }?.second ?: Icons.Default.Category
    }
}
