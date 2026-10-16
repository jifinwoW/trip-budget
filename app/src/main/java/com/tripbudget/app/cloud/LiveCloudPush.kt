package com.tripbudget.app.cloud

/**
 * Fire-and-forget cloud upsert/delete after a local Room write when the user is signed in.
 * Offline / signed-out writes stay local until the next full sync or a later online write.
 */
interface LiveCloudPush {
    fun onTripUpserted(localId: Long)
    fun onTripDeleted(remoteId: String?)

    fun onMemberUpserted(localId: Long)
    fun onMemberDeleted(remoteId: String?)

    fun onContributionUpserted(localId: Long)
    fun onContributionDeleted(remoteId: String?)

    fun onExpenseUpserted(localId: Long)
    fun onExpenseDeleted(remoteId: String?)

    fun onRouteStopUpserted(localId: Long)
    fun onRouteStopDeleted(remoteId: String?)
}
