package com.tripbudget.app.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteTrip(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val destination: String? = null,
    @SerialName("place_lat") val placeLat: Double? = null,
    @SerialName("place_lng") val placeLng: Double? = null,
    @SerialName("planned_budget_paise") val plannedBudgetPaise: Long,
    @SerialName("start_date_epoch_day") val startDateEpochDay: Long? = null,
    @SerialName("end_date_epoch_day") val endDateEpochDay: Long? = null,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("route_tracking_active") val routeTrackingActive: Boolean = false,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RemoteRouteStop(
    val id: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("sort_order") val sortOrder: Int,
    val title: String,
    val subtitle: String? = null,
    val lat: Double,
    val lng: Double,
    @SerialName("osm_place_id") val osmPlaceId: String? = null,
    val status: String,
    val note: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class JoinTripParams(
    @SerialName("p_code") val code: String,
)

@Serializable
data class RemoteMember(
    val id: String,
    @SerialName("trip_id") val tripId: String,
    val name: String,
    @SerialName("expected_contribution_paise") val expectedContributionPaise: Long,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RemoteContribution(
    val id: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("amount_paise") val amountPaise: Long,
    @SerialName("paid_at_millis") val paidAtMillis: Long,
    val note: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RemoteExpense(
    val id: String,
    @SerialName("trip_id") val tripId: String,
    val title: String,
    @SerialName("amount_paise") val amountPaise: Long,
    val category: String,
    @SerialName("date_epoch_day") val dateEpochDay: Long,
    @SerialName("paid_by_member_id") val paidByMemberId: String? = null,
    @SerialName("payment_source") val paymentSource: String,
    val notes: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)
