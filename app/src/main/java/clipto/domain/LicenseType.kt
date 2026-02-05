package clipto.domain

enum class LicenseType(val id: Int, val code: String) {

    NONE(0, "none"),

    CONTRIBUTOR(1, "contributor") {
        override fun isSubscriptionPlan(): Boolean = true
    },

    PERSONAL(2, "personal") {
        override fun isSubscriptionPlan(): Boolean = true
    },

    SUBSCRIPTION(3, "subscription") {
        override fun isSubscriptionPlan(): Boolean = true
    }
    ;

    open fun isSubscriptionPlan() = false

    companion object {

        private val licenses = arrayOf(NONE, CONTRIBUTOR, PERSONAL, SUBSCRIPTION)

        fun byId(id: Int?) = licenses.getOrElse(id ?: 0) { NONE }

    }

}