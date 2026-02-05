package clipto.domain

import android.graphics.drawable.LayerDrawable
import androidx.annotation.IntRange
import clipto.AppContext
import clipto.common.misc.Units
import clipto.common.presentation.view.DrawableViewTarget
import clipto.extensions.getActionIconColorInactive
import clipto.extensions.getColorNegative
import clipto.extensions.getColorPositive
import clipto.utils.GlideUtils
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

enum class FileState {

    Normal {

        override fun doApply(chip: Chip, fileRef: FileRef) {
            chip.setChipIconResource(fileRef.type.roundIconRes)
            chip.isChipIconVisible = true
            loadPreview(chip, fileRef)
        }

        override fun setProgress(chip: Chip, progress: Int): FileState = this
    },

    Upload {
        override fun isInProgress(): Boolean = true
        override fun doApply(chip: Chip, fileRef: FileRef) {
            chip.setChipIconResource(R.drawable.attachment_upload)
            chip.isChipIconVisible = true
            val drawable = chip.chipIcon
            if (drawable is LayerDrawable) {
                drawable.getDrawable(1).apply {
                    setTint(chip.context.getActionIconColorInactive())
                    level = 10_000
                }
            }
            setProgress(chip, 0)
        }
    },

    Download {
        override fun isInProgress(): Boolean = true
        override fun doApply(chip: Chip, fileRef: FileRef) {
            chip.setChipIconResource(R.drawable.attachment_download)
            chip.isChipIconVisible = true
            val drawable = chip.chipIcon
            if (drawable is LayerDrawable) {
                drawable.getDrawable(1).apply {
                    setTint(chip.context.getActionIconColorInactive())
                    level = 10_000
                }
            }
            setProgress(chip, 0)
        }
    },

    Downloaded {
        override fun doApply(chip: Chip, fileRef: FileRef) {
            chip.setChipIconResource(fileRef.type.roundIconRes)
            chip.isChipIconVisible = true
            val drawable = chip.chipIcon
            if (drawable is LayerDrawable) {
                drawable.getDrawable(1).apply {
                    setTint(chip.context.getColorPositive())
                }
            }
            loadPreview(chip, fileRef)
        }

        override fun setProgress(chip: Chip, progress: Int): FileState = this
    },

    Uploaded {
        override fun doApply(chip: Chip, fileRef: FileRef) {
            chip.setChipIconResource(fileRef.type.roundIconRes)
            chip.isChipIconVisible = true
            val drawable = chip.chipIcon
            if (drawable is LayerDrawable) {
                drawable.getDrawable(1).apply {
                    setTint(chip.context.getColorPositive())
                }
            }
            loadPreview(chip, fileRef)
        }

        override fun setProgress(chip: Chip, progress: Int): FileState = this
    },

    Error {
        override fun doApply(chip: Chip, fileRef: FileRef) {
            chip.setChipIconResource(R.drawable.attachment_error)
            chip.isChipIconVisible = true
            val drawable = chip.chipIcon
            if (drawable is LayerDrawable) {
                drawable.getDrawable(1).apply {
                    setTint(chip.context.getColorNegative())
                }
            }
        }

        override fun setProgress(chip: Chip, progress: Int): FileState = this
    },

    ;

    fun apply(chip: Chip, fileRef: FileRef): FileState {
        chip.tag = fileRef
        doApply(chip, fileRef)
        setProgress(chip, fileRef.progress)
        return this
    }

    open fun setProgress(chip: Chip, @IntRange(from = 0L, to = 100L) progress: Int): FileState {
        val drawable = chip.chipIcon
        if (drawable is LayerDrawable) {
            drawable.getDrawable(2).apply {
                setTint(chip.context.getColorPositive())
                level = progress * 100
            }
        }
        return this
    }

    protected fun loadPreview(chip: Chip, fileRef: FileRef) {
        if (fileRef.isLarge()) return
        AppContext.get().getAuthUserCollection()?.let { collection ->
            val url = fileRef.getPreviewUrl(chip.context, collection) ?: return
            val s = Units.DP.toPx(24f).toInt()
            val options = RequestOptions().override(s, s).circleCrop()
            val thumbUrl = fileRef.getThumbUrl(chip.context, collection)
            val previewUrl = thumbUrl ?: url
            GlideUtils.loadDrawable(chip.context, previewUrl)
                .apply(options)
                .let {
                    if (url !== previewUrl) {
                        it.error(GlideUtils.loadDrawable(chip.context, url).apply(options))
                    } else {
                        it
                    }
                }
                .into(DrawableViewTarget(chip) {
                    val actualFileRef = chip.tag as? FileRef
                    if (actualFileRef == fileRef) {
                        chip.chipIcon = it
                    }
                })
        }
    }

    protected abstract fun doApply(chip: Chip, fileRef: FileRef)
    open fun isInProgress(): Boolean = false

}