package uk.dubai.mall.maps.construction.data.model

import com.google.gson.annotations.SerializedName

data class AppConfig(
    @SerializedName("admob_config") val admobConfig: AdmobConfig,
    @SerializedName("malls") val malls: List<Mall>
)

data class AdmobConfig(
    @SerializedName("banner_id") val bannerId: String,
    @SerializedName("interstitial_id") val interstitialId: String,
    @SerializedName("native_id") val nativeId: String? = null
)

data class Mall(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("description") val description: String,
    @SerializedName("description_in") val descriptionIn: String? = null,
    @SerializedName("description_ar") val descriptionAr: String? = null,
    @SerializedName("floor_plan_url") val floorPlanUrl: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("gmaps_link") val gmapsLink: String,
    @SerializedName("website") val website: String? = null,
    @SerializedName("opening_hours") val openingHours: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("review_count") val reviewCount: Int? = null,
    @SerializedName("favorite_count") val favoriteCount: Int? = null,
    @SerializedName("stores") val stores: Int? = null,
    @SerializedName("parking") val parking: Int? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = null,
    @SerializedName("gallery_images") val galleryImages: List<String>? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("facilities") val facilities: List<String>? = null,
    @SerializedName("metro_station") val metroStation: String? = null,
    @SerializedName("anchor_stores") val anchorStores: List<String>? = null,
    @SerializedName("restaurants") val restaurants: List<String>? = null,
    @SerializedName("highlights") val highlights: List<String>? = null,
    @SerializedName("price_level") val priceLevel: String? = null,
    @SerializedName("estimated_visit_time") val estimatedVisitTime: String? = null,
    @SerializedName("family_friendly") val familyFriendly: Boolean? = null,
    @SerializedName("pet_friendly") val petFriendly: Boolean? = null,
    @SerializedName("wheelchair_accessible") val wheelchairAccessible: Boolean? = null
) : java.io.Serializable {
    fun getLocalizedDescription(): String {
        val locale = java.util.Locale.getDefault().language
        return when (locale) {
            "in", "id" -> descriptionIn ?: description
            "ar" -> descriptionAr ?: description
            else -> description
        }
    }
}
