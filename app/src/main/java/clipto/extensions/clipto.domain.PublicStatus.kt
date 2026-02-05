package clipto.extensions

import clipto.domain.PublicStatus
import com.wb.clipboard.R

fun PublicStatus.getTitleRes(): Int =
        when (this) {
            PublicStatus.PUBLISHED -> R.string.public_status_published
            PublicStatus.PRIVATE -> R.string.public_status_private
            PublicStatus.BLOCKED -> R.string.public_status_blocked
            PublicStatus.IN_REVIEW -> R.string.public_status_review
            PublicStatus.REJECTED -> R.string.public_status_rejected
            PublicStatus.NOT_FOUND -> R.string.public_status_not_found
            PublicStatus.DELETED -> R.string.public_status_deleted
            PublicStatus.RESTRICTED -> R.string.public_status_restricted
        }